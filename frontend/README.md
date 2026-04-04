# Frontend Module

该目录是 Smart ATS 的前端模块，负责：
- HR 看板与岗位评估页面
- Candidate 推荐与行为页面
- 图表展示与交互式权重调整
- 后续的 Wasm/PDF 前处理能力

## 常用命令

```bash
npm install
npm run dev
npm run build
```

## 目录说明

- `src/views/`: 页面级组件
- `src/components/`: 可复用组件
- `src/stores/`: 状态管理
- `src/utils/api.ts`: 统一 API 访问入口

## 当前注意事项

- 页面应统一使用 `src/utils/api.ts`，避免混用手写 fetch 与 axios 解包逻辑。
- 任何 mock 文案或模拟成功提示，在进入答辩前都应替换为真实接口流程。
- 如果新增临时修补脚本，不要长期保留；能直接改源码就不要生成脚本。
