# Synthetic Dataset Pack v1

本目录用于存放当前项目的第一版合成数据集，目标是解决“没有真实授权简历数据时，如何继续做系统演示、实验和论文支撑”的问题。

## 目录结构

- `jobs.json`: 6 个岗位模板，可直接作为 HR 演示或后续导入脚本的输入草案。
- `manifest.json`: 全部候选人样本索引，包含目标岗位、强项、短板和预期匹配结果。
- `resumes/`: 8 份虚构简历 Markdown，可后续导出为 PDF 供 Candidate 上传演示。
- `pdf/`: 已准备好的 PDF 样例，可直接用于浏览器端 Wasm 预处理演示。
- `truth/`: 8 份真值 JSON，用于解析评估与人工对照。
- `printable/`: 可直接在浏览器中打印导出为 PDF 的简历排版源。
- `multimodal/`: Phase 3 多模态演示样例，包括本地作品集 HTML、GitHub 仓库页面样例、输入清单与截图清单。

## 使用原则

1. 所有样本均为完全虚构，不对应任何真实个人或机构。
2. 简历内容是“可演示样本”，不是“真实招聘语料”。
3. 后续如果用 LLM 扩充数据，必须继续遵守 `doc/合成数据集与手工演示方案.md` 中的约束。

## 当前建议用法

1. 先用 `jobs.json` 在 HR 侧准备岗位。
2. 可直接使用 `pdf/C01_resume_sample.pdf` 做第一份 Wasm 上传演示，也可以再从 `printable/` 或 `resumes/` 里扩展 3 到 5 份 PDF。
3. 用 `truth/` 中的结构化真值表做解析字段核对。
4. 用 `manifest.json` 中的预期匹配说明做 Top-N 合理性评价。
5. 用 `multimodal/` 中的本地 HTML / GitHub 页面样例补齐 Phase 3 外链聚合演示。

## 当前样本概览

- 岗位数量: 6
- 候选人样本数量: 8
- 覆盖方向: Kotlin/Java 后端、Vue 前端、测试自动化、数据分析、DevOps、全栈、跨方向低匹配样本

## 下一步

1. 按 `multimodal/screenshot_shotlist.md` 收集输入与输出截图证据。
2. 如果要做论文实验，可以继续把这 8 份扩到 15 到 20 份。