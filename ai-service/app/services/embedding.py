from __future__ import annotations

from dataclasses import dataclass
import hashlib
import logging
import math
import re

import httpx
from litellm import aembedding

from app.config import Settings
from app.schemas.embedding import EmbeddingResponse
from app.services.ai_observability import AIObservability
from app.services.litellm_resilience import (
    LiteLLMCircuitOpenError,
    LiteLLMRetryExhaustedError,
    ResilientLiteLLMClient,
    get_ai_observability,
    get_shared_litellm_client,
)


class EmbeddingGenerationError(RuntimeError):
    pass


logger = logging.getLogger(__name__)


@dataclass
class EmbeddingService:
    settings: Settings
    resilience_client: ResilientLiteLLMClient | None = None
    observability: AIObservability | None = None

    def __post_init__(self) -> None:
        if self.resilience_client is None:
            self.resilience_client = get_shared_litellm_client()
        if self.observability is None:
            self.observability = get_ai_observability()

    async def generate(self, text: str) -> EmbeddingResponse:
        try:
            embedding = await self._generate_remote_embedding(text)
        except (LiteLLMRetryExhaustedError, LiteLLMCircuitOpenError) as exc:
            logger.warning("Embedding provider unavailable, falling back to local hashed embedding: %s", exc)
            assert self.observability is not None
            reason = "circuit_open" if isinstance(exc, LiteLLMCircuitOpenError) else "retry_exhausted"
            self.observability.record_embedding_fallback(reason)
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

    async def _generate_remote_embedding(self, text: str) -> list[float]:
        assert self.resilience_client is not None
        return await self.resilience_client.execute(
            operation="embedding_generate",
            action=lambda: self._generate_remote_embedding_once(text),
        )

    async def _generate_remote_embedding_once(self, text: str) -> list[float]:
        normalized_model = self._normalize_model_name(self.settings.embedding_model)
        if self._should_use_dashscope_embedding_endpoint(normalized_model):
            return await self._generate_dashscope_embedding(text, normalized_model)

        response = await aembedding(
            model=self.settings.embedding_model,
            input=[text],
            dimensions=self.settings.embedding_dimensions,
            **self.settings.litellm_request_options(),
        )
        data = response["data"] if isinstance(response, dict) else response.data
        return data[0]["embedding"]

    def _normalize_model_name(self, model: str) -> str:
        if "/" in model:
            return model.split("/", 1)[1]
        return model

    def _should_use_dashscope_embedding_endpoint(self, normalized_model: str) -> bool:
        return (
            bool(self.settings.dashscope_api_key)
            and bool(self.settings.dashscope_api_base)
            and normalized_model.startswith("text-embedding-")
        )

    async def _generate_dashscope_embedding(self, text: str, normalized_model: str) -> list[float]:
        assert self.settings.dashscope_api_key is not None
        assert self.settings.dashscope_api_base is not None

        endpoint = self.settings.dashscope_api_base.rstrip("/") + "/embeddings"
        async with httpx.AsyncClient(timeout=self.settings.external_content_timeout_seconds) as client:
            response = await client.post(
                endpoint,
                headers={
                    "Authorization": f"Bearer {self.settings.dashscope_api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": normalized_model,
                    "input": text,
                    "dimensions": self.settings.embedding_dimensions,
                    "encoding_format": "float",
                },
            )
            response.raise_for_status()

        payload = response.json()
        data = payload.get("data")
        if not isinstance(data, list) or not data:
            raise EmbeddingGenerationError("DashScope embedding response missing data array")

        first = data[0]
        if not isinstance(first, dict) or not isinstance(first.get("embedding"), list):
            raise EmbeddingGenerationError("DashScope embedding response missing embedding vector")

        return first["embedding"]

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