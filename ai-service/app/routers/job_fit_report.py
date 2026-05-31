from typing import Any

from fastapi import APIRouter, Body, Depends, HTTPException
from fastapi.exceptions import RequestValidationError
from pydantic import ValidationError

from app.config import Settings, get_settings
from app.schemas.job_fit import JobFitReportRequest, JobFitReportResponse
from app.services.litellm_resilience import ResilientLiteLLMClient, get_shared_litellm_client
from app.services.job_fit_report import JobFitReportGenerationError, JobFitReportService

router = APIRouter(prefix="/api/job-fit-report", tags=["job-fit-report"])


def get_litellm_client() -> ResilientLiteLLMClient:
    return get_shared_litellm_client()


def get_job_fit_report_service(
    settings: Settings = Depends(get_settings),
    client: ResilientLiteLLMClient = Depends(get_litellm_client),
) -> JobFitReportService:
    return JobFitReportService(settings, completion_client=client)


@router.post("", response_model=JobFitReportResponse)
async def create_job_fit_report(
    payload: dict[str, Any] = Body(...),
    service: JobFitReportService = Depends(get_job_fit_report_service),
) -> JobFitReportResponse:
    try:
        request = JobFitReportRequest.model_validate(payload)
    except ValidationError as exc:
        raise RequestValidationError(exc.errors(), body=payload) from exc

    try:
        return await service.generate(request)
    except JobFitReportGenerationError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc