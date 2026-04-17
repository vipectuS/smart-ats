from __future__ import annotations

import json
from pathlib import Path
from uuid import uuid4

import httpx
import pytest

from app.config import Settings
from app.consumer import ResumeQueueListener
from app.schemas.resume import BasicInfo, RadarScores, TalentProfile
from app.services.callbacks import BackendCallbackClient, BackendCallbackError
from app.services.parser import ExternalContentContext, LiteLLMResumeParser, MockResumeParser
from app.services.content_extractor import RemoteContentExtractor


def build_settings() -> Settings:
    settings = Settings()
    settings.backend_base_url = "http://backend.test"
    settings.internal_callback_api_key = "test-key"
    settings.backend_parse_failed_path = "/internal/api/resumes/{resumeId}/parse-failed"
    return settings


def build_payload(include_external_refs: bool = False) -> str:
    payload = {
        "resumeId": str(uuid4()),
        "rawContentReference": "resumes/jane_doe.pdf",
        "browserPreprocessedPayload": {
            "engine": "pdfjs-browser-renderer",
            "mode": "pdf-to-page-previews",
            "sourceFileName": "jane_doe.pdf",
            "sourceMimeType": "application/pdf",
            "sourceFileSize": 4096,
            "derivedReference": "browser-pdf-preview://demo/2p/jane_doe.pdf",
            "pageCount": 2,
            "extractedTextPreview": "Jane Doe backend engineer with Python and FastAPI experience",
            "generatedAt": "2026-04-05T08:00:00Z",
            "warnings": ["preview-payload-batched"],
            "pagePreviews": [
                {
                    "pageNumber": 1,
                    "width": 280,
                    "height": 396,
                    "imageDataUrl": "data:image/jpeg;base64,preview-1",
                    "textPreview": "Jane Doe backend engineer",
                },
            ],
        },
        "requestedAt": "2026-03-24T12:00:00Z",
    }
    if include_external_refs:
        payload["externalContentReferences"] = [
            {
                "sourceType": "github",
                "url": "https://github.com/jane-doe/project",
            },
            {
                "sourceType": "portfolio",
                "url": "https://jane.dev/portfolio",
            },
        ]
    return json.dumps(payload)


def build_profile() -> TalentProfile:
    return TalentProfile(
        basicInfo=BasicInfo(fullName="Jane Doe", email="jane@example.com"),
        skills=[{"name": "Python", "category": "Programming"}],
        radarScores=RadarScores(
            communication=80,
            technicalDepth=88,
            problemSolving=90,
            collaboration=84,
            leadership=72,
            adaptability=86,
        ),
        xaiReasoning="Consistent backend and integration depth across resume evidence.",
    )


class SuccessfulParser:
    async def parse(self, message):
        return build_profile()


class FailingParser:
    async def parse(self, message):
        raise RuntimeError("model timeout")


class StubExtractor:
    async def extract_content(self, source_type: str, url: str):
        from app.services.content_extractor import ExtractedRemoteContent

        if "github.com" in url:
            return ExtractedRemoteContent(
                source_type=source_type,
                url=url,
                plain_text="README highlights FastAPI, Docker, and CI pipelines",
                title="jane-doe/project",
                description="Resume parsing and dashboard project",
                github_owner="jane-doe",
                github_repo="project",
                github_readme_excerpt="README highlights FastAPI, Docker, and CI pipelines",
                github_languages=["Python", "TypeScript"],
                github_recent_public_signals=["Updated 2 days ago", "Stars 12"],
            )
        if "jane.dev" in url:
            return ExtractedRemoteContent(
                source_type=source_type,
                url=url,
                plain_text="Portfolio case study covers resume parsing UX and dashboard delivery",
                title="Jane Portfolio",
                description="Portfolio case study collection",
            )
        raise RuntimeError("unexpected url")


class RecordingCallbackClient:
    def __init__(self) -> None:
        self.submitted: list[tuple[str, TalentProfile]] = []
        self.failures: list[tuple[str, str]] = []
        self.raise_on_submit = False

    async def submit_parsed_result(self, resume_id, profile: TalentProfile) -> None:
        if self.raise_on_submit:
            raise BackendCallbackError("upload failed")
        self.submitted.append((str(resume_id), profile))

    async def report_failure(self, resume_id, reason: str) -> None:
        self.failures.append((str(resume_id), reason))

    async def close(self) -> None:
        return None


@pytest.mark.anyio
async def test_backend_callback_client_posts_parsed_result_with_internal_api_key() -> None:
    settings = build_settings()
    captured: dict[str, object] = {}

    async def handler(request: httpx.Request) -> httpx.Response:
        captured["path"] = request.url.path
        captured["header"] = request.headers.get("X-Internal-Api-Key")
        captured["body"] = json.loads(request.content.decode())
        return httpx.Response(200, json={"status": 200})

    transport = httpx.MockTransport(handler)
    async with httpx.AsyncClient(base_url=settings.backend_base_url, transport=transport) as http_client:
        client = BackendCallbackClient(settings, http_client=http_client)
        await client.submit_parsed_result(uuid4(), build_profile())

    assert captured["path"].startswith("/internal/api/resumes/")
    assert captured["path"].endswith("/parsed-result")
    assert captured["header"] == "test-key"
    assert captured["body"]["parsedData"]["basicInfo"]["fullName"] == "Jane Doe"
    assert captured["body"]["parsedData"]["radarScores"]["technicalDepth"] == 88


@pytest.mark.anyio
async def test_resume_queue_listener_submits_parsed_result_when_parsing_succeeds() -> None:
    callback_client = RecordingCallbackClient()
    listener = ResumeQueueListener(
        build_settings(),
        parser=SuccessfulParser(),
        callback_client=callback_client,
    )

    payload = build_payload()
    resume_id = json.loads(payload)["resumeId"]
    await listener._handle_payload(payload)

    assert [entry[0] for entry in callback_client.submitted] == [resume_id]
    assert callback_client.failures == []


@pytest.mark.anyio
async def test_resume_queue_listener_reports_failure_when_parser_raises() -> None:
    callback_client = RecordingCallbackClient()
    listener = ResumeQueueListener(
        build_settings(),
        parser=FailingParser(),
        callback_client=callback_client,
    )

    payload = build_payload()
    resume_id = json.loads(payload)["resumeId"]
    await listener._handle_payload(payload)

    assert callback_client.submitted == []
    assert callback_client.failures == [(resume_id, "model timeout")]


@pytest.mark.anyio
async def test_resume_queue_listener_reports_failure_when_upload_raises() -> None:
    callback_client = RecordingCallbackClient()
    callback_client.raise_on_submit = True
    listener = ResumeQueueListener(
        build_settings(),
        parser=SuccessfulParser(),
        callback_client=callback_client,
    )

    payload = build_payload()
    resume_id = json.loads(payload)["resumeId"]
    await listener._handle_payload(payload)

    assert callback_client.submitted == []
    assert callback_client.failures == [(resume_id, "upload failed")]


def test_litellm_parser_request_uses_json_schema_contract() -> None:
    from app.schemas.resume import ResumeParseMessage

    parser = LiteLLMResumeParser(build_settings())
    message = ResumeParseMessage.model_validate_json(build_payload())

    request = parser.build_request(message)

    assert request["model"] == build_settings().litellm_model
    assert request["response_format"]["type"] == "json_schema"
    assert request["response_format"]["json_schema"]["name"] == "talent_profile"
    assert "basicInfo" in request["response_format"]["json_schema"]["schema"]["properties"]


def test_litellm_parser_request_includes_browser_preprocessed_context() -> None:
    from app.schemas.resume import ResumeParseMessage

    parser = LiteLLMResumeParser(build_settings())
    message = ResumeParseMessage.model_validate_json(build_payload())

    request = parser.build_request(message)
    user_content = request["messages"][1]["content"]

    assert isinstance(user_content, list)
    assert user_content[0]["type"] == "text"
    assert "browser-preprocessed PDF payload" in user_content[0]["text"]
    assert any(item["type"] == "image_url" for item in user_content)


@pytest.mark.anyio
async def test_mock_parser_summary_includes_external_content_when_present() -> None:
    from app.schemas.resume import ResumeParseMessage

    parser = MockResumeParser(settings=build_settings(), extractor=StubExtractor())
    message = ResumeParseMessage.model_validate_json(build_payload(include_external_refs=True))

    profile = await parser.parse(message)

    assert profile.basic_info.summary is not None
    assert "External HTML/GitHub context extracted" in profile.basic_info.summary
    assert "FastAPI" in profile.basic_info.summary
    assert "Portfolio case study" in profile.basic_info.summary


@pytest.mark.anyio
async def test_mock_parser_loads_synthetic_truth_profile_when_sample_matches(tmp_path: Path) -> None:
    from app.schemas.resume import ResumeParseMessage

    truth_dir = tmp_path / "truth"
    truth_dir.mkdir(parents=True)
    (truth_dir / "C01_demo.json").write_text(
        json.dumps(
            {
                "candidateName": "林泽远",
                "email": "linzeyuan@example.com",
                "phone": "13800001231",
                "summary": "熟悉 Kotlin、Spring Boot、PostgreSQL 与 Redis。",
                "skills": ["Kotlin", "Spring Boot", "PostgreSQL", "Redis"],
                "workExperiences": [
                    {
                        "company": "云栈软件科技有限公司",
                        "title": "后端开发工程师",
                        "startDate": "2023.07",
                        "endDate": "2025.03",
                    },
                ],
                "educationExperiences": [
                    {
                        "school": "虚构东海大学",
                        "degree": "本科",
                        "fieldOfStudy": "软件工程",
                        "startDate": "2018.09",
                        "endDate": "2022.06",
                    },
                ],
            },
            ensure_ascii=False,
        ),
        encoding="utf-8",
    )

    settings = build_settings()
    settings.synthetic_dataset_dir = str(tmp_path)
    parser = MockResumeParser(settings=settings)
    payload = json.loads(build_payload())
    payload["rawContentReference"] = "browser-pdf-preview://demo/C01_resume_sample.pdf"
    payload["browserPreprocessedPayload"]["sourceFileName"] = "C01_resume_sample.pdf"
    message = ResumeParseMessage.model_validate(payload)

    profile = await parser.parse(message)

    assert profile.basic_info.full_name == "林泽远"
    assert profile.basic_info.email == "linzeyuan@example.com"
    assert {skill.name for skill in profile.skills} >= {"Kotlin", "Spring Boot", "PostgreSQL"}
    assert profile.basic_info.summary is not None
    assert "浏览器端" not in profile.basic_info.summary
    assert "Browser-side PDF preprocessing rendered" in profile.basic_info.summary


def test_litellm_parser_request_includes_external_content_context() -> None:
    from app.schemas.resume import ResumeParseMessage

    parser = LiteLLMResumeParser(build_settings())
    message = ResumeParseMessage.model_validate_json(build_payload(include_external_refs=True))

    request = parser.build_request(
        message,
        external_context=[
            ExternalContentContext(
                source_type="github",
                url="https://github.com/jane-doe/project",
                text="README mentions FastAPI, Docker, and async queue handling",
            ),
        ],
        external_warnings=["portfolio https://jane.dev/portfolio unavailable: timeout"],
    )
    user_content = request["messages"][1]["content"]

    assert isinstance(user_content, list)
    assert "External web context extracted successfully" in user_content[0]["text"]
    assert "External fetch warnings" in user_content[0]["text"]
    assert any(
                item["type"] == "text" and "External github repository context extracted" in item["text"]
        for item in user_content
    )


@pytest.mark.anyio
async def test_remote_content_extractor_builds_structured_github_fields() -> None:
        settings = build_settings()
        extractor = RemoteContentExtractor(settings)

        html = """
        <html>
            <head>
                <title>octocat / hello-world · GitHub</title>
                <meta name="description" content="Example repository for resume parsing demo" />
            </head>
            <body>
                <div>README This repository shows FastAPI and Docker integration for resume parsing workflows.</div>
                <div>Languages Python TypeScript HTML</div>
                <div>Updated 2 days ago</div>
                <div>Stars 42</div>
            </body>
        </html>
        """

        async def handler(request: httpx.Request) -> httpx.Response:
                return httpx.Response(200, headers={"content-type": "text/html"}, text=html)

        transport = httpx.MockTransport(handler)
        original_client = httpx.AsyncClient

        def build_mock_client(*args, **kwargs):
                kwargs["transport"] = transport
                return original_client(*args, **kwargs)

        monkeypatch = pytest.MonkeyPatch()
        monkeypatch.setattr(httpx, "AsyncClient", build_mock_client)
        try:
                extracted = await extractor.extract_content("github", "https://github.com/octocat/hello-world")
        finally:
                monkeypatch.undo()

        assert extracted.github_owner == "octocat"
        assert extracted.github_repo == "hello-world"
        assert extracted.description == "Example repository for resume parsing demo"
        assert extracted.github_readme_excerpt is not None
        assert "FastAPI" in extracted.github_readme_excerpt
        assert extracted.github_languages == ["Python", "TypeScript", "HTML"]
        assert extracted.github_recent_public_signals == ["Updated 2 days ago", "Stars 42"]