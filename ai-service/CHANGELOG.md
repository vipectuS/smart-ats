# AI-Service Changelog

## 2026-04-03

### Milestone 5 - Schema Alias Cleanup & Warning-Free Tests
- Added shared camel-case schema helpers in [ai-service/app/schemas/common.py](ai-service/app/schemas/common.py) and refactored [ai-service/app/schemas/job_fit.py](ai-service/app/schemas/job_fit.py) plus [ai-service/app/schemas/resume.py](ai-service/app/schemas/resume.py) to use `ConfigDict(populate_by_name=True, alias_generator=to_camel)` instead of scattering per-field alias declarations across the model layer.
- Updated [ai-service/app/routers/job_fit_report.py](ai-service/app/routers/job_fit_report.py) to validate the request body explicitly through `JobFitReportRequest.model_validate(...)`, which avoids the FastAPI request-body alias reconstruction path that was emitting Pydantic `UnsupportedFieldAttributeWarning` noise under Python 3.13.
- Tightened [ai-service/pytest.ini](ai-service/pytest.ini) to filter the remaining LiteLLM third-party class-config deprecation warning, so local test output stays focused on project-owned regressions instead of vendor noise.
- Re-ran [ai-service/tests/test_job_fit_report.py](ai-service/tests/test_job_fit_report.py) and the full AI-service suite; verified with `.venv/bin/pytest -q` that all 10 tests pass without warning clutter.

#### Next
- Revisit request-body OpenAPI examples for the job-fit route if frontend needs richer generated docs now that request validation is performed explicitly inside the endpoint.
- Track upstream LiteLLM releases and remove the temporary pytest warning filter once the library migrates its remaining class-based Pydantic config.
- Consider moving other request-heavy routes to the same explicit validation pattern only if they hit the same FastAPI/Pydantic alias edge case; do not generalize it prematurely.

### Milestone 4 - Structured Job Fit Report API
- Added structured job-fit request/response schemas in [ai-service/app/schemas/job_fit.py](ai-service/app/schemas/job_fit.py) and exposed `POST /api/job-fit-report` from [ai-service/app/routers/job_fit_report.py](ai-service/app/routers/job_fit_report.py), returning strict JSON sections such as `headline`, `fitBand`, `strengths`, `risks`, `improvementSuggestions`, `nextSteps`, and `narrative`.
- Added [ai-service/app/services/job_fit_report.py](ai-service/app/services/job_fit_report.py) to orchestrate LiteLLM structured completion calls using JSON Schema output, with audience-aware prompting for candidate-facing vs HR-facing fit explanations.
- Extended [ai-service/app/config.py](ai-service/app/config.py), [ai-service/app/main.py](ai-service/app/main.py), and [ai-service/GUIDE.md](ai-service/GUIDE.md) with `JOB_FIT_REPORT_MODEL` configuration and runtime registration for the new report route.
- Added route and service coverage in [ai-service/tests/test_job_fit_report.py](ai-service/tests/test_job_fit_report.py), including dependency-overridden endpoint validation and mocked LiteLLM JSON parsing behavior. Verified with `pytest` that all 10 AI-service tests pass.

#### Next
- Reduce the remaining Pydantic alias warnings in the job-fit schema by moving to `Annotated[...]` field metadata or another warning-free alias strategy before the schema layer grows further.
- Add retry / fallback policy around job-fit report generation so transient upstream LLM failures can be surfaced with clearer operational diagnostics.
- Consider adding multilingual output controls to the job-fit report route if frontend will need both Chinese and English candidate guidance.

## 2026-04-03

### Milestone 3 - Embedding API & External Content Groundwork
- Added embedding request/response contracts in [ai-service/app/schemas/embedding.py](ai-service/app/schemas/embedding.py) and exposed `POST /api/embeddings` from [ai-service/app/routers/embeddings.py](ai-service/app/routers/embeddings.py), returning strict `{ embedding, dimensions }` JSON for backend semantic matching.
- Introduced [ai-service/app/services/embedding.py](ai-service/app/services/embedding.py) to call a real LiteLLM embedding model, enforce configured vector dimensions, and tolerate both object-style and dict-style provider responses so tests and production adapters behave consistently.
- Extended [ai-service/app/config.py](ai-service/app/config.py), [ai-service/app/main.py](ai-service/app/main.py), and [ai-service/GUIDE.md](ai-service/GUIDE.md) with embedding model settings, dimension settings, external content timeout configuration, and the new default service port `8000` expected by the backend.
- Added groundwork for remote text ingestion in [ai-service/app/services/content_extractor.py](ai-service/app/services/content_extractor.py), including HTML plain-text extraction and a reserved DOCX path for the next document-fetching slice.
- Added coverage in [ai-service/tests/test_embeddings.py](ai-service/tests/test_embeddings.py) for both the HTTP route contract and the LiteLLM service adapter behavior, and verified with `pytest` that all 8 AI-service tests pass.

#### Next
- Implement full external document fetching for trusted HTML and DOCX sources, then reuse the extracted text in both embedding generation and resume parsing.
- Introduce provider fallback and retry policy around the embedding adapter so transient upstream failures can be surfaced to the backend with clearer error classes.
- Share a versioned embedding/parsing contract with the backend once external document ingestion is wired, to avoid drift in request and response expectations.

## 2026-03-24

### Milestone 2 - 引擎跑通阶段
- Added schema-first Pydantic models in [ai-service/app/schemas/resume.py](ai-service/app/schemas/resume.py) for queue payloads, talent profiles, radar scores, callback success payloads, and failure payloads.
- Added parser services in [ai-service/app/services/parser.py](ai-service/app/services/parser.py), including a `MockResumeParser` for local engine bring-up and a `LiteLLMResumeParser` contract using JSON Schema formatted output.
- Added backend callback client in [ai-service/app/services/callbacks.py](ai-service/app/services/callbacks.py) to POST validated results to the Kotlin internal callback API with `X-Internal-Api-Key`.
- Reworked [ai-service/app/consumer.py](ai-service/app/consumer.py) to consume backend Redis pub/sub messages, retain legacy list fallback, parse resumes, submit parsed results, and attempt failure callbacks when parsing or upload fails.
- Extended [ai-service/app/config.py](ai-service/app/config.py) and [ai-service/GUIDE.md](ai-service/GUIDE.md) with backend callback, parser-provider, and failure-callback environment settings.
- Added async coverage in [ai-service/tests/test_consumer.py](ai-service/tests/test_consumer.py) for callback headers/body, successful processing, parser failure handling, upload failure handling, and LiteLLM request contract generation.

#### Next
- Add the backend `PARSE_FAILED` callback endpoint and wire `BACKEND_PARSE_FAILED_PATH` so AI-side failure reporting can change resume status remotely instead of only attempting the call conditionally.
- Replace mock profile generation with real resume content loading plus LiteLLM multimodal extraction once model credentials and raw content retrieval are available.
- Lock a shared versioned JSON schema between backend, frontend, and AI service before the matching engine consumes parsed profiles.

## 2026-03-19

### Milestone 1 - Service Bootstrap
- Initialized the FastAPI-based AI microservice scaffold under [ai-service/app](ai-service/app).
- Added async Redis list listener bootstrap for `resume_parsing_queue` with runtime logging on received messages.
- Added `/health` endpoint and a pytest smoke test.
- Verified locally by starting the service and pushing a Redis message to confirm the consumer logs receipt.

#### Next
- Align the Redis contract with backend Milestone 3, which currently publishes to `resume.parse.requests` via pub/sub.
- Introduce a strict queue payload schema and a LiteLLM adapter abstraction before real model calls are added.
- Add PostgreSQL persistence and/or backend callback contract once message semantics are finalized.