import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.config import get_settings
from app.consumer import ResumeQueueListener
from app.routers.embeddings import router as embeddings_router
from app.routers.health import router as health_router
from app.routers.job_fit_report import router as job_fit_report_router


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
)

settings = get_settings()
listener = ResumeQueueListener(settings)


@asynccontextmanager
async def lifespan(_: FastAPI):
    await listener.start()
    try:
        yield
    finally:
        await listener.stop()


app = FastAPI(title="smart-ats-ai-service", version="0.1.0", lifespan=lifespan)
app.include_router(health_router)
app.include_router(embeddings_router)
app.include_router(job_fit_report_router)
