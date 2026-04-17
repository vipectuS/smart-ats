from fastapi.testclient import TestClient

from app.config import Settings
from app.main import app
from app.routers.embeddings import get_embedding_service
from app.schemas.embedding import EmbeddingResponse
from app.services.embedding import EmbeddingService


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

    service = EmbeddingService(Settings(EMBEDDING_DIMENSIONS=3))
    response = __import__("asyncio").run(service.generate("resume text"))

    assert response == EmbeddingResponse(embedding=[0.3, -0.1, 0.8], dimensions=3)


def test_embedding_service_falls_back_to_local_embedding_when_provider_fails(monkeypatch) -> None:
    async def failing_aembedding(**_kwargs):
        raise RuntimeError("provider unavailable")

    monkeypatch.setattr("app.services.embedding.aembedding", failing_aembedding)

    service = EmbeddingService(Settings(EMBEDDING_DIMENSIONS=8))
    response = __import__("asyncio").run(service.generate("kotlin spring boot redis"))

    assert response.dimensions == 8
    assert len(response.embedding) == 8
    assert any(value != 0.0 for value in response.embedding)