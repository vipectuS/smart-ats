# Screenshot Shotlist

本清单用于补齐 Phase 3 的“输入样例与输出画像结果截图”证据。

## 必拍输入截图

1. Candidate 资料页中填写本地作品集 URL 与 GitHub URL。
2. 浏览器端 PDF 渲染完成后的逐页预览与文本摘要。
3. 本地托管的作品集 HTML 页面。
4. 本地托管的 GitHub 仓库样例页面。

## 必拍输出截图

1. 简历状态从 `PENDING_PARSE` 或 `PARSING` 进入 `PARSED`。
2. HR 或 Candidate 侧出现结构化画像摘要。
3. 推荐页或岗位详情页展示匹配解释与技能缺口。
4. 如条件允许，再拍一张 AI 服务日志或回调成功记录，证明多源输入已进入统一链路。

## 建议命名

- `phase3_input_profile_urls.png`
- `phase3_input_pdf_preprocess.png`
- `phase3_input_portfolio_page.png`
- `phase3_input_github_page.png`
- `phase3_output_resume_parsed.png`
- `phase3_output_profile_summary.png`
- `phase3_output_job_fit.png`

## 备注

当前工作区已经按本清单生成成功态截图，输出目录为 `doc/synthetic-dataset/screenshots/`。

当前已得到的关键证据包括：

1. 多源输入截图：资料页 URL 填写、本地作品集页、本地 GitHub 样例页、浏览器端 PDF 预处理结果。
2. 输出结果截图：简历解析完成、画像摘要展示、推荐页匹配结果与技能缺口说明。
3. 推荐结果截图已经重新验证为成功态，不再是“推荐结果加载失败”的错误页。