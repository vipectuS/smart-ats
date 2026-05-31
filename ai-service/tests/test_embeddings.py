from fastapi.testclient import TestClient
import httpx

from app.config import Settings
from app.main import app
from app.routers.embeddings import get_embedding_service
from app.schemas.embedding import EmbeddingResponse
from app.services.ai_observability import AIObservability
from app.services.embedding import EmbeddingService
from app.services.litellm_resilience import ResilientLiteLLMClient


class FakeResilienceClient:
    def __init__(self) -> None:
        self.operations: list[str] = []

    async def execute(self, *, operation: str, action):
        self.operations.append(operation)
        return await action()


class StubEmbeddingService:
    async def generate(self, text: str) -> EmbeddingResponse:
        assert text == "kotlin spring boot"
        return EmbeddingResponse(embedding=[0.12, -0.45, 0.67], dimensions=3)


def test_embeddings_endpoint_returns_expected_payload() -> None:
    app.dependency_overrides[get_embedding_service] = lambda: StubEmbeddingService()

    try:
        with TestClient(app) as client:
            response = client.post("/api/embeddings", json={"text": "kotlin spring boot"})
    finally:
        app.dependency_overrides.clear()

    assert response.status_code == 200
    assert response.json() == {
        "embedding": [0.12, -0.45, 0.67],
        "dimensions": 3,
    }


def test_embedding_service_uses_litellm_and_enforces_dimensions(monkeypatch) -> None:
    async def fake_aembedding(**kwargs):
        assert kwargs["model"] == "text-embedding-3-small"
        assert kwargs["dimensions"] == 3
        assert kwargs["input"] == ["resume text"]
        return {"data": [{"embedding": [0.3, -0.1, 0.8]}]}

    monkeypatch.setattr("app.services.embedding.aembedding", fake_aembedding)

    service = EmbeddingService(
        Settings(
            EMBEDDING_DIMENSIONS=3,
            EMBEDDING_MODEL="text-embedding-3-small",
            DASHSCOPE_API_KEY="",
            DASHSCOPE_API_BASE="",
        ),
        resilience_client=FakeResilienceClient(),
    )
    response = __import__("asyncio").run(service.generate("resume text"))

    assert response == EmbeddingResponse(embedding=[0.3, -0.1, 0.8], dimensions=3)


def test_embedding_service_uses_dashscope_compatible_endpoint_for_text_embedding_v4(monkeypatch) -> None:
    captured: dict[str, object] = {}

    class StubAsyncClient:
        def __init__(self, *args, **kwargs):
            captured["timeout"] = kwargs.get("timeout")

        async def __aenter__(self):
            return self

        async def __aexit__(self, exc_type, exc, tb):
            return None

        async def post(self, url, headers=None, json=None):
            captured["url"] = url
            captured["headers"] = headers
            captured["json"] = json
            request = httpx.Request("POST", url)
            return httpx.Response(
                200,
                request=request,
                json={"data": [{"embedding": [0.1, 0.2, 0.3]}]},
            )

    monkeypatch.setattr("app.services.embedding.httpx.AsyncClient", StubAsyncClient)

    service = EmbeddingService(
        Settings(
            EMBEDDING_MODEL="text-embedding-v4",
            EMBEDDING_DIMENSIONS=3,
            DASHSCOPE_API_KEY="test-key",
            DASHSCOPE_API_BASE="https://dashscope.aliyuncs.com/compatible-mode/v1",
            EXTERNAL_CONTENT_TIMEOUT_SECONDS=7,
        ),
        resilience_client=FakeResilienceClient(),
    )
    response = __import__("asyncio").run(service.generate("resume text"))

    assert response == EmbeddingResponse(embedding=[0.1, 0.2, 0.3], dimensions=3)
    assert captured["url"] == "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings"
    assert captured["headers"] == {
        "Authorization": "Bearer test-key",
        "Content-Type": "application/json",
    }
    assert captured["json"] == {
        "model": "text-embedding-v4",
        "input": "resume text",
        "dimensions": 3,
        "encoding_format": "float",
    }


def test_embedding_service_falls_back_to_local_embedding_when_provider_fails(monkeypatch) -> None:
    async def failing_aembedding(**_kwargs):
        raise RuntimeError("provider unavailable")

    monkeypatch.setattr("app.services.embedding.aembedding", failing_aembedding)
    observability = AIObservability()

    service = EmbeddingService(
        Settings(
            EMBEDDING_DIMENSIONS=8,
            AI_COMPLETION_MAX_ATTEMPTS=1,
            DASHSCOPE_API_KEY="",
            DASHSCOPE_API_BASE="",
        ),
        resilience_client=ResilientLiteLLMClient(
            Settings(
                EMBEDDING_DIMENSIONS=8,
                AI_COMPLETION_MAX_ATTEMPTS=1,
                DASHSCOPE_API_KEY="",
                DASHSCOPE_API_BASE="",
            ),
            observability=observability,
        ),
        observability=observability,
    )
    response = __import__("asyncio").run(service.generate("kotlin spring boot redis"))
    snapshot = observability.snapshot()

    assert response.dimensions == 8
    assert len(response.embedding) == 8
    assert any(value != 0.0 for value in response.embedding)
    assert snapshot["embeddingFallbacks"]["retry_exhausted"] == 1


def test_embedding_service_routes_remote_calls_through_shared_resilience_client(monkeypatch) -> None:
    async def fake_aembedding(**kwargs):
        assert kwargs["model"] == "text-embedding-3-small"
        return {"data": [{"embedding": [0.2, 0.4, 0.6]}]}

    monkeypatch.setattr("app.services.embedding.aembedding", fake_aembedding)

    client = FakeResilienceClient()
    service = EmbeddingService(
        Settings(
            EMBEDDING_DIMENSIONS=3,
            EMBEDDING_MODEL="text-embedding-3-small",
            DASHSCOPE_API_KEY="",
            DASHSCOPE_API_BASE="",
        ),
        resilience_client=client,
    )

    response = __import__("asyncio").run(service.generate("resume text"))

    assert response == EmbeddingResponse(embedding=[0.2, 0.4, 0.6], dimensions=3)
    assert client.operations == ["embedding_generate"]