# Smart ATS

Smart ATS 是一个基于前后端分离与 AI 微服务的智能招聘辅助系统，目标是完成多模态简历解析、可解释画像构建、双向岗位匹配与招聘数据看板。

## 仓库结构

```text
.
├── AGENTS.md
├── GUIDE.md
├── PROJECT_PHASE_PLAN.md      # 接手后的阶段计划与推进路线
├── README.md
├── doc/                       # 任务书、开题报告、实验与素材文档
├── tools/maintenance/         # 维护工具预留目录
├── frontend/                  # Vue 3 + Vite 前端
├── backend/                   # Kotlin + Spring Boot 后端
└── ai-service/                # Python + FastAPI AI 微服务
```

说明：
- 根目录不再作为独立 Node 项目使用。
- 前端、后端、AI 服务各自维护自己的依赖与启动方式。
- `tools/maintenance/` 仅保留给未来可复用的维护工具，历史一次性补丁脚本已清理。

## 技术栈

- Frontend: Vue 3, TypeScript, Vite, ECharts
- Backend: Kotlin, Spring Boot 3, PostgreSQL, pgvector, Redis
- AI Service: FastAPI, Pydantic, LiteLLM, HTTPX

## 启动方式

### 前置依赖
- Java 21+
- Maven 3.9+
- Node.js 20+
- Python 3.10+
- PostgreSQL 15+ with `pgvector`
- Redis

### 数据库准备

```bash
sudo systemctl start redis-server
sudo -u postgres psql -c "CREATE DATABASE smart_ats;"
sudo -u postgres psql -d smart_ats -c "CREATE EXTENSION vector;"
```

### 启动后端

```bash
cd backend
mvn spring-boot:run
```

### 启动 AI 服务

```bash
cd ai-service
python3 -m venv .venv
. .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

### 启动前端

```bash
cd frontend
npm install
npm run dev
```

## 当前建议阅读顺序

1. `PROJECT_PHASE_PLAN.md`
2. `GUIDE.md`
3. `backend/GUIDE.md`
4. `frontend/GUIDE.md`
5. `ai-service/GUIDE.md`

## 开发约束

- 先看接口契约，再写联调代码。
- 优先保证主链路可跑通，不要盲目堆功能。
- 每完成一个阶段，要同步更新 CHANGELOG 和相关文档。
