# Frontend Guide (UI & Wasm)

Welcome to the frontend domain of the **Smart ATS** project. This microservice is responsible for the user interface and browser-side inference/preprocessing. 

## 1. Directory Outline

The frontend workspace is organized to support a robust Vue 3 SPA architecture alongside specialized Wasm integration:

```
frontend/
├── src/
│   ├── assets/        # Static images, icons, and global CSS
│   ├── components/    # Reusable Vue 3 Presentational Components
│   ├── views/         # Page-level components
│   ├── router/        # Vue Router configuration
│   ├── stores/        # Pinia state management
│   ├── utils/         # Helper functions and formatting tools
│   ├── services/      # Axios API adapters and endpoints
│   ├── wasm/          # WebAssembly bindings and modules (PDF-to-Image rendering)
│   ├── App.vue        # Application Root
│   └── main.ts        # App Initialization
├── public/            # Public static assets
├── index.html         # Application entry
├── tailwind.config.ts # Optional Tailwind configurations
├── vite.config.ts     # Vite configuration
└── package.json       # Project dependencies & scripts
```

## 2. Wasm Integration Plan
To avoid sending heavy, multi-page PDFs directly to the backend, we offload PDF parsing and rendering to the browser using WebAssembly.
- The `wasm/` directory will contain or interface with Wasm binaries.
- When resuming uploading, the frontend processes the document, extracts structural images/redactions locally, and sends clean images + basic text payload to the backend.
- The current implementation uses PDFium WebAssembly via `@hyzyla/pdfium` in `src/wasm/resumePreprocessor.ts` to generate page previews, text snippets, and a lightweight reference before calling the backend upload contract.

## 3. UI Charting
- **ECharts** is our primary visualization library. It should be used for rendering Candidate Radar Profiles to indicate skill distribution visually within dashboard interfaces.
- Chart logic should be decoupled into dedicated composables or standalone components wrapper around ECharts.

## 4. Build & Test Instructions

### Setup
```bash
npm install
```

### Dev Server
```bash
npm run dev
```

### Build for Production
```bash
npm run build
```

## 5. Code Style & Linting
- **Framework:** Vue 3 via `<script setup>` syntax (Composition API).
- **Styles:** Styling is handled primarily through utility classes (Tailwind CSS v4+).
- **Prettier:** Use Prettier for consistent code formatting. Set up your editor to format on save.
- **ESLint:** Maintain strong TypeScript typing across all logic.

## 6. Environment Variables Template
Environment variables will be detailed in an `.env.example` file. 
At a minimum, ensure:
```env
## 7. UI Style Requirements
- **Theme:** Tokyo Night Theme (primary background `#24283b`).
- **Micro-interactions:** Heavily utilize Vue `<Transition>` components combined with Tailwind CSS classes (e.g., `hover:*`, `transition-*`, `duration-*`, `translate-*`) for smooth, high-quality micro-interactions and transitions across the application. These interactions must be applied to all interactive elements to provide a rich user experience.
