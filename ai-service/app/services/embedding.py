from __future__ import annotations

from dataclasses import dataclass

from litellm import aembedding

from app.config import Settings
from app.schemas.embedding import EmbeddingResponse


class EmbeddingGenerationError(RuntimeError):
    pass


@dataclass
class EmbeddingService:
    settings: Settings

    async def generate(self, text: str) -> EmbeddingResponse:
        response = await aembedding(
            model=self.settings.embedding_model,
            input=[text],
            dimensions=self.settings.embedding_dimensions,
        )

        try:
            data = response["data"] if isinstance(response, dict) else response.data
            embedding = data[0]["embedding"]
        except (AttributeError, KeyError, IndexError, TypeError) as exc:
            raise EmbeddingGenerationError("Embedding provider returned an invalid payload") from exc

        if not isinstance(embedding, list) or not embedding:
            raise EmbeddingGenerationError("Embedding provider returned an empty vector")

        if len(embedding) != self.settings.embedding_dimensions:
            raise EmbeddingGenerationError(
                f"Embedding dimension mismatch: expected {self.settings.embedding_dimensions}, got {len(embedding)}"
            )

        return EmbeddingResponse(
            embedding=[float(value) for value in embedding],
            dimensions=len(embedding),
        )