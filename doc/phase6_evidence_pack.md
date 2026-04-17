# Phase 6 证据包与复现说明

本仓库现在提供一套可重复执行的本地演示与证据导出入口，目标不是生成漂亮截图，而是稳定复现以下几类事实：

- 本地三服务可以按统一入口启动。
- 合成数据集可以通过真实 API 幂等导入系统。
- AI mock 解析能够基于样本真值文件重建稳定画像。
- HR 与 Candidate 两侧推荐结果可以导出为结构化证据报告。

## 一键启动与导入

在仓库根目录执行：

```bash
tools/maintenance/bootstrap_local_demo.sh --with-demo-data
```

该命令会完成：

1. 启动 backend、ai-service、frontend。
2. 通过真实接口导入演示岗位、候选人、简历与部分投递数据。
3. 触发岗位评估。
4. 生成 Phase 6 报告 JSON。

停止命令：

```bash
tools/maintenance/stop_local_demo.sh
```

## 产物位置

- 演示导入摘要：`tools/maintenance/output/seed_demo_data_summary.json`
- Phase 6 报告：`tools/maintenance/output/phase6_report.json`
- 运行日志：`tools/maintenance/logs/`

## Phase 6 报告内容

`phase6_report.json` 包含以下关键信息：

- 管理员总览快照：用户、岗位、简历、技能词典数量。
- HR 侧岗位推荐结果摘要：每个岗位的推荐人数与 Top 候选人。
- Candidate 侧排序命中情况：按合成数据集 `expectedTopJobs` 对比系统实际 Top-1 / Top-3 推荐。

## 说明

- 本证据链默认依赖本地 PostgreSQL、Redis、backend local 配置和 ai-service `.env` 已可用。
- `seed_demo_data.py` 是幂等导入脚本，多次执行时会优先复用已存在的账号、岗位和简历引用。
- 合成简历上传不会直接传重 PDF，而是构造带 `Cxx` 样本 ID 的浏览器预处理引用，驱动 AI mock 解析读取 `doc/synthetic-dataset/truth/` 中的真值 JSON。