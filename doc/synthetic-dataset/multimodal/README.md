# Multimodal Demo Bundle

本目录用于补齐 Phase 3 的多模态演示材料，重点解决两件事：

1. 提供可本地托管的 HTML 作品集样例。
2. 提供可本地托管的 GitHub 仓库页面样例。

同时，这里也配套一份可打印导出为 PDF 的简历 HTML 源，方便把 Candidate 上传演示从 Markdown 进一步收口到答辩可用的样例文件。

## 目录结构

- `portfolio_C01_林泽远.html`: 本地作品集样例页面。
- `github_C01_linzeyuan-example.html`: GitHub 仓库页面样例，内容已按当前 AI 服务抽取规则组织。
- `bundle_C01_manifest.json`: C01 的多模态输入清单与建议映射。
- `screenshot_shotlist.md`: 演示截图与录屏取证清单。
- `../printable/C01_林泽远_resume_print.html`: 可通过浏览器导出为 PDF 的简历排版源。

## 本地托管方式

在当前目录执行：

```bash
cd doc/synthetic-dataset/multimodal
python3 -m http.server 8088
```

然后可以得到如下本地演示 URL：

- 作品集样例: `http://127.0.0.1:8088/portfolio_C01_%E6%9E%97%E6%B3%BD%E8%BF%9C.html`
- GitHub 样例: `http://127.0.0.1:8088/github_C01_linzeyuan-example.html`

Candidate 侧可把这两个地址填入资料页的 `portfolioUrl` 与 `githubUrl`，从而走完整的外链聚合解析链路。

## PDF 样例导出方式

当前仓库同时提供了可直接使用的 PDF 样例和可打印 HTML 源：

1. 可直接使用 `../pdf/C01_resume_sample.pdf` 做第一份上传演示。
2. 也可以用浏览器打开 `../printable/C01_林泽远_resume_print.html`。
3. 选择“打印”或“导出为 PDF”。
4. 建议文件名统一为 `C01_林泽远.pdf`。

这样既保留了直接可用的 PDF 输入样例，也保留了后续扩展更多样例时的统一版式来源。

## 建议演示顺序

1. 先打开可打印简历 HTML 并导出 PDF。
2. 候选人资料页填写本地 GitHub 与作品集 URL。
3. 上传导出的 PDF，走浏览器端 Wasm 预处理。
4. 触发解析并检查 AI 服务是否同时吸收 PDF、作品集 HTML 与 GitHub 结构化上下文。
5. 按 `screenshot_shotlist.md` 记录输入与输出证据。