from __future__ import annotations

import asyncio
import logging
from dataclasses import dataclass
from functools import lru_cache
from time import monotonic
from typing import Any, Awaitable, Callable

from litellm import acompletion

from app.config import Settings, get_settings
from app.services.ai_observability import AIObservability


logger = logging.getLogger(__name__)


class LiteLLMRetryExhaustedError(RuntimeError):
    pass


class LiteLLMCircuitOpenError(RuntimeError):
    pass


@dataclass
class CircuitState:
    consecutive_failures: int = 0
    open_until: float | None = None


class ResilientLiteLLMClient:
    def __init__(
        self,
        settings: Settings,
        completion_callable: Callable[..., Awaitable[Any]] | None = None,
        observability: AIObservability | None = None,
    ) -> None:
        self.settings = settings
        self._completion_callable = completion_callable or acompletion
        self._circuit_states: dict[str, CircuitState] = {}
        self._observability = observability or get_ai_observability()

    async def complete(self, *, operation: str, fallback_models: list[str] | None = None, **kwargs: Any) -> Any:
        primary_model = kwargs.get("model")
        if not isinstance(primary_model, str) or not primary_model.strip():
            raise ValueError("LiteLLM completion requests require a non-empty model")

        model_candidates = self._build_model_candidates(primary_model, fallback_models or [])
        self._observability.record_request(operation)
        started_at = monotonic()
        last_error: Exception | None = None

        for index, model_name in enumerate(model_candidates):
            state = self._circuit_states.setdefault(self._build_model_circuit_key(operation, model_name), CircuitState())
            try:
                self._ensure_circuit_closed(operation, state, target=model_name)
            except LiteLLMCircuitOpenError as exc:
                last_error = exc
                if index < len(model_candidates) - 1:
                    logger.warning(
                        "LiteLLM model circuit open operation=%s model=%s switching_to=%s",
                        operation,
                        model_name,
                        model_candidates[index + 1],
                    )
                    continue
                raise

            model_kwargs = {**kwargs, "model": model_name}
            try:
                response = await self._execute_with_retries(
                    operation=operation,
                    target=model_name,
                    state=state,
                    action=lambda model_kwargs=model_kwargs: self._completion_callable(**model_kwargs),
                )
            except Exception as exc:
                last_error = exc
                if index < len(model_candidates) - 1:
                    logger.warning(
                        "LiteLLM model exhausted operation=%s model=%s switching_to=%s error=%s",
                        operation,
                        model_name,
                        model_candidates[index + 1],
                        exc,
                    )
                    continue

                self._observability.record_retry_exhausted(operation, exc)
                raise LiteLLMRetryExhaustedError(
                    f"LiteLLM call failed for {operation} after exhausting models {model_candidates}: {exc}"
                ) from exc

            self._record_success(state)
            self._observability.record_success(operation, (monotonic() - started_at) * 1000)
            return response

        assert last_error is not None
        raise last_error

    async def execute(
        self,
        *,
        operation: str,
        action: Callable[[], Awaitable[Any]],
    ) -> Any:
        state = self._circuit_states.setdefault(operation, CircuitState())
        self._observability.record_request(operation)
        self._ensure_circuit_closed(operation, state)
        start = monotonic()
        try:
            response = await self._execute_with_retries(
                operation=operation,
                target=operation,
                state=state,
                action=action,
            )
        except Exception as exc:
            self._observability.record_retry_exhausted(operation, exc)
            raise LiteLLMRetryExhaustedError(
                f"LiteLLM call failed for {operation} after {max(1, self.settings.ai_completion_max_attempts)} attempts: {exc}"
            ) from exc

        self._record_success(state)
        self._observability.record_success(operation, (monotonic() - start) * 1000)
        return response

    async def _execute_with_retries(
        self,
        *,
        operation: str,
        target: str,
        state: CircuitState,
        action: Callable[[], Awaitable[Any]],
    ) -> Any:
        last_error: Exception | None = None
        max_attempts = max(1, self.settings.ai_completion_max_attempts)

        for attempt in range(1, max_attempts + 1):
            try:
                return await action()
            except Exception as exc:  # pragma: no cover - external SDK surface is broad
                last_error = exc
                if attempt >= max_attempts:
                    break

                self._observability.record_retry_attempt(operation, exc)
                delay = min(
                    self.settings.ai_completion_retry_base_seconds
                    * (self.settings.ai_completion_retry_backoff_multiplier ** (attempt - 1)),
                    self.settings.ai_completion_retry_max_delay_seconds,
                )
                logger.warning(
                    "LiteLLM call failed operation=%s target=%s attempt=%s/%s retry_in=%.2fs error=%s",
                    operation,
                    target,
                    attempt,
                    max_attempts,
                    delay,
                    exc,
                )
                await asyncio.sleep(delay)

        assert last_error is not None
        self._record_failure(operation, state, last_error, target=target)
        raise last_error

    def _ensure_circuit_closed(self, operation: str, state: CircuitState, target: str | None = None) -> None:
        if state.open_until is None:
            return

        now = monotonic()
        if now >= state.open_until:
            logger.info("LiteLLM circuit reset operation=%s target=%s", operation, target or operation)
            state.open_until = None
            state.consecutive_failures = 0
            return

        remaining = state.open_until - now
        error = LiteLLMCircuitOpenError(
            f"LiteLLM circuit open for {target or operation}; retry after {remaining:.1f}s cooldown"
        )
        self._observability.record_circuit_rejection(operation, error)
        raise error

    def _build_model_candidates(self, primary_model: str, fallback_models: list[str]) -> list[str]:
        candidates: list[str] = []
        for model_name in [primary_model, *fallback_models]:
            normalized = model_name.strip()
            if normalized and normalized not in candidates:
                candidates.append(normalized)
        return candidates

    def _build_model_circuit_key(self, operation: str, model_name: str) -> str:
        return f"{operation}:{model_name}"

    def _record_success(self, state: CircuitState) -> None:
        state.consecutive_failures = 0
        state.open_until = None

    def _record_failure(self, operation: str, state: CircuitState, error: Exception, *, target: str) -> None:
        state.consecutive_failures += 1
        threshold = max(1, self.settings.ai_completion_circuit_breaker_threshold)

        if state.consecutive_failures >= threshold:
            state.open_until = monotonic() + self.settings.ai_completion_circuit_breaker_reset_seconds
            self._observability.record_circuit_opened(operation, error)
            logger.error(
                "LiteLLM circuit opened operation=%s target=%s failures=%s cooldown=%.2fs error=%s",
                operation,
                target,
                state.consecutive_failures,
                self.settings.ai_completion_circuit_breaker_reset_seconds,
                error,
            )
            return

        self._observability.record_failure(operation, error)
        logger.warning(
            "LiteLLM operation failed operation=%s target=%s failures=%s/%s error=%s",
            operation,
            target,
            state.consecutive_failures,
            threshold,
            error,
        )


@lru_cache
def get_ai_observability() -> AIObservability:
    return AIObservability()


@lru_cache
def get_shared_litellm_client() -> ResilientLiteLLMClient:
    return ResilientLiteLLMClient(get_settings(), observability=get_ai_observability())