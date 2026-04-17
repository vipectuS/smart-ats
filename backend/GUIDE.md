# Backend Guide & Infrastructure Setup (Kotlin + Spring Boot 3 + PostgreSQL/Redis)

This document serves as the internal guide and infrastructure checklist for the **Backend** module of the Smart ATS project. **All backend agents MUST read this document** to understand the infrastructure context (IPs, Ports, Credentials) before writing connection logic or database scripts.

## 1. Infrastructure Readiness Checklist
*Human developer: Please fill in the actual values below based on your local environment infrastructure. Backend agents will read these sections to configure `application.yml`.*

### PostgreSQL Database
- **Host (IP)**: `127.0.0.1`
- **Port**: `5432`
- **Database Name**: `smart_ats`
- **Username**: `postgres`
- **Password**: `(set in backend/local/backend-local.yml)`
- **Extension Check**: Does the database have `pgvector` installed? `Yes (installed in smart_ats)`

### Redis Message Broker
- **Host (IP)**: `127.0.0.1`
- **Port**: `6379`
- **Password (if any)**: `(none)`
- **Database Index**: `0`

---

## 2. Directory Outline
```text
backend/
├── GUIDE.md                 # This document (Infrastructure & Configs)
├── pom.xml                  # Maven build script
├── CHANGELOG.md             # Milestone delivery record
└── src/
    └── main/
        ├── kotlin/
      │   └── com/smartats/backend/
      │       ├── config/           # Security and typed property configurations
      │       ├── controller/       # Auth and user REST endpoints
      │       ├── domain/           # JPA entities and enums
      │       ├── dto/              # API response/request DTOs
      │       ├── exception/        # Global exception handlers
      │       ├── repository/       # Spring Data JPA interfaces
      │       ├── security/         # JWT filter, token service, user details loader
      │       └── service/          # Auth/user business services
        └── resources/
            ├── application.yml       # Using parameters from the checklist above
            └── db/migration/         # Flyway or Liquibase scripts (to create tables below)
   └── test/
      ├── kotlin/                   # JUnit 5 + MockMvc integration tests
      └── resources/                # application-test.yml
```

## 3. Current Database Domain Overview
*Agents: treat this as the current contract summary before reading the concrete entity, DTO, and controller source.*

1. **`users`**
   - `id` (UUID, Primary Key)
   - `role` (HR, CANDIDATE, ADMIN)
   - `username`, `password_hash`, `email`
   - `created_at`, `updated_at`

2. **`jobs`** (HR posts)
   - `id` (UUID, Primary Key)
   - `title`, `description` (TEXT)
   - `requirements` (JSONB - explicit tags/skills)
   - `created_by` (FK -> users)

3. **`resumes`** (Candidate-owned parsed assets)
   - `id` (UUID, Primary Key)
   - `user_id` (FK -> users)
   - `candidate_name`, `contact_info`
   - `raw_content_reference` (String - path to S3/local file)
   - `browser_preprocessed_payload` (JSONB - optional browser-side page previews, text snippets, and render metadata)
   - `parsed_data` (JSONB - strict output from AI Microservice)
   - `embedding` (VECTOR - for semantic matching)
   - `status` (PENDING_PARSE, PARSING, PARSED, PARSE_FAILED)

4. **`candidate_profiles`**
   - `user_id` (FK -> users)
   - `github_url`, `portfolio_url`
   - latest resume/profile projection for candidate-facing APIs

5. **`job_recommendations`**
   - `job_id` (FK -> jobs)
   - `resume_id` (FK -> resumes)
   - `match_score`, `semantic_score`
   - `xai_reasoning`, `xai_report`

6. **`job_applications` / `job_favorites` / `job_ignores`**
   - Candidate-side action state and audit timeline tables
   - Used by candidate recommendation actions and `/api/candidates/me/timeline`

## 4. Build & Test Instructions
- **Build tool**: Maven (already installed locally).
- **Run local server**: `mvn spring-boot:run`
- **Run tests**: `mvn test`
- **Testing Policy**: Test-As-You-Go. Every controller and service must have corresponding JUnit 5 + MockMvc/Mockito tests implemented in the same module sprint.

## 5. Milestone 1 Baseline
- **Auth endpoints**:
   - `POST /api/v1/auth/register`
   - `POST /api/v1/auth/login`
   - `GET /api/v1/users/me`
   - Compatibility aliases: `POST /api/auth/register`, `POST /api/auth/login`
- **Security model**: Stateless JWT Bearer token, BCrypt password hashing, role-based authorities (`ROLE_HR`, `ROLE_CANDIDATE`, `ROLE_ADMIN`). Public registration rejects `ADMIN`.
- **Database baseline**: `users` table managed by Flyway `V1__create_users_table.sql`.
- **Test profile**: H2 in PostgreSQL compatibility mode via `src/test/resources/application-test.yml`.
- **Response envelope**: `{"status": 200, "data": {...}, "message": "Success"}`.

## 6. Development Policies & Formatting
- **Code Style**: Strictly follow `ktlint` standard for Kotlin.
- **REST Guidelines**: Use proper HTTP verbs, return standard JSON responses `{"status": 200, "data": {...}, "message": "Success"}`.
- **Queue/Async**: Do not block the main HTTP thread for AI parsing. Send a job ID to Redis, store the status as `PENDING_PARSE`, and let the frontend poll or use SSE/WebSocket for updates.
- **Resume Upload Contract**: Candidate uploads may now include optional browser-preprocessed PDF page batches; preserve this payload for AI consumption while keeping `raw_content_reference` as the lightweight fallback reference.
- **External Context Contract**: When a candidate profile contains `github_url` or `portfolio_url`, the backend parse queue payload should forward them as lightweight `externalContentReferences` so the AI service can fetch HTML text without forcing the frontend to upload those pages manually.
- **Maintain Changelog**: Agents must document all completed features in `backend/CHANGELOG.md` upon completion of a phase or significant function.

## 7. Environment Variables Template
- `DB_HOST=127.0.0.1`
- `DB_PORT=5432`
- `DB_NAME=smart_ats`
- `DB_USERNAME=postgres`
- `DB_PASSWORD=change-me-db-password`
- `REDIS_HOST=127.0.0.1`
- `REDIS_PORT=6379`
- `REDIS_PASSWORD=`
- `REDIS_DATABASE=0`
- `JWT_SECRET=change-me-jwt-secret-at-least-32-bytes`
- `JWT_EXPIRATION_MINUTES=120`
- `SERVER_PORT=8080`

## 7.1 Local Override File
- Tracked template: `backend/local/backend-local.example.yml`
- Local ignored override: `backend/local/backend-local.yml`
- Copy the example file into the local override and fill your own machine-specific values there instead of committing real local credentials into `application.yml`.

## 8. Backend Milestones
To prevent hallucinations and complex rollbacks, follow these phases strictly:

- **Milestone 1: Foundation & Security**: Initialize Spring Boot with Maven. Set up application.yml for Postgres. Implement Global Exception Handling, User Entity, and basic JWT Authentication. (Deliverable: Auth APIs with JUnit/MockMvc tests).
- **Milestone 2: Core Domain (Jobs & Database Mapping)**: Define JPA Entities for Jobs and Resumes. Map `JSONB` and `VECTOR` types explicitly for PostgreSQL. Create REST controllers for basic CRUD. (Deliverable: Job management APIs with integration tests).
- **Milestone 3: Async Queue & External Services**: Integrate Spring Data Redis. Create Producer/Consumer logic for the "Resume Parsing" async tasks. Design the RestTemplate/WebClient caller for the Python AI Microservice. (Deliverable: Async task flow with Mockito tests).
- **Milestone 4: XAI Matching Engine**: Implement the logic to extract and calculate match scores using `pgvector` similarity queries and format the XAI reasoning response. (Deliverable: Recommendation API with unit tests covering matching logic).

## 9. Milestone 4 API Draft

### Frontend Polling API
- `GET /api/resumes/{resumeId}/status`
- Purpose: lightweight polling endpoint for progress tags and progress bars.
- Auth: same JWT Bearer auth as other HR endpoints.
- Response:

```json
{
   "status": 200,
   "data": {
      "resumeId": "7db0c4a1-0e7b-4c61-9e79-cc5dd35fae1c",
      "status": "PARSING",
      "parsedDataAvailable": false,
      "parseFailureReason": null,
      "updatedAt": "2026-03-24T16:00:00"
   },
   "message": "Success"
}
```

### Resume Detail API
- `GET /api/resumes/{resumeId}`
- Purpose: full resume detail view.
- Note: this response already contains the current `status`, full `parsedData` once available, and `parseFailureReason` for failure display.

### AI-Service Callback API
- `POST /internal/api/resumes/{resumeId}/parsed-result`
- Purpose: protected callback endpoint for Python AI service to persist final structured profile JSON into PostgreSQL.
- Protection: `X-Internal-Api-Key` header must match backend internal callback configuration.
- Request body:

```json
{
   "parsedData": {
      "candidateProfile": {
         "name": "Jane Doe",
         "skills": ["Kotlin", "Spring Boot"]
      },
      "xaiSummary": "Structured profile generated by AI service"
   }
}
```

- Success response:

```json
{
   "status": 200,
   "data": {
      "resumeId": "7db0c4a1-0e7b-4c61-9e79-cc5dd35fae1c",
      "status": "PARSED",
      "parsedDataAvailable": true,
      "parseFailureReason": null,
      "updatedAt": "2026-03-24T16:00:01"
   },
   "message": "Parsed result accepted via X-Internal-Api-Key"
}
```

### AI-Service Failure Callback API
- `POST /internal/api/resumes/{resumeId}/parse-failed`
- Purpose: protected callback endpoint for Python AI service to report parse failure and move resume state to `PARSE_FAILED`.
- Protection: `X-Internal-Api-Key` header must match backend internal callback configuration.
- Request body:

```json
{
   "reason": "Vision model failed to parse uploaded image"
}
```

- Success response:

```json
{
   "status": 200,
   "data": {
      "resumeId": "7db0c4a1-0e7b-4c61-9e79-cc5dd35fae1c",
      "status": "PARSE_FAILED",
      "parsedDataAvailable": false,
      "parseFailureReason": "Vision model failed to parse uploaded image",
      "updatedAt": "2026-03-24T18:00:01"
   },
   "message": "Parse failure accepted via X-Internal-Api-Key"
}
```

### Error Contract
- `400 Bad Request`: malformed JSON or missing/empty `parsedData`.
- `401 Unauthorized`: invalid or missing internal callback API key.
- `404 Not Found`: `resumeId` does not exist.
