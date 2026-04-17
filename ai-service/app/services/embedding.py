from __future__ import annotations

from dataclasses import dataclass
import hashlib
import logging
import math
import re

from litellm import aembedding

from app.config import Settings
from app.schemas.embedding import EmbeddingResponse


class EmbeddingGenerationError(RuntimeError):
    pass


logger = logging.getLogger(__name__)


@dataclass
class EmbeddingService:
    settings: Settings

    async def generate(self, text: str) -> EmbeddingResponse:
        try:
            response = await aembedding(
                model=self.settings.embedding_model,
                input=[text],
                dimensions=self.settings.embedding_dimensions,
            )
            data = response["data"] if isinstance(response, dict) else response.data
            embedding = data[0]["embedding"]
        except Exception as exc:  # pragma: no cover - upstream/network failures vary at runtime
            logger.warning("Embedding provider unavailable, falling back to local hashed embedding: %s", exc)
            embedding = self._generate_local_embedding(text)

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

    def _generate_local_embedding(self, text: str) -> list[float]:
        dimensions = self.settings.embedding_dimensions
        vector = [0.0] * dimensions
        tokens = re.findall(r"[a-z0-9+#.]+", text.lower())

        if not tokens:
            tokens = ["empty"]

        for token in tokens:
            digest = hashlib.sha256(token.encode("utf-8")).digest()
            index = int.from_bytes(digest[:4], "big") % dimensions
            sign = 1.0 if digest[4] % 2 == 0 else -1.0
            weight = 0.5 + (digest[5] / 255.0)
            vector[index] += sign * weight

        norm = math.sqrt(sum(value * value for value in vector))
        if norm == 0:
            raise EmbeddingGenerationError("Local embedding fallback produced a zero vector")

        return [round(value / norm, 8) for value in vector]