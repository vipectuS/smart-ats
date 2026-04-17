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
- `tools/maintenance/` 现在包含可复用的统一启动、演示数据导入和证据导出脚本。

## 技术栈

- Frontend: Vue 3, TypeScript, Vite, ECharts
- Backend: Kotlin, Spring Boot 3, PostgreSQL, pgvector, Redis
- AI Service: FastAPI, Pydantic, LiteLLM, HTTPX

## 启动方式

### 本地配置文件

- 后端：从 `backend/local/backend-local.example.yml` 复制到 `backend/local/backend-local.yml`，填写本机数据库密码、JWT 密钥，以及是否启用本地管理员种子。
- AI 服务：从 `ai-service/.env.example` 复制到 `ai-service/.env`，填写内部回调密钥、数据库密码和模型相关配置。
- 前端：可选从 `frontend/.env.example` 复制到 `frontend/.env.local`，按本机环境覆盖代理目标、直连 API 地址和本地演示账号。
- 仓库中不再提交可直接使用的默认管理员账号密码；如需本地演示账号，请在后端本地覆盖配置中显式开启。

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

### 统一启动与演示数据导入

如果本机已具备 PostgreSQL、Redis 和各模块本地配置，可直接在仓库根目录执行：

```bash
tools/maintenance/bootstrap_local_demo.sh --with-demo-data
```

该入口会启动三服务、导入合成岗位与候选人、触发评估，并输出：

- `tools/maintenance/output/seed_demo_data_summary.json`
- `tools/maintenance/output/phase6_report.json`

停止命令：

```bash
tools/maintenance/stop_local_demo.sh
```

## 当前可验证主路径

1. HR 登录后进入看板，查看岗位列表、岗位详情、推荐结果与简历列表。
2. Candidate 注册或登录后进入推荐页，执行投递、收藏、忽略与时间线查看。
3. 简历上传的最终正式方案必须走浏览器端 Wasm PDF 预处理主链路，当前计划已写入 `PROJECT_PHASE_PLAN.md`，后续实现以该约束为准。
4. 管理员可进入新增的 Admin Console，查看系统概览、技能词典与解析失败记录。

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
