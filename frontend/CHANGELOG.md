# Changelog

All notable changes to the Agent Frontend (UI & Wasm) microservice will be documented in this file.

## [Unreleased]

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
