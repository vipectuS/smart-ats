from decimal import Decimal
from typing import Any, Literal

from pydantic import Field

from app.schemas.common import CamelModel


class JobFitReportRequest(CamelModel):
    audience: Literal["candidate", "hr"]
    candidate_name: str = Field(min_length=1)
    job_title: str = Field(min_length=1)
    job_description: str = Field(min_length=1)
    job_requirements: dict[str, Any] | None = Field(default=None)
    match_score: Decimal = Field(ge=0, le=100)
    semantic_score: Decimal = Field(ge=0, le=100)
    skill_score: Decimal = Field(ge=0, le=100)
    experience_score: Decimal = Field(ge=0, le=100)
    education_score: Decimal = Field(ge=0, le=100)
    matched_skills: list[str] = Field(default_factory=list)
    missing_skills: list[str] = Field(default_factory=list)


class JobFitReportResponse(CamelModel):
    headline: str = Field(min_length=1)
    fit_band: Literal["HIGH", "MEDIUM", "LOW"]
    summary: str = Field(min_length=1)
    strengths: list[str] = Field(default_factory=list)
    risks: list[str] = Field(default_factory=list)
    improvement_suggestions: list[str] = Field(default_factory=list)
    next_steps: list[str] = Field(default_factory=list)
    narrative: str = Field(min_length=1)