# AI-Service Changelog

## 2026-04-16

### Demo Bootstrap Compatibility
- Updated [ai-service/.env.example](ai-service/.env.example) so local bootstrap paths now include `BACKEND_PARSE_FAILED_PATH=/internal/api/resumes/{resumeId}/parse-failed`, keeping the AI-service failure callback contract aligned with the backend internal API during unified demo startup.
- Verified the current local bootstrap path against the project virtual environment and the maintenance scripts, so AI-service can participate in `tools/maintenance/bootstrap_local_demo.sh --with-demo-data` instead of remaining a separately managed manual step.

#### Next
- If local demo scripts later need stronger environment isolation, move the current bootstrap-specific callback and parser defaults into a dedicated demo profile rather than relying only on the shared `.env` template.

## 2026-04-05

### Synthetic Dataset-Aware Mock Parsing
- Updated [ai-service/app/config.py](ai-service/app/config.py), [ai-service/app/services/parser.py](ai-service/app/services/parser.py), [ai-service/.env.example](ai-service/.env.example), and [ai-service/GUIDE.md](ai-service/GUIDE.md) so local `mock` parser mode can now detect synthetic sample ids such as `C01` from backend references or browser-side file names, load the matching truth JSON from `SYNTHETIC_DATASET_DIR`, and emit a stronger deterministic TalentProfile for demo and evaluation runs.
- Extended [ai-service/tests/test_consumer.py](ai-service/tests/test_consumer.py) with regression coverage for the synthetic truth-loading path, while keeping browser-side summary and external web context merged into the final profile summary.

#### Next
- Decide whether the synthetic truth-aware path should stay limited to local/demo mode only, or become a versioned fallback parser for offline benchmark generation as well.
- If later demo packs expand beyond the current `Cxx` naming convention, formalize the synthetic sample lookup contract instead of relying only on file-name pattern matching.

### Local Embedding Fallback
- Updated [ai-service/app/services/embedding.py](ai-service/app/services/embedding.py) so `POST /api/embeddings` now falls back to a deterministic local hashed vector when the upstream embedding provider is unavailable, preventing local recommendation and demo flows from failing closed with HTTP 500.
- Extended [ai-service/tests/test_embeddings.py](ai-service/tests/test_embeddings.py) with regression coverage for the fallback path.

#### Next
- Keep using the local fallback only as a development safety net; when a real provider is configured, verify dimensions and retrieval quality against the production model before Phase 4 score tuning.

### Structured GitHub Repository Context
- Extended [ai-service/app/services/content_extractor.py](ai-service/app/services/content_extractor.py) so GitHub links are no longer treated as undifferentiated HTML pages: the extractor now derives a controlled minimal field set including repository owner/name, description, README excerpt, detected languages, and recent public signals such as stars or update hints.
- Updated [ai-service/app/services/parser.py](ai-service/app/services/parser.py) so GitHub references are injected into the resume parsing prompt as structured repository context instead of plain page dumps, while portfolio links still fall back to generic HTML text extraction.
- Extended [ai-service/tests/test_consumer.py](ai-service/tests/test_consumer.py) with coverage for structured GitHub extraction and prompt assembly. Re-ran `tests/test_consumer.py` successfully with 9 passing tests.

#### Next
- Decide whether structured GitHub fields should also feed the embedding pipeline directly so Phase 4 matching can use the same multimodal evidence instead of parsing-only context.
- Keep the GitHub scope intentionally narrow; if richer repository metadata is needed later, add it behind a versioned contract rather than reintroducing uncontrolled full-page scraping.

### External HTML Context in Resume Parsing
- Extended [ai-service/app/schemas/resume.py](ai-service/app/schemas/resume.py) with optional `externalContentReferences`, and updated [ai-service/app/services/parser.py](ai-service/app/services/parser.py) so both mock and LiteLLM parsers now fetch best-effort HTML text from candidate GitHub / portfolio links and merge that text into the same parsing context as browser-side PDF previews.
- Kept the current implementation intentionally narrow: it caps external sources to two links, truncates fetched text, records fetch warnings instead of failing the whole parse, and leaves GitHub-specific structured field extraction for the next Phase 3 slice.
- Extended [ai-service/tests/test_consumer.py](ai-service/tests/test_consumer.py) to verify external context-aware mock summaries and LiteLLM request construction. Re-ran `tests/test_consumer.py` successfully with 8 passing tests.

#### Next
- Replace the current GitHub-as-generic-HTML strategy with a controlled minimal field extractor for README, description, and primary language so downstream prompts stay stable.
- Decide whether external text should also feed the embedding pipeline directly, or remain parsing-only until Phase 4 score-breakdown work demands shared multimodal evidence.

### Browser Payload-Aware Resume Parsing
- Extended [ai-service/app/schemas/resume.py](ai-service/app/schemas/resume.py) so queue payloads can optionally carry browser-preprocessed PDF metadata, including paged preview images, text snippets, and render warnings from the frontend upload flow.
- Updated [ai-service/app/services/parser.py](ai-service/app/services/parser.py) so the parser now prefers browser-side PDF context when available: mock parsing incorporates browser text summaries, and the LiteLLM request builder emits multi-modal user content with preview text plus page image URLs.
- Fixed the current multimodal budget policy in [ai-service/app/services/parser.py](ai-service/app/services/parser.py) so parser context handling is aligned with the frontend upload cap of 3 preview pages, and the prompt text now records how many preview pages were actually attached.
- Extended [ai-service/tests/test_consumer.py](ai-service/tests/test_consumer.py) to cover the richer queue payload and the multimodal request-construction path. Verified with `.venv/bin/python3 -m pytest tests/test_consumer.py -q` that all 6 targeted tests pass.

#### Next
- Decide later whether the default cap should remain 3 preview pages or become model-specific once larger samples and token budgets are measured.
- When real external resume retrieval is added, keep browser-preprocessed payloads as the preferred source and use `rawContentReference` only as fallback context.

## 2026-04-04

### Config Hygiene - Local Env Split
- Replaced tracked fallback secrets in [ai-service/app/config.py](ai-service/app/config.py) with placeholder-safe defaults for PostgreSQL password and internal callback key.
- Added tracked template [ai-service/.env.example](ai-service/.env.example) and local ignored [ai-service/.env](ai-service/.env) so current machine-specific runtime values can stay available for local testing without leaking into the public repo.
- Updated [ai-service/GUIDE.md](ai-service/GUIDE.md) to document the `.env.example` to `.env` workflow and remove real-looking local credential examples from public docs.

#### Next
- Decide whether the AI service should hard-fail startup when placeholder secrets are still present outside local development, instead of silently accepting them.
- Align callback-key loading rules with backend deployment profiles so cross-service local/dev/prod behavior stays explicit.

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