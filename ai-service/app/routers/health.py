from fastapi import APIRouter

from app.config import get_settings
from app.services.litellm_resilience import get_ai_observability


router = APIRouter()


@router.get("/health")
async def health() -> dict[str, str]:
    settings = get_settings()
    return {
        "status": "ok",
        "service": "ai-service",
        "queue": settings.resume_queue_name,
    }


@router.get("/health/ai-observability")
async def ai_observability_health() -> dict[str, object]:
    settings = get_settings()
    return {
        "status": "ok",
        "service": "ai-service",
        "queue": settings.resume_queue_name,
        "aiObservability": get_ai_observability().snapshot(),
    }
