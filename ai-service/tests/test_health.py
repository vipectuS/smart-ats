from fastapi.testclient import TestClient

from app.main import app
from app.services.litellm_resilience import get_ai_observability


def test_health() -> None:
    with TestClient(app) as client:
        response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {
        "status": "ok",
        "service": "ai-service",
        "queue": "resume_parsing_queue",
    }


def test_ai_observability_health_snapshot() -> None:
    observability = get_ai_observability()
    observability.reset()
    observability.record_request("resume_parse")
    observability.record_retry_attempt("resume_parse", RuntimeError("transient"))
    observability.record_retry_exhausted("resume_parse", RuntimeError("fatal"))
    observability.record_embedding_fallback("retry_exhausted")
    observability.record_external_content_failure()

    try:
        with TestClient(app) as client:
            response = client.get("/health/ai-observability")
    finally:
        observability.reset()

    assert response.status_code == 200
    payload = response.json()
    assert payload["status"] == "ok"
    assert payload["aiObservability"]["operations"]["resume_parse"]["requests"] == 1
    assert payload["aiObservability"]["operations"]["resume_parse"]["retry_attempts"] == 1
    assert payload["aiObservability"]["operations"]["resume_parse"]["retry_exhausted"] == 1
    assert payload["aiObservability"]["embeddingFallbacks"]["retry_exhausted"] == 1
    assert payload["aiObservability"]["externalContentFailures"] == 1