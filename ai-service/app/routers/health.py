from fastapi import APIRouter

from app.config import get_settings


router = APIRouter()


@router.get("/health")
async def health() -> dict[str, str]:
    settings = get_settings()
    return {
        "status": "ok",
        "service": "ai-service",
        "queue": settings.resume_queue_name,
    }
