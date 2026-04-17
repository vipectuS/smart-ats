# Changelog

All notable changes to the Agent Frontend (UI & Wasm) microservice will be documented in this file.

## [Unreleased]

### Phase 5 / Phase 7 - 管理台与可复现演示
- Added [frontend/src/views/AdminConsoleView.vue](frontend/src/views/AdminConsoleView.vue), [frontend/src/types/admin.ts](frontend/src/types/admin.ts), and updated [frontend/src/router/index.ts](frontend/src/router/index.ts) so ADMIN users now have a dedicated management surface wired to real backend contracts for system overview, skill dictionary maintenance, and parse-failure inspection.
- Updated [frontend/src/layouts/ATSLayout.vue](frontend/src/layouts/ATSLayout.vue) and [frontend/src/stores/auth.ts](frontend/src/stores/auth.ts) so admin users get dedicated navigation, header actions, and home-route routing instead of being silently treated as generic HR users.
- Re-ran `npm run build` successfully; the production build now contains the Admin Console bundle and remains green after the new route and layout wiring.

### Next
- If the admin surface expands further, extract the current page-level data loading into dedicated composables before adding more charts or maintenance panels.
- When preparing final答辩录屏, capture one admin walkthrough covering overview refresh, skill edit, and parse-failure inspection so the role difference is visible in evidence material.

### Phase 2 / Phase 3 - 状态收口与答辩材料补齐
- Updated [frontend/src/views/LoginView.vue](frontend/src/views/LoginView.vue) to remove the lingering `admin` placeholder and replace the login hint with a neutral env-based test-account note, so the login page no longer carries tracked demo identity residue.
- Updated [frontend/src/views/JobDetailView.vue](frontend/src/views/JobDetailView.vue) to replace the “第一版/后续扩展” reviewer note with a stable workflow description, aligning the HR detail page with the current Phase 2 completed state.
- Added [doc/phase3_wasm_pdf_flow.md](doc/phase3_wasm_pdf_flow.md) to formally document the browser-side PDFium Wasm preprocessing path, upload batching strategy, backend contract, and AI aggregation flow for Phase 3答辩材料.

### Next
- Capture one runtime screenshot set for the Phase 3 multimodal path on a machine that has browser screenshot tooling available, then fold the resulting evidence into the final答辩材料包。

### Phase 2 - HR 主路径补强
- Updated [frontend/src/views/JobsView.vue](frontend/src/views/JobsView.vue) to replace the placeholder岗位发布入口 with a real HR create-job flow, including a slide-over form, field-level validation, requirements preview, tag-based skills input, and inline API error feedback.
- Extended [frontend/src/views/JobsView.vue](frontend/src/views/JobsView.vue) so created jobs immediately refresh the paginated list and route into the job detail page, letting HR continue directly into recommendation generation instead of stopping at the list page.
- Improved [frontend/src/views/JobsView.vue](frontend/src/views/JobsView.vue) list cards to display real description snippets and requirement tags, making the HR job list closer to a demo-ready management surface instead of a minimal data table.
- Added [frontend/src/views/ResumeDetailView.vue](frontend/src/views/ResumeDetailView.vue), updated [frontend/src/router/index.ts](frontend/src/router/index.ts), and connected [frontend/src/views/ResumeListView.vue](frontend/src/views/ResumeListView.vue) so HR users can open a real简历详情页, inspect parsed talent-profile fields, watch parsing status refresh, and re-trigger parsing without停留在列表页里盲操作.
- Updated [frontend/src/views/JobDetailView.vue](frontend/src/views/JobDetailView.vue) to add a real HR edit-job panel backed by the new update contract, with requirement field backfill, skill-tag editing, inline validation, and save feedback that keeps the recruiter in the evaluation context instead of bouncing back to the list page.
- Extended [frontend/src/views/JobDetailView.vue](frontend/src/views/JobDetailView.vue) with an in-page申请审核台 backed by the new岗位投递列表接口, so HR can see active applicants, correlate them with existing AI recommendation scores, and jump directly into resume detail from the same job workspace.
- Upgraded [frontend/src/views/JobDetailView.vue](frontend/src/views/JobDetailView.vue) again so the申请审核台 now supports直接保存约面/淘汰状态与审核备注, turning the page from a read-only applicant list into a real first-pass recruiter workflow surface.
- Updated [frontend/src/views/CandidateApplicationsView.vue](frontend/src/views/CandidateApplicationsView.vue), [frontend/src/views/CandidateDashboardView.vue](frontend/src/views/CandidateDashboardView.vue), [frontend/src/views/CandidateJobDetailView.vue](frontend/src/views/CandidateJobDetailView.vue), [frontend/src/components/CandidateTimeline.vue](frontend/src/components/CandidateTimeline.vue), and [frontend/src/types/CandidateActions.ts](frontend/src/types/CandidateActions.ts) so candidate-facing pages can render `INTERVIEW` / `REJECTED` review states cleanly instead of showing raw English enums or exposing misleading repeat-apply actions.

### Next
- Walk the HR demo path once from dashboard to create job, edit job, trigger evaluation, inspect recommendations, process at least one applicant in the审核台, then jump into resume detail and confirm the full recruiter path is stable.
- Decide whether the next HR-side closing step should be second-stage actions such as interview scheduling/result logging, or a dedicated dashboard/report polish pass for screenshots and答辩录屏.

### Phase 2 - Candidate 旅程收尾
- Added [frontend/src/views/CandidateProfileView.vue](frontend/src/views/CandidateProfileView.vue) to connect the real `GET /api/candidate/profile` and `PUT /api/candidate/profile` contracts, so candidate users can maintain GitHub and portfolio links while seeing their latest resume parsing state in the same page.
- Updated [frontend/src/router/index.ts](frontend/src/router/index.ts), [frontend/src/layouts/ATSLayout.vue](frontend/src/layouts/ATSLayout.vue), and [frontend/src/views/CandidateDashboardView.vue](frontend/src/views/CandidateDashboardView.vue) to expose the Candidate profile route in navigation and to route empty-state recovery toward profile completion instead of leaving the user at a dead end.
- Added [frontend/src/views/CandidateJobDetailView.vue](frontend/src/views/CandidateJobDetailView.vue) and the corresponding candidate route in [frontend/src/router/index.ts](frontend/src/router/index.ts), so candidates can open a dedicated岗位详情页 with public job data plus their personalized match analysis, then complete apply/favorite/ignore actions without bouncing back to the card list first.
- Updated [frontend/src/views/CandidateDashboardView.vue](frontend/src/views/CandidateDashboardView.vue) so recommendation cards can navigate into the new detail page while preserving in-card action buttons through click propagation control.
- Added a formal Candidate upload entry to [frontend/src/views/CandidateProfileView.vue](frontend/src/views/CandidateProfileView.vue) and introduced the browser-side preprocessing contract in [frontend/src/wasm/resumePreprocessor.ts](frontend/src/wasm/resumePreprocessor.ts), so the frontend now prepares a lightweight browser-generated reference before calling `POST /api/resumes/upload` instead of treating raw PDF upload as the final long-term flow.
- Replaced the upload placeholder in [frontend/src/wasm/resumePreprocessor.ts](frontend/src/wasm/resumePreprocessor.ts) with a real browser-side PDF renderer backed by `pdfjs-dist`, including per-page thumbnail generation, text snippet extraction, and progress callbacks.
- Replaced the interim `pdfjs-dist` renderer in [frontend/src/wasm/resumePreprocessor.ts](frontend/src/wasm/resumePreprocessor.ts) with a real PDFium WebAssembly integration backed by [frontend/package.json](frontend/package.json), so the project now uses a genuine Wasm PDF parsing/rendering core and keeps TypeScript only as the browser binding layer.
- Updated [frontend/src/views/CandidateProfileView.vue](frontend/src/views/CandidateProfileView.vue) to surface render progress, page counts, text summary, and page-level preview cards before the candidate submits the lightweight upload reference.
- Updated [frontend/src/views/CandidateProfileView.vue](frontend/src/views/CandidateProfileView.vue) and [frontend/src/wasm/resumePreprocessor.ts](frontend/src/wasm/resumePreprocessor.ts) so candidate upload now submits optional page-image batching metadata alongside the lightweight reference, allowing the backend and AI service to consume browser-side previews without breaking the current upload contract.
- Fixed the page-image batching policy in [frontend/src/wasm/resumePreprocessor.ts](frontend/src/wasm/resumePreprocessor.ts) so upload payloads now carry only the first 3 preview pages by default, while [frontend/src/views/CandidateProfileView.vue](frontend/src/views/CandidateProfileView.vue) keeps full local preview rendering visible to the user.
- Verified the new Candidate profile flow with static checks and a production frontend build, keeping the existing Candidate dashboard and applications path intact.

### Next
- Walk the full Candidate demo path once to confirm profile maintenance, upload, resume state, recommendations, actions, and timeline can be recorded as a stable demo sequence.
- Decide later whether the default upload cap should stay at 3 preview pages or be raised for longer sample resumes after measuring payload size and model input cost.

### Phase 2 - Candidate 走查补断点
- Updated [frontend/src/views/CandidateJobDetailView.vue](frontend/src/views/CandidateJobDetailView.vue) so public job details now remain available even when `POST /api/candidate/match-jobs` cannot yet generate a personalized analysis, preventing the Candidate detail page from collapsing into a full-page error just because resume parsing is still incomplete.
- Added [frontend/src/utils/candidateActivity.ts](frontend/src/utils/candidateActivity.ts) and wired [frontend/src/views/CandidateDashboardView.vue](frontend/src/views/CandidateDashboardView.vue), [frontend/src/views/CandidateJobDetailView.vue](frontend/src/views/CandidateJobDetailView.vue), and [frontend/src/components/CandidateTimeline.vue](frontend/src/components/CandidateTimeline.vue) so apply/favorite/ignore actions immediately refresh the timeline instead of forcing a manual page reload during demo walkthroughs.
- Updated [frontend/src/views/CandidateJobDetailView.vue](frontend/src/views/CandidateJobDetailView.vue) to align the ignored-state rules with the recommendation list, preventing candidates from directly applying from a job that is currently marked as ignored until they explicitly cancel that state.
- Updated [frontend/src/views/CandidateApplicationsView.vue](frontend/src/views/CandidateApplicationsView.vue) so application records can route directly into the Candidate job detail page, making the recommendation -> apply -> application list -> detail walkthrough continuous.
- Updated [frontend/src/views/CandidateProfileView.vue](frontend/src/views/CandidateProfileView.vue) to auto-refresh pending resume parsing state after upload, reducing the chance that the Candidate flow appears stuck at a stale “待解析” status while the backend queue is still progressing.

### Next
- Re-run the Candidate demo path end-to-end with a fresh account: profile update, upload, parse completion, recommendation generation, apply/favorite/ignore, timeline refresh, and application detail fallback.
- Decide whether the next Candidate-side polish should focus on more explicit recommendation gating when no parsed resume exists, or on mobile-specific layout tightening for the答辩演示路径。

### Phase 1 - 联调收口与接口统一
- 将 [frontend/src/views/ResumeListView.vue](frontend/src/views/ResumeListView.vue) 中遗留的 `fetch` 请求统一迁移到共享 API 客户端，消除页面手拼 URL 与响应裸读。
- 将 [frontend/src/views/RegisterView.vue](frontend/src/views/RegisterView.vue) 接入真实注册接口，并将 [frontend/src/stores/auth.ts](frontend/src/stores/auth.ts) 扩展为支持真实用户信息、注册调用、`/v1/users/me` 拉取和基于角色的首页分流。
- 更新 [frontend/src/router/index.ts](frontend/src/router/index.ts) 与 [frontend/src/layouts/ATSLayout.vue](frontend/src/layouts/ATSLayout.vue)，补上 Candidate 投递记录路由与按真实角色显示的导航、页头按钮和用户信息，移除硬编码管理员展示。
- 将 [frontend/src/views/CandidateApplicationsView.vue](frontend/src/views/CandidateApplicationsView.vue) 与 [frontend/src/components/CandidateTimeline.vue](frontend/src/components/CandidateTimeline.vue) 接入真实后端数据，清除候选人端假数据和不存在字段依赖。
- 为 [frontend/src/views/CandidateDashboardView.vue](frontend/src/views/CandidateDashboardView.vue)、[frontend/src/views/JobsView.vue](frontend/src/views/JobsView.vue)、[frontend/src/views/JobDetailView.vue](frontend/src/views/JobDetailView.vue) 和 [frontend/src/views/DashboardView.vue](frontend/src/views/DashboardView.vue) 等核心页面补上更明确的 loading、error、empty 状态反馈，并修复岗位详情页权重滑块提交前的数值类型问题。
- 为 [frontend/src/views/ResumeListView.vue](frontend/src/views/ResumeListView.vue) 和 [frontend/src/views/JobsView.vue](frontend/src/views/JobsView.vue) 接入真实分页元数据、页码切换和动态统计，避免继续使用前端假分页。
- 修正 [frontend/src/views/ResumeListView.vue](frontend/src/views/ResumeListView.vue) 与 [frontend/src/views/JobDetailView.vue](frontend/src/views/JobDetailView.vue) 对后端 DTO 的错误假设，移除未被后端支持的 `jobId` 上传字段，并改为从 `requirements`、`parsedData`、`contactInfo` 等真实字段派生展示信息。
- 复核当前前端已使用的 API 路径与后端 Controller 映射，确认 Phase 1 范围内不再存在遗留 `fetch` 或页面手写 URL 的断点。
- 将 [frontend/src/views/RegisterView.vue](frontend/src/views/RegisterView.vue)、[frontend/src/views/LoginView.vue](frontend/src/views/LoginView.vue)、[frontend/src/views/CandidateApplicationsView.vue](frontend/src/views/CandidateApplicationsView.vue)、[frontend/src/views/CandidateDashboardView.vue](frontend/src/views/CandidateDashboardView.vue) 和 [frontend/src/views/ResumeListView.vue](frontend/src/views/ResumeListView.vue) 的 `alert`、控制台静默失败和缺失空态替换为页面内反馈，补齐注册成功提示、投递记录重试入口、候选人操作成功/失败提示与简历列表操作反馈。

### Next
- 按 HR 与 Candidate 两条主路径实际走查一遍，确认当前页面提示、空态和错误态已经足够支撑录制演示。
- 开始 Phase 2 上传入口收尾，把 Wasm PDF 预处理的 UI 契约落到真实页面流转上。

### Config Hygiene - Local Env Split
- Added tracked template [frontend/.env.example](frontend/.env.example) and local ignored [frontend/.env.local](frontend/.env.local) so proxy target, direct API base URL, and optional demo credentials can stay machine-specific.
- Updated [frontend/src/views/LoginView.vue](frontend/src/views/LoginView.vue) to stop exposing hardcoded `admin/admin` in tracked source and instead read optional local demo credentials from Vite env.
- Updated [frontend/src/views/ResumeListView.vue](frontend/src/views/ResumeListView.vue) and [frontend/vite.config.ts](frontend/vite.config.ts) to remove tracked localhost hardcoding in favor of env-driven API configuration.
- Added [frontend/src/vite-env.d.ts](frontend/src/vite-env.d.ts) so `import.meta.env` usage is typed correctly in the Vue codebase.

### Next
- Keep env-driven API configuration aligned with the remaining Phase 1 README cleanup so local demo credentials and proxy behavior stay documented.
- Start Phase 2 upload work by introducing the Wasm PDF preprocessing contract in UI state and transport layers before backend upload wiring expands further.

### Milestone 1 - Frontend Scaffolding
- Bootstrap Vue 3 + Vite + TypeScript application framework.
- Setup fundamental Node build chain and configuration tools.
- Integrated core libraries to handle application features:
  - `vue-router` for view routing.
  - `pinia` for state management.
  - `axios` for centralized API interfacing.
  - `echarts` prep for Candidate Radar profiles.
- Integrated styling foundation:
  - `tailwindcss`, `postcss`, and `autoprefixer` mapped to standard Vite pipelines.
- Implemented Frontend specific `GUIDE.md` highlighting structural architecture and Wasm offload requirements.
- Appended UI Style requirements (Tokyo Night Theme `#24283b`, Vue transitions, and Tailwind micro-interactions) to `GUIDE.md`.
- Confirmed a successful baseline production build pipeline (`npm run build`).

### Next
- Expand the `src` directory to map directly to the structure detailed in `GUIDE.md`.
- Develop the WebAssembly (Wasm) architecture framework for browser-side PDF-to-Image rendering.
- Build initial dummy View components and structure the App Layout.
- Config and implement `ESLint` and `Prettier` rules.
