from __future__ import annotations

import asyncio
from abc import ABC, abstractmethod
from dataclasses import dataclass
import json
import logging
from pathlib import Path
import re
from typing import Any

from litellm import acompletion
from pydantic import ValidationError

from app.config import Settings
from app.schemas.resume import (
    BasicInfo,
    BrowserResumePreprocessedPayload,
    EducationExperience,
    RadarScores,
    ResumeParseMessage,
    Skill,
    TalentProfile,
    WorkExperience,
)
from app.services.content_extractor import ExtractedRemoteContent, RemoteContentExtractor


class ResumeParsingError(RuntimeError):
    pass


MAX_BROWSER_CONTEXT_PREVIEW_PAGES = 3
MAX_EXTERNAL_CONTENT_ITEMS = 2
MAX_EXTERNAL_CONTENT_CHARS = 1800


logger = logging.getLogger(__name__)


@dataclass
class ExternalContentContext:
    source_type: str
    url: str
    text: str
    title: str | None = None
    description: str | None = None
    structured_summary: str | None = None


class BaseResumeParser(ABC):
    @abstractmethod
    async def parse(self, message: ResumeParseMessage) -> TalentProfile:
        raise NotImplementedError


class MockResumeParser(BaseResumeParser):
    SAMPLE_ID_PATTERN = re.compile(r"(C\d{2})", re.IGNORECASE)

    def __init__(self, settings: Settings | None = None, extractor: RemoteContentExtractor | None = None) -> None:
        self.settings = settings or Settings()
        self.extractor = extractor

    async def parse(self, message: ResumeParseMessage) -> TalentProfile:
        synthetic_profile = self._load_synthetic_profile(message)
        candidate_name = synthetic_profile.basic_info.full_name if synthetic_profile else self._build_candidate_name(message.raw_content_reference)
        browser_summary = self._build_browser_summary(message.browser_preprocessed_payload)
        external_context, external_warnings = await collect_external_content_context(message, self.extractor)
        summary = self._build_summary(browser_summary, external_context, external_warnings)
        if synthetic_profile is not None:
            return self._merge_summary_into_profile(synthetic_profile, summary)

        return TalentProfile(
            basicInfo=BasicInfo(
                fullName=candidate_name,
                email="candidate@example.com",
                phone="13800000000",
                location="Shanghai",
                headline="Full Stack Engineer",
                summary=summary,
            ),
            workExperiences=[
                WorkExperience(
                    company="Smart ATS Labs",
                    title="Senior Software Engineer",
                    startDate="2021-03",
                    endDate="Present",
                    responsibilities=[
                        "Led resume parsing pipeline integration",
                        "Built API callback and failure recovery flows",
                    ],
                    achievements=[
                        "Reduced parsing turnaround time via async processing",
                    ],
                ),
            ],
            educationExperiences=[
                EducationExperience(
                    school="Zhejiang University",
                    degree="Bachelor of Engineering",
                    fieldOfStudy="Computer Science",
                    startDate="2016-09",
                    endDate="2020-06",
                ),
            ],
            skills=[
                Skill(name="Python", category="Programming", proficiency="Advanced", evidence="Backend orchestration"),
                Skill(name="FastAPI", category="Framework", proficiency="Advanced", evidence="AI microservice APIs"),
                Skill(name="Redis", category="Infrastructure", proficiency="Intermediate", evidence="Async queue consumption"),
            ],
            radarScores=RadarScores(
                communication=82,
                technicalDepth=88,
                problemSolving=90,
                collaboration=84,
                leadership=76,
                adaptability=87,
            ),
            xaiReasoning=(
                "The candidate shows strong backend implementation depth, demonstrated by asynchronous queue "
                "handling, schema-first API integration, and reliable callback design."
            ),
        )

    def _load_synthetic_profile(self, message: ResumeParseMessage) -> TalentProfile | None:
        sample_id = self._extract_sample_id(message)
        if sample_id is None:
            return None

        dataset_dir = Path(self.settings.synthetic_dataset_dir).expanduser().resolve()
        truth_dir = dataset_dir / "truth"
        if not truth_dir.exists():
            return None

        truth_match = next(truth_dir.glob(f"{sample_id}_*.json"), None)
        if truth_match is None:
            return None

        try:
            payload = json.loads(truth_match.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            logger.warning("Failed to load synthetic truth profile for %s", sample_id)
            return None

        return self._truth_payload_to_profile(sample_id, payload)

    def _extract_sample_id(self, message: ResumeParseMessage) -> str | None:
        candidates = [message.raw_content_reference]
        if message.browser_preprocessed_payload is not None:
            candidates.extend(
                [
                    message.browser_preprocessed_payload.source_file_name,
                    message.browser_preprocessed_payload.derived_reference,
                ],
            )

        for candidate in candidates:
            if not candidate:
                continue
            match = self.SAMPLE_ID_PATTERN.search(candidate)
            if match:
                return match.group(1).upper()
        return None

    def _truth_payload_to_profile(self, sample_id: str, payload: dict[str, Any]) -> TalentProfile:
        skill_names = [item for item in payload.get("skills", []) if isinstance(item, str) and item.strip()]
        work_experiences = [
            WorkExperience(
                company=str(item.get("company", "未知公司")),
                title=str(item.get("title", "未命名岗位")),
                startDate=str(item.get("startDate", "未知开始时间")),
                endDate=item.get("endDate"),
                responsibilities=item.get("responsibilities", []) if isinstance(item.get("responsibilities"), list) else [],
                achievements=item.get("achievements", []) if isinstance(item.get("achievements"), list) else [],
            )
            for item in payload.get("workExperiences", [])
            if isinstance(item, dict)
        ]
        education_experiences = [
            EducationExperience(
                school=str(item.get("school", "未知院校")),
                degree=str(item.get("degree", "未知学历")),
                fieldOfStudy=item.get("fieldOfStudy"),
                startDate=item.get("startDate"),
                endDate=item.get("endDate"),
            )
            for item in payload.get("educationExperiences", [])
            if isinstance(item, dict)
        ]

        return TalentProfile(
            basicInfo=BasicInfo(
                fullName=str(payload.get("candidateName", sample_id)),
                email=payload.get("email"),
                phone=payload.get("phone"),
                location=self._infer_location(payload),
                headline=self._infer_headline(skill_names),
                summary=payload.get("summary"),
            ),
            workExperiences=work_experiences,
            educationExperiences=education_experiences,
            skills=[Skill(name=name, category=self._infer_skill_category(name)) for name in skill_names],
            radarScores=self._build_radar_scores(skill_names, len(work_experiences), len(education_experiences)),
            xaiReasoning=(
                f"Synthetic dataset profile {sample_id} was reconstructed from the repository truth JSON, "
                f"highlighting primary strengths in {', '.join(skill_names[:4]) or 'general engineering'} and preserving deterministic demo output."
            ),
        )

    def _merge_summary_into_profile(self, profile: TalentProfile, extra_summary: str | None) -> TalentProfile:
        if not extra_summary:
            return profile

        merged_summary = " ".join(
            part.strip()
            for part in [profile.basic_info.summary or "", extra_summary]
            if part and part.strip()
        )
        return profile.model_copy(
            update={
                "basic_info": profile.basic_info.model_copy(update={"summary": merged_summary}),
            },
        )

    def _infer_location(self, payload: dict[str, Any]) -> str | None:
        summary = str(payload.get("summary", ""))
        for city in ["Shanghai", "Hangzhou", "Nanjing", "Suzhou", "Beijing", "Shenzhen"]:
            if city.lower() in summary.lower():
                return city
        return "Shanghai"

    def _infer_headline(self, skill_names: list[str]) -> str:
        lowered = {skill.lower() for skill in skill_names}
        if {"kotlin", "spring boot", "java"} & lowered:
            return "Backend Engineer"
        if {"vue", "typescript", "javascript"} & lowered:
            return "Frontend Engineer"
        if {"pytest", "selenium", "api testing"} & lowered:
            return "QA Automation Engineer"
        if {"linux", "docker", "kubernetes"} & lowered:
            return "DevOps Engineer"
        return "Software Engineer"

    def _infer_skill_category(self, skill_name: str) -> str:
        lowered = skill_name.lower()
        if lowered in {"kotlin", "java", "python", "typescript", "javascript", "sql"}:
            return "Programming"
        if lowered in {"spring boot", "vue", "vite", "node.js", "pandas"}:
            return "Framework"
        if lowered in {"postgresql", "redis", "docker", "kubernetes", "linux"}:
            return "Infrastructure"
        if lowered in {"selenium", "pytest", "echarts", "data visualization"}:
            return "Tooling"
        return "General"

    def _build_radar_scores(self, skill_names: list[str], work_count: int, education_count: int) -> RadarScores:
        base = min(92, 58 + len(skill_names) * 4)
        work_bonus = min(12, work_count * 5)
        edu_bonus = 6 if education_count > 0 else 0
        return RadarScores(
            communication=min(95, 68 + work_bonus),
            technicalDepth=min(96, base + work_bonus),
            problemSolving=min(95, 66 + work_bonus + len(skill_names) * 2),
            collaboration=min(94, 64 + work_bonus),
            leadership=min(90, 52 + work_count * 4),
            adaptability=min(94, 65 + edu_bonus + len(skill_names) * 2),
        )

    def _build_candidate_name(self, raw_content_reference: str) -> str:
        stem = Path(raw_content_reference).stem.replace("_", " ").replace("-", " ").strip()
        if not stem:
            return "Unknown Candidate"
        return " ".join(part.capitalize() for part in stem.split())

    def _build_browser_summary(self, payload: BrowserResumePreprocessedPayload | None) -> str | None:
        if payload is None:
            return None

        text_preview = (payload.extracted_text_preview or "").strip()
        if text_preview:
            return (
                f"Browser-side PDF preprocessing rendered {payload.page_count} pages before parsing. "
                f"Text preview: {text_preview[:220]}"
            )

        return f"Browser-side PDF preprocessing rendered {payload.page_count} pages before parsing."

    def _build_summary(
        self,
        browser_summary: str | None,
        external_context: list[ExternalContentContext],
        external_warnings: list[str],
    ) -> str:
        summary_parts: list[str] = []
        if browser_summary:
            summary_parts.append(browser_summary)

        external_summary = build_external_content_summary(external_context, external_warnings)
        if external_summary:
            summary_parts.append(external_summary)

        if summary_parts:
            return " ".join(summary_parts)

        return "Structured mock profile generated while LiteLLM credentials are not configured."


class LiteLLMResumeParser(BaseResumeParser):
    def __init__(self, settings: Settings, extractor: RemoteContentExtractor | None = None) -> None:
        self.settings = settings
        self.extractor = extractor or RemoteContentExtractor(settings)

    async def parse(self, message: ResumeParseMessage) -> TalentProfile:
        external_context, external_warnings = await collect_external_content_context(message, self.extractor)
        request_payload = self.build_request(message, external_context, external_warnings)
        response = await acompletion(**request_payload)
        content = self._extract_content(response)

        try:
            return TalentProfile.model_validate_json(content)
        except ValidationError as exc:
            raise ResumeParsingError("LiteLLM returned invalid talent profile JSON") from exc

    def build_request(
        self,
        message: ResumeParseMessage,
        external_context: list[ExternalContentContext] | None = None,
        external_warnings: list[str] | None = None,
    ) -> dict[str, Any]:
        return {
            "model": self.settings.litellm_model,
            "temperature": 0,
            "messages": [
                {
                    "role": "system",
                    "content": (
                        "You are a multi-modal resume parsing engine. Return only JSON matching the provided schema. "
                        "Use browser-side PDF previews, extracted HTML portfolio text, GitHub page text, and backend "
                        "resume references when available. "
                        "Extract basic info, work history, education, skills, radar scores, and XAI reasoning."
                    ),
                },
                {
                    "role": "user",
                    "content": self._build_user_content(message, external_context or [], external_warnings or []),
                },
            ],
            "response_format": {
                "type": "json_schema",
                "json_schema": {
                    "name": "talent_profile",
                    "schema": TalentProfile.model_json_schema(by_alias=True),
                },
            },
        }

    def _build_user_content(
        self,
        message: ResumeParseMessage,
        external_context: list[ExternalContentContext],
        external_warnings: list[str],
    ) -> str | list[dict[str, Any]]:
        payload = message.browser_preprocessed_payload
        if payload is None and not external_context and not external_warnings:
            return (
                "Parse the resume referenced by this backend content identifier and emit only valid JSON: "
                f"{message.raw_content_reference}"
            )

        content: list[dict[str, Any]] = [
            {
                "type": "text",
                "text": self._build_context_text(message, payload, external_context, external_warnings),
            },
        ]

        if payload is not None:
            for page in payload.page_previews[:MAX_BROWSER_CONTEXT_PREVIEW_PAGES]:
                if page.text_preview:
                    content.append(
                        {
                            "type": "text",
                            "text": f"Page {page.page_number} text preview: {page.text_preview}",
                        },
                    )
                content.append(
                    {
                        "type": "image_url",
                        "image_url": {
                            "url": page.image_data_url,
                        },
                    },
                )

        for item in external_context:
            content.append(
                {
                    "type": "text",
                    "text": self._build_external_content_prompt(item),
                },
            )

        return content

    def _build_external_content_prompt(self, item: ExternalContentContext) -> str:
        if item.structured_summary:
            return item.structured_summary

        if item.source_type == "github":
            return f"External github repository context extracted from {item.url}: readme_excerpt={item.text}"

        return f"External {item.source_type} content extracted from {item.url}: {item.text}"

    def _build_context_text(
        self,
        message: ResumeParseMessage,
        payload: BrowserResumePreprocessedPayload | None,
        external_context: list[ExternalContentContext],
        external_warnings: list[str],
    ) -> str:
        parts = [
            "Parse the candidate resume using the richest context first, then fall back to the backend resume reference "
            f"if needed. Backend reference: {message.raw_content_reference}."
        ]

        if payload is not None:
            extracted_text = (payload.extracted_text_preview or "").strip()
            warnings = "; ".join(payload.warnings) if payload.warnings else "none"
            attached_preview_pages = len(payload.page_previews)
            parts.append(
                "browser-preprocessed PDF payload available: "
                f"engine={payload.engine}, mode={payload.mode}, sourceFile={payload.source_file_name}, "
                f"pageCount={payload.page_count}, attachedPreviewPages={attached_preview_pages}, warnings={warnings}. "
                f"Combined text preview: {extracted_text or 'none provided'}."
            )

        if external_context:
            references = ", ".join(f"{item.source_type}:{item.url}" for item in external_context)
            parts.append(f"External web context extracted successfully from {len(external_context)} source(s): {references}.")

            github_items = [item for item in external_context if item.source_type == "github"]
            if github_items:
                parts.append(
                    "GitHub inputs are normalized into minimal repository fields such as owner/repo, description, "
                    "README excerpt, languages, and recent public signals before parsing."
                )

        if external_warnings:
            parts.append(f"External fetch warnings: {'; '.join(external_warnings)}.")

        return " ".join(parts)

    def _extract_content(self, response: Any) -> str:
        content = response.choices[0].message.content
        if isinstance(content, str):
            return content
        if isinstance(content, list):
            return "".join(part.get("text", "") for part in content if isinstance(part, dict))
        raise ResumeParsingError("LiteLLM response did not contain string content")


def build_resume_parser(settings: Settings) -> BaseResumeParser:
    provider = settings.resume_parser_provider.strip().lower()
    if provider == "litellm":
        return LiteLLMResumeParser(settings)
    if provider == "mock":
        return MockResumeParser(settings=settings, extractor=RemoteContentExtractor(settings))
    raise ValueError(f"Unsupported resume parser provider: {settings.resume_parser_provider}")


async def collect_external_content_context(
    message: ResumeParseMessage,
    extractor: RemoteContentExtractor | None,
) -> tuple[list[ExternalContentContext], list[str]]:
    references = message.external_content_references[:MAX_EXTERNAL_CONTENT_ITEMS]
    if not references or extractor is None:
        return [], []

    fetches = await asyncio.gather(
        *(extractor.extract_content(reference.source_type, reference.url) for reference in references),
        return_exceptions=True,
    )

    context: list[ExternalContentContext] = []
    warnings: list[str] = []

    for reference, result in zip(references, fetches, strict=False):
        if isinstance(result, Exception):
            warning = f"{reference.source_type} {reference.url} unavailable: {result}"
            logger.warning("Failed to extract external content from %s: %s", reference.url, result)
            warnings.append(warning)
            continue

        assert isinstance(result, ExtractedRemoteContent)
        normalized_text = " ".join(result.plain_text.split())
        if not normalized_text:
            warnings.append(f"{reference.source_type} {reference.url} returned empty content")
            continue

        context.append(
            ExternalContentContext(
                source_type=reference.source_type,
                url=reference.url,
                text=normalized_text[:MAX_EXTERNAL_CONTENT_CHARS],
                title=result.title,
                description=result.description,
                structured_summary=build_structured_external_content_summary(result, normalized_text[:MAX_EXTERNAL_CONTENT_CHARS]),
            )
        )

    return context, warnings


def build_external_content_summary(
    external_context: list[ExternalContentContext],
    external_warnings: list[str],
) -> str | None:
    parts: list[str] = []

    if external_context:
        excerpts = " ".join(
            f"[{item.source_type}] {item.text[:180]}" for item in external_context
        )
        parts.append(f"External HTML/GitHub context extracted: {excerpts}")

    if external_warnings:
        parts.append(f"External source warnings: {'; '.join(external_warnings)}")

    if not parts:
        return None

    return " ".join(parts)


def build_structured_external_content_summary(
    extracted: ExtractedRemoteContent,
    truncated_plain_text: str,
) -> str | None:
    if extracted.source_type != "github":
        title_bits: list[str] = []
        if extracted.title:
            title_bits.append(f"title={extracted.title}")
        if extracted.description:
            title_bits.append(f"description={extracted.description}")
        prefix = "; ".join(title_bits)
        if prefix:
            return f"External {extracted.source_type} content extracted from {extracted.url}: {prefix}. Plain text excerpt: {truncated_plain_text}"
        return None

    github_fields: list[str] = []
    if extracted.github_owner and extracted.github_repo:
        github_fields.append(f"repo={extracted.github_owner}/{extracted.github_repo}")
    if extracted.description:
        github_fields.append(f"description={extracted.description}")
    if extracted.github_languages:
        github_fields.append(f"languages={', '.join(extracted.github_languages)}")
    if extracted.github_recent_public_signals:
        github_fields.append(f"recent_public_signals={'; '.join(extracted.github_recent_public_signals)}")
    if extracted.github_readme_excerpt:
        github_fields.append(f"readme_excerpt={extracted.github_readme_excerpt}")

    if not github_fields:
        return f"External github content extracted from {extracted.url}: plain_text={truncated_plain_text}"

    return f"External github repository context extracted from {extracted.url}: {'; '.join(github_fields)}"