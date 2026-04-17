# AI-Service Guide

This document defines the local development contract for the Python AI microservice.

## 1. Directory Outline
```text
ai-service/
├── GUIDE.md                # This document
├── CHANGELOG.md            # Milestone delivery record
├── requirements.txt        # Python dependencies
├── main.py                 # Root ASGI entrypoint
├── app/
│   ├── __init__.py
│   ├── config.py           # Environment-backed settings
│   ├── consumer.py         # Redis queue listener
│   ├── main.py             # FastAPI app factory and lifespan
│   ├── schemas/            # Pydantic queue, profile, callback schemas
│   ├── services/           # Parser adapters and backend callback client
│   └── routers/
│       ├── __init__.py
│       └── health.py       # Health endpoint
└── tests/
    ├── test_consumer.py    # Queue listener and callback integration tests
    └── test_health.py      # FastAPI smoke test
```

## 2. Build & Test Instructions
- Install dependencies: `python3 -m venv .venv && . .venv/bin/activate && pip install -r requirements.txt`
- Run local server: `uvicorn main:app --reload --host 0.0.0.0 --port 8000`
- Run tests: `pytest`
- Redis queue smoke test: `redis-cli LPUSH resume_parsing_queue '{"resumeId":"test"}'`

## 3. Internal Code Style
- Follow PEP 8.
- Prefer typed functions and dataclasses/Pydantic models for queue payloads.
- Use `logging` instead of print statements for runtime diagnostics.

## 4. Environment Variables Template
- `AI_SERVICE_HOST=0.0.0.0`
- `AI_SERVICE_PORT=8000`
- `REDIS_HOST=127.0.0.1`
- `REDIS_PORT=6379`
- `REDIS_DB=0`
- `REDIS_PASSWORD=`
- `RESUME_QUEUE_NAME=resume_parsing_queue`
- `BACKEND_QUEUE_CHANNEL=resume.parse.requests`
- `BACKEND_BASE_URL=http://127.0.0.1:18080`
- `BACKEND_PARSED_RESULT_PATH=/internal/api/resumes/{resumeId}/parsed-result`
- `BACKEND_PARSE_FAILED_PATH=/internal/api/resumes/{resumeId}/parse-failed`
- `INTERNAL_CALLBACK_HEADER_NAME=X-Internal-Api-Key`
- `INTERNAL_CALLBACK_API_KEY=change-me-internal-callback-key`
- `BACKEND_CALLBACK_TIMEOUT_SECONDS=10`
- `POSTGRES_HOST=127.0.0.1`
- `POSTGRES_PORT=5432`
- `POSTGRES_DB=smart_ats`
- `POSTGRES_USER=postgres`
- `POSTGRES_PASSWORD=change-me-postgres-password`
- `LITELLM_MODEL=gpt-4o-mini`
- `JOB_FIT_REPORT_MODEL=gpt-4o-mini`
- `RESUME_PARSER_PROVIDER=mock`
- `EMBEDDING_MODEL=text-embedding-3-small`
- `EMBEDDING_DIMENSIONS=1536`
- `EXTERNAL_CONTENT_TIMEOUT_SECONDS=10`
- `SYNTHETIC_DATASET_DIR=../doc/synthetic-dataset`

## 4.1 Local Env File
- Tracked template: `ai-service/.env.example`
- Local ignored runtime file: `ai-service/.env`
- Keep current machine-specific passwords and callback keys only in `.env`, never in tracked source files.

## 5. Integration Note
- AI service now subscribes to Redis pub/sub channel `resume.parse.requests` and keeps the old `resume_parsing_queue` list polling as a compatibility fallback.
- Queue payloads may now include optional browser-side PDF preprocessing metadata (`browserPreprocessedPayload`) with page previews, image data URLs, text snippets, and render warnings; parsers should prefer this richer context before falling back to the raw backend reference.
- Queue payloads may also include optional candidate-owned external references (`externalContentReferences`), currently used for GitHub and portfolio URLs. The AI service now fetches these links on a best-effort basis and normalizes GitHub inputs into a minimal field set: `owner/repo`, repository description, README excerpt, detected languages, and recent public signals before merging them with PDF/browser context.
- Local `mock` parser mode is now synthetic-dataset aware: if the uploaded backend reference or browser-side file name contains a sample id such as `C01`, it will try to load the matching truth JSON from `SYNTHETIC_DATASET_DIR/truth/` and emit a stronger deterministic TalentProfile for demo and evaluation runs.
- Successful callbacks POST validated `parsedData` to `/internal/api/resumes/{resumeId}/parsed-result` with the configured `X-Internal-Api-Key`.
- Failed parse callbacks POST to `/internal/api/resumes/{resumeId}/parse-failed` with the configured `X-Internal-Api-Key`.
- `POST /api/embeddings` now exposes a real embedding contract returning `{"embedding": [...], "dimensions": 1536}` and is intended for backend semantic matching calls. When the remote provider is unavailable in local development, the service falls back to a deterministic hashed embedding so local recommendation and screenshot/demo paths can still complete.
- `POST /api/job-fit-report` now exposes a strict JSON XAI contract for candidate-facing or HR-facing job fit summaries, suitable for direct backend/frontend rendering.