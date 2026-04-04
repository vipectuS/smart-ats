# Development Guide

This is the central development guide for the **Smart ATS (Multi-modal Resume Parsing System)**.

## 1. Architecture Map
The architecture is designed to offload data preprocessing to the edge, separate core logic from heavy AI computation, and utilize vector databases for efficient candidate matching.

```
graph TD;
    Browser[Frontend: Vue 3 + Vite] -->|1. Wasm PDF rendering & Redaction| Browser;
    Browser -->|2. Upload Image & Text Form| Backend[Kotlin + Spring Boot 3];
    Backend -->|CRUD & Auth| Postgres[(PostgreSQL + pgvector)];
    Backend -->|3. Redis Async Queue| Redis((Redis Message Broker));
    Redis -->|4. Pull Task| AIService[Python + FastAPI];
    AIService -->|5. VLM API Call| LLM((Third-party LLMs: GLM-4V/Qwen-VL));
    AIService -->|6. Return JSON Profile & XAI text| Backend;
    Backend -->|7. Match & Score| Postgres;
```

## 2.Global Tech Stack
- Frontend: Vue 3, TypeScript, Vite, ECharts, PDF.js/Wasm.
- Backend Core: Kotlin 1.9+, Java 17+, Spring Boot 3, Spring Data JPA/JDBC.
- Database: PostgreSQL 15+ with pgvector extension.
- Message Broker: Redis.
- AI Microservice: Python 3.10+, FastAPI, LiteLLM/LangChain, PyMuPDF (backup), HTTPX.

## 3. Project Structure
The repository MUST strictly follow this structure:
```
.
├── AGENTS.md            # Global Agent Policies
├── GUIDE.md             # This document
├── doc/                 # Global documentation
│   └── (Your documents)
├── frontend/            # Web UI & Wasm
│   ├── GUIDE.md         # Component mapping, Prettier styles, Wasm bindings doc.
│   └── src/
├── backend/             # Kotlin Spring Boot App
│   ├── GUIDE.md         # DB schema, Kotlin code style, Queue configs.
│   └── src/
└── ai-service/          # Python Microservice
    ├── GUIDE.md         # Python linting, LLM Prompts structure, Schema validation.
    └── app/
```

## 4. Sub-module Guides (*/GUIDE.md)
Each module directory contains its own GUIDE.md.
Whenever an Agent modifies or builds a service, they must first generate or update their local GUIDE.md.
A sub-module guide must minimally include:
1. Directory Outline: What lives where.
2. Build & Test Instructions: e.g., mvn spring-boot:run or uvicorn app.main:app --reload.
3. Internal Code Style: e.g., ktlint for Kotlin, PEP8/Black for Python.
4. Environment Variables Template: Key dependencies like Database URI or LLM API keys.

## 5. Global Policy
- API Contracts Matrix: Wait for Frontend/Backend/AI to agree upon unified JSON schemas before blind coding.
- Make it Work First: Prioritize the golden pipeline (Single Graphic Resume Wasm parsing -> Upload -> AI JSON Extract -> DB Store).