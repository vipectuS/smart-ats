# Agent Directives & Project Policies (智能招聘辅助系统)

This document serves as the top-level policy and system instruction for all AI Agents contributing to this project. 
All agents MUST read and strictly adhere to the project's global `GUIDE.md` and their respective sub-module `#GUIDE.md` before execution.

## 1. Project Overview & Boundaries
This project is a B/S architecture Smart ATS (Applicant Tracking System).
**Core Strategy**: Make it work cleanly first. Maximize the use of Multi-modal LLMs for complex resume unstructured data extraction, and offload CPU-intensive pre-processing to the Frontend using WebAssembly (Wasm).

## 2. Agent Assignments
We have divided the project into three distinct modules. Whenever invoked, you will be assigned to ONE of these domains. Stay within your designated boundary.

### 🎭 Agent Frontend (UI & Wasm)
- **Role**: Vue 3 / Vite Engineer + Wasm Integrator
- **Scope**: `frontend/` directory.
- **Rules**:
  - Focus on ECharts interactive radar charts and user dashboards.
  - Implement WebAssembly (Wasm) architecture for browser-side PDF-to-Image rendering. Do NOT push raw heavy PDFs to the backend.
  - Maintain your own `frontend/GUIDE.md`.

### 🧮 Agent Backend (Business & Core Logic)
- **Role**: Kotlin & Spring Boot 3 Architect.
- **Scope**: `backend/` directory.
- **Rules**:
  - Use PostgreSQL + `pgvector` for CV vector storage and semantic matching.
  - Handle Auth, CRUD operations, and interface with the AI Microservice.
  - Offload demanding tasks like AI calls via a Redis Message Queue to prevent HTTP timeouts.
  - Maintain your own `backend/GUIDE.md`.

### 🧠 Agent AI-Service (LLM Integration)
- **Role**: Python / FastAPI / LLM Orchestrator.
- **Scope**: `ai-service/` directory.
- **Rules**:
  - Implement a highly abstracted LLM Adapter (Factory pattern) to allow seamless switching between Vision Large Language Models (like Zhipu GLM-4V, Qwen-VL).
  - Return STRICT JSON structures matching the predefined talent profile models.
  - Generate the XAI (Explainable AI) assessment textual reports.
  - Maintain your own `ai-service/GUIDE.md`.

## 3. General Directives for All Agents
- **No bold assumptions**: If an integration contract (API schema) is ambiguous, ask the human or check the parallel module's files.
- **Step-by-step & Milestones**: Work strictly according to the defined project milestones. Do not jump ahead to avoid hallucinations and context bloat.
- **Test-As-You-Go**: You MUST write comprehensive automated tests (Unit/Integration) for each module immediately after implementing it. Do not wait until the end of the project.
- **CHANGELOG Maintenance**: Every time you complete a significant task or milestone, you MUST log the progress in `CHANGELOG.md` inside your respective module directory.
- Before committing any code, cross-reference the architecture constraints in `/GUIDE.md`.
## 4. Anti-Hallucination & API Contract Rules (Crucial)
To avoid integration bugs like 401 Unauthorized or mismatched payload fields, the following rules are MANDATORY:
1. **API Contract-First**: Before writing any frontend API calling logic (like Axios requests), HR agents MUST explicitly read the Kotlin `*Controller.kt` endpoints and `*DTO.kt` payloads in the backend. DO NOT GUESS the endpoints, request bodies, or response structures.
2. **Data Seeding First**: If a frontend UI relies on mock data or default users (e.g., `admin`), the backend agent MUST implement a `CommandLineRunner` (like `DataSeeder.kt`) to ensure this data actually exists in the database. Never rely on empty databases for initial UI testing.
3. **No Assumptions on Paths**: Check the `@RequestMapping` carefully. e.g., `/api/v1/auth` vs `/api/auth`.
