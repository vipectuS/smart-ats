from __future__ import annotations

from collections import deque
from dataclasses import asdict, dataclass, field
from threading import Lock
from time import time
from typing import Any


@dataclass
class OperationMetrics:
    requests: int = 0
    successes: int = 0
    retry_attempts: int = 0
    retry_exhausted: int = 0
    circuit_opened: int = 0
    circuit_open_rejections: int = 0
    failures: int = 0
    last_error: str | None = None
    last_latency_ms: float | None = None
    latency_ms_window: deque[float] = field(default_factory=lambda: deque(maxlen=200))

    def snapshot(self) -> dict[str, Any]:
        payload = asdict(self)
        latencies = list(self.latency_ms_window)
        payload["latency_p95_ms"] = _compute_percentile(latencies, 0.95)
        payload["latency_avg_ms"] = round(sum(latencies) / len(latencies), 2) if latencies else None
        payload["latency_samples"] = len(latencies)
        payload.pop("latency_ms_window", None)
        return payload


def _compute_percentile(values: list[float], percentile: float) -> float | None:
    if not values:
        return None
    ordered = sorted(values)
    index = max(0, min(len(ordered) - 1, int(round((len(ordered) - 1) * percentile))))
    return round(ordered[index], 2)


class AIObservability:
    def __init__(self) -> None:
        self._lock = Lock()
        self._operation_metrics: dict[str, OperationMetrics] = {}
        self._embedding_fallbacks: dict[str, int] = {}
        self._external_content_failures: int = 0
        self._external_content_warnings: int = 0
        self._last_updated_epoch_seconds: float | None = None

    def record_request(self, operation: str) -> None:
        with self._lock:
            metrics = self._operation_metrics.setdefault(operation, OperationMetrics())
            metrics.requests += 1
            self._last_updated_epoch_seconds = time()

    def record_success(self, operation: str, duration_ms: float) -> None:
        with self._lock:
            metrics = self._operation_metrics.setdefault(operation, OperationMetrics())
            metrics.successes += 1
            metrics.last_latency_ms = round(duration_ms, 2)
            metrics.latency_ms_window.append(duration_ms)
            self._last_updated_epoch_seconds = time()

    def record_retry_attempt(self, operation: str, error: Exception) -> None:
        with self._lock:
            metrics = self._operation_metrics.setdefault(operation, OperationMetrics())
            metrics.retry_attempts += 1
            metrics.last_error = str(error)
            self._last_updated_epoch_seconds = time()

    def record_retry_exhausted(self, operation: str, error: Exception) -> None:
        with self._lock:
            metrics = self._operation_metrics.setdefault(operation, OperationMetrics())
            metrics.retry_exhausted += 1
            metrics.failures += 1
            metrics.last_error = str(error)
            self._last_updated_epoch_seconds = time()

    def record_circuit_opened(self, operation: str, error: Exception) -> None:
        with self._lock:
            metrics = self._operation_metrics.setdefault(operation, OperationMetrics())
            metrics.circuit_opened += 1
            metrics.last_error = str(error)
            self._last_updated_epoch_seconds = time()

    def record_circuit_rejection(self, operation: str, error: Exception) -> None:
        with self._lock:
            metrics = self._operation_metrics.setdefault(operation, OperationMetrics())
            metrics.circuit_open_rejections += 1
            metrics.last_error = str(error)
            self._last_updated_epoch_seconds = time()

    def record_failure(self, operation: str, error: Exception) -> None:
        with self._lock:
            metrics = self._operation_metrics.setdefault(operation, OperationMetrics())
            metrics.failures += 1
            metrics.last_error = str(error)
            self._last_updated_epoch_seconds = time()

    def record_embedding_fallback(self, reason: str) -> None:
        with self._lock:
            self._embedding_fallbacks[reason] = self._embedding_fallbacks.get(reason, 0) + 1
            self._last_updated_epoch_seconds = time()

    def record_external_content_failure(self) -> None:
        with self._lock:
            self._external_content_failures += 1
            self._last_updated_epoch_seconds = time()

    def record_external_content_warning(self) -> None:
        with self._lock:
            self._external_content_warnings += 1
            self._last_updated_epoch_seconds = time()

    def snapshot(self) -> dict[str, Any]:
        with self._lock:
            return {
                "operations": {
                    operation: metrics.snapshot()
                    for operation, metrics in sorted(self._operation_metrics.items())
                },
                "embeddingFallbacks": dict(sorted(self._embedding_fallbacks.items())),
                "externalContentFailures": self._external_content_failures,
                "externalContentWarnings": self._external_content_warnings,
                "lastUpdatedEpochSeconds": round(self._last_updated_epoch_seconds, 3)
                if self._last_updated_epoch_seconds is not None
                else None,
            }

    def reset(self) -> None:
        with self._lock:
            self._operation_metrics.clear()
            self._embedding_fallbacks.clear()
            self._external_content_failures = 0
            self._external_content_warnings = 0
            self._last_updated_epoch_seconds = None