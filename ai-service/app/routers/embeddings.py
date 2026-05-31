from fastapi import APIRouter, Depends, HTTPException, status

from app.config import Settings, get_settings
from app.schemas.embedding import EmbeddingRequest, EmbeddingResponse
from app.services.litellm_resilience import ResilientLiteLLMClient, get_shared_litellm_client
from app.services.embedding import EmbeddingGenerationError, EmbeddingService

router = APIRouter(prefix="/api/embeddings", tags=["embeddings"])


def get_litellm_client() -> ResilientLiteLLMClient:
    return get_shared_litellm_client()


def get_embedding_service(
    settings: Settings = Depends(get_settings),
    client: ResilientLiteLLMClient = Depends(get_litellm_client),
) -> EmbeddingService:
    return EmbeddingService(settings, resilience_client=client)


@router.post("", response_model=EmbeddingResponse)
async def create_embedding(
    request: EmbeddingRequest,
    service: EmbeddingService = Depends(get_embedding_service),
) -> EmbeddingResponse:
    try:
        return await service.generate(request.text)
    except EmbeddingGenerationError as exc:
        raise HTTPException(status_code=status.HTTP_502_BAD_GATEWAY, detail=str(exc)) from exc