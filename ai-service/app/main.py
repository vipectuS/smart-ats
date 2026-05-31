import logging
from contextlib import asynccontextmanager
import json
import warnings

from fastapi import FastAPI
from fastapi.exception_handlers import request_validation_exception_handler
from fastapi.exceptions import RequestValidationError
from fastapi.requests import Request
from fastapi.responses import JSONResponse
import litellm
from litellm.litellm_core_utils.model_param_helper import TranscriptionCreateParams

from app.config import get_settings
from app.consumer import ResumeQueueListener
from app.routers.embeddings import router as embeddings_router
from app.routers.health import router as health_router
from app.routers.job_fit_report import router as job_fit_report_router


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
)
logger = logging.getLogger(__name__)

settings = get_settings()


def _apply_runtime_compatibility_patches() -> None:
    warnings.filterwarnings(
        "ignore",
        message="Pydantic serializer warnings:.*",
        category=UserWarning,
    )
    if not hasattr(TranscriptionCreateParams, "__annotations__"):
        TranscriptionCreateParams.__annotations__ = {}


_apply_runtime_compatibility_patches()
litellm.suppress_debug_info = True
litellm.turn_off_message_logging = True
listener = ResumeQueueListener(settings)


def _summarize_validation_body(body: object) -> str:
    if body is None:
        return "<empty>"
    if isinstance(body, bytes):
        text = body.decode("utf-8", errors="replace")
        return text[:800]
    if isinstance(body, (dict, list)):
        try:
            return json.dumps(body, ensure_ascii=False)[:800]
        except TypeError:
            return repr(body)[:800]
    return str(body)[:800]


@asynccontextmanager
async def lifespan(_: FastAPI):
    await listener.start()
    try:
        yield
    finally:
        await listener.stop()


app = FastAPI(title="smart-ats-ai-service", version="0.1.0", lifespan=lifespan)


@app.exception_handler(RequestValidationError)
async def log_request_validation_error(request: Request, exc: RequestValidationError) -> JSONResponse:
    request_body = exc.body
    if request_body is None:
        try:
            request_body = await request.body()
        except RuntimeError:
            request_body = None

    logger.warning(
        "Request validation failed path=%s method=%s client=%s content_type=%s user_agent=%s errors=%s body=%s",
        request.url.path,
        request.method,
        request.client.host if request.client else "unknown",
        request.headers.get("content-type", ""),
        request.headers.get("user-agent", ""),
        exc.errors(),
        _summarize_validation_body(request_body),
    )
    return await request_validation_exception_handler(request, exc)


app.include_router(health_router)
app.include_router(embeddings_router)
app.include_router(job_fit_report_router)
