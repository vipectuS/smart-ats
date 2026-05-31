import asyncio

from app.config import Settings
from app.services.ai_observability import AIObservability
from app.services.litellm_resilience import (
    LiteLLMCircuitOpenError,
    ResilientLiteLLMClient,
)


def test_resilient_litellm_client_retries_and_then_succeeds() -> None:
    attempts: list[int] = []
    observability = AIObservability()

    async def flaky_completion(**kwargs):
        attempts.append(1)
        if len(attempts) < 3:
            raise RuntimeError(f"transient-{len(attempts)}")
        return {"choices": [{"message": {"content": '{"ok":true}'}}]}

    client = ResilientLiteLLMClient(
        Settings(
            AI_COMPLETION_MAX_ATTEMPTS=3,
            AI_COMPLETION_RETRY_BASE_SECONDS=0,
            AI_COMPLETION_RETRY_MAX_DELAY_SECONDS=0,
            AI_COMPLETION_CIRCUIT_BREAKER_THRESHOLD=3,
        ),
        completion_callable=flaky_completion,
        observability=observability,
    )

    response = asyncio.run(client.complete(operation="job_fit_report", model="gpt-4o-mini", messages=[]))
    snapshot = observability.snapshot()

    assert len(attempts) == 3
    assert response["choices"][0]["message"]["content"] == '{"ok":true}'
    assert snapshot["operations"]["job_fit_report"]["requests"] == 1
    assert snapshot["operations"]["job_fit_report"]["successes"] == 1
    assert snapshot["operations"]["job_fit_report"]["retry_attempts"] == 2


def test_resilient_litellm_client_switches_to_fallback_model_after_primary_exhausts() -> None:
    attempts: list[str] = []
    observability = AIObservability()

    async def model_aware_completion(**kwargs):
        model = kwargs["model"]
        attempts.append(model)
        if model == "primary-model":
            raise RuntimeError("primary unavailable")
        return {"choices": [{"message": {"content": '{"ok":true}'}}]}

    client = ResilientLiteLLMClient(
        Settings(
            AI_COMPLETION_MAX_ATTEMPTS=2,
            AI_COMPLETION_RETRY_BASE_SECONDS=0,
            AI_COMPLETION_RETRY_MAX_DELAY_SECONDS=0,
            AI_COMPLETION_CIRCUIT_BREAKER_THRESHOLD=3,
        ),
        completion_callable=model_aware_completion,
        observability=observability,
    )

    response = asyncio.run(
        client.complete(
            operation="resume_parse",
            model="primary-model",
            fallback_models=["backup-model"],
            messages=[],
        )
    )

    assert attempts == ["primary-model", "primary-model", "backup-model"]
    assert response["choices"][0]["message"]["content"] == '{"ok":true}'
    snapshot = observability.snapshot()
    assert snapshot["operations"]["resume_parse"]["requests"] == 1
    assert snapshot["operations"]["resume_parse"]["successes"] == 1
    assert snapshot["operations"]["resume_parse"]["retry_attempts"] == 1
    assert snapshot["operations"]["resume_parse"]["retry_exhausted"] == 0


def test_resilient_litellm_client_opens_circuit_after_repeated_failures() -> None:
    attempts: list[int] = []
    observability = AIObservability()

    async def always_fail(**kwargs):
        attempts.append(1)
        raise RuntimeError("upstream unavailable")

    client = ResilientLiteLLMClient(
        Settings(
            AI_COMPLETION_MAX_ATTEMPTS=1,
            AI_COMPLETION_RETRY_BASE_SECONDS=0,
            AI_COMPLETION_RETRY_MAX_DELAY_SECONDS=0,
            AI_COMPLETION_CIRCUIT_BREAKER_THRESHOLD=2,
            AI_COMPLETION_CIRCUIT_BREAKER_RESET_SECONDS=60,
        ),
        completion_callable=always_fail,
        observability=observability,
    )

    for _ in range(2):
        try:
            asyncio.run(client.complete(operation="resume_parse", model="gpt-4o-mini", messages=[]))
        except RuntimeError:
            pass

    try:
        asyncio.run(client.complete(operation="resume_parse", model="gpt-4o-mini", messages=[]))
        assert False, "Expected circuit to be open"
    except LiteLLMCircuitOpenError:
        pass

    assert len(attempts) == 2
    snapshot = observability.snapshot()
    assert snapshot["operations"]["resume_parse"]["retry_exhausted"] == 2
    assert snapshot["operations"]["resume_parse"]["circuit_opened"] == 1
    assert snapshot["operations"]["resume_parse"]["circuit_open_rejections"] == 1


def test_resilient_litellm_client_skips_open_primary_circuit_and_uses_fallback() -> None:
    attempts: list[str] = []

    async def model_aware_completion(**kwargs):
        model = kwargs["model"]
        attempts.append(model)
        if model == "primary-model":
            raise RuntimeError("primary unavailable")
        return {"choices": [{"message": {"content": '{"ok":true}'}}]}

    client = ResilientLiteLLMClient(
        Settings(
            AI_COMPLETION_MAX_ATTEMPTS=1,
            AI_COMPLETION_RETRY_BASE_SECONDS=0,
            AI_COMPLETION_RETRY_MAX_DELAY_SECONDS=0,
            AI_COMPLETION_CIRCUIT_BREAKER_THRESHOLD=1,
            AI_COMPLETION_CIRCUIT_BREAKER_RESET_SECONDS=60,
        ),
        completion_callable=model_aware_completion,
        observability=AIObservability(),
    )

    try:
        asyncio.run(client.complete(operation="resume_parse", model="primary-model", messages=[]))
    except RuntimeError:
        pass

    response = asyncio.run(
        client.complete(
            operation="resume_parse",
            model="primary-model",
            fallback_models=["backup-model"],
            messages=[],
        )
    )

    assert attempts == ["primary-model", "backup-model"]
    assert response["choices"][0]["message"]["content"] == '{"ok":true}'