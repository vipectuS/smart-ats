from fastapi import APIRouter, Depends, HTTPException, status

from app.config import Settings, get_settings
from app.schemas.embedding import EmbeddingRequest, EmbeddingResponse
from app.services.embedding import EmbeddingGenerationError, EmbeddingService

router = APIRouter(prefix="/api/embeddings", tags=["embeddings"])


def get_embedding_service(settings: Settings = Depends(get_settings)) -> EmbeddingService:
    return EmbeddingService(settings)


@router.post("", response_model=EmbeddingResponse)
async def create_embedding(
    request: EmbeddingRequest,
    service: EmbeddingService = Depends(get_embedding_service),
) -> EmbeddingResponse:
    try:
        return await service.generate(request.text)
    except EmbeddingGenerationError as exc:
        raise HTTPException(status_code=status.HTTP_502_BAD_GATEWAY, detail=str(exc)) from exc