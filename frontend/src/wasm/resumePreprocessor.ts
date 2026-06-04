import { PDFiumLibrary } from '@hyzyla/pdfium';
import pdfiumWasmUrl from '@hyzyla/pdfium/pdfium.wasm?url';

export interface BrowserResumePagePreview {
  pageNumber: number;
  width: number;
  height: number;
  imageDataUrl: string;
  textPreview: string;
}

export interface BrowserResumePreprocessProgress {
  stage: 'loading' | 'rendering' | 'finalizing' | 'done';
  completedPages: number;
  totalPages: number | null;
  percentage: number;
  currentPage: number | null;
  message: string;
}

export interface BrowserResumePreprocessOptions {
  onProgress?: (progress: BrowserResumePreprocessProgress) => void;
  maxPreviewWidth?: number;
  maxTextPreviewLength?: number;
}

export interface BrowserResumePreprocessResult {
  engine: 'pdfium-wasm-renderer';
  mode: 'pdf-to-page-previews';
  sourceFileName: string;
  sourceMimeType: string;
  sourceFileSize: number;
  derivedReference: string;
  pageCountEstimate: number;
  generatedAt: string;
  warnings: string[];
  pagePreviews: BrowserResumePagePreview[];
  extractedTextPreview: string;
}

export interface BrowserResumeUploadPayload {
  engine: string;
  mode: string;
  sourceFileName: string;
  sourceMimeType: string;
  sourceFileSize: number;
  derivedReference: string;
  pageCount: number;
  extractedTextPreview: string;
  generatedAt: string;
  warnings: string[];
  pagePreviews: BrowserResumePagePreview[];
}

export const MAX_BROWSER_UPLOAD_PREVIEW_PAGES = 3;

class ResumePreprocessValidationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'ResumePreprocessValidationError';
  }
}

let pdfiumLibraryPromise: Promise<PDFiumLibrary> | null = null;

const sanitizeFileName = (fileName: string) => fileName
  .toLowerCase()
  .replace(/[^a-z0-9._-]+/g, '-')
  .replace(/-+/g, '-')
  .slice(0, 60);

const fallbackFingerprint = (data: Uint8Array) => {
  let hashA = 0x811c9dc5;
  let hashB = 0x9e3779b9;

  for (const value of data) {
    hashA ^= value;
    hashA = Math.imul(hashA, 0x01000193);
    hashB ^= value;
    hashB = Math.imul(hashB, 0x85ebca6b);
  }

  const partA = (hashA >>> 0).toString(16).padStart(8, '0');
  const partB = (hashB >>> 0).toString(16).padStart(8, '0');
  return `${partA}${partB}`;
};

const shortFingerprint = async (input: string) => {
  const data = new TextEncoder().encode(input);
  const subtleCrypto = globalThis.crypto?.subtle;

  if (!subtleCrypto) {
    return fallbackFingerprint(data).slice(0, 16);
  }

  const digest = await subtleCrypto.digest('SHA-256', data);
  return Array.from(new Uint8Array(digest))
    .slice(0, 8)
    .map((value) => value.toString(16).padStart(2, '0'))
    .join('');
};

const emitProgress = (
  options: BrowserResumePreprocessOptions | undefined,
  progress: BrowserResumePreprocessProgress,
) => {
  options?.onProgress?.(progress);
};

const getPdfiumLibrary = () => {
  if (!pdfiumLibraryPromise) {
    pdfiumLibraryPromise = PDFiumLibrary.init({ wasmUrl: pdfiumWasmUrl });
  }

  return pdfiumLibraryPromise;
};

const rgbaBitmapToJpegDataUrl = (data: Uint8Array, width: number, height: number) => {
  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = height;

  const context = canvas.getContext('2d');
  if (!context) {
    throw new Error('浏览器无法创建 Wasm PDF 预览画布上下文。');
  }

  const imageData = new ImageData(new Uint8ClampedArray(data), width, height);
  context.putImageData(imageData, 0, 0);
  return canvas.toDataURL('image/jpeg', 0.82);
};

const buildTextPreview = (pageText: string, maxTextPreviewLength: number) => pageText
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, maxTextPreviewLength);

const describeError = (error: unknown) => {
  if (error instanceof Error) {
    return error.message;
  }

  if (typeof error === 'string') {
    return error;
  }

  return 'unknown error';
};

const estimateInkCoverage = (rgbaData: Uint8Array) => {
  if (rgbaData.length < 4) {
    return 0;
  }

  let sampledPixels = 0;
  let nonBlankPixels = 0;

  for (let offset = 0; offset <= rgbaData.length - 4; offset += 32) {
    sampledPixels += 1;

    const red = rgbaData[offset];
    const green = rgbaData[offset + 1];
    const blue = rgbaData[offset + 2];
    const alpha = rgbaData[offset + 3];
    const brightness = (red + green + blue) / 3;

    if (alpha > 16 && brightness < 245) {
      nonBlankPixels += 1;
    }
  }

  return sampledPixels === 0 ? 0 : nonBlankPixels / sampledPixels;
};

const buildFallbackResult = async (
  file: File,
  sanitizedName: string,
  totalPages: number,
  warnings: string[],
  extractedTextPreview: string,
): Promise<BrowserResumePreprocessResult> => {
  const fingerprint = await shortFingerprint([
    file.name,
    String(file.size),
    String(file.lastModified),
    String(totalPages),
    extractedTextPreview,
    warnings.join('|'),
  ].join(':'));

  return {
    engine: 'pdfium-wasm-renderer',
    mode: 'pdf-to-page-previews',
    sourceFileName: file.name,
    sourceMimeType: file.type || 'application/pdf',
    sourceFileSize: file.size,
    derivedReference: `browser-pdf-preview://${fingerprint}/${totalPages}p/${sanitizedName}`,
    pageCountEstimate: totalPages,
    generatedAt: new Date().toISOString(),
    warnings,
    pagePreviews: [],
    extractedTextPreview,
  };
};

export const preprocessResumePdfInBrowser = async (
  file: File,
  options?: BrowserResumePreprocessOptions,
): Promise<BrowserResumePreprocessResult> => {
  const looksLikePdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');
  const maxPreviewWidth = options?.maxPreviewWidth ?? 280;
  const maxTextPreviewLength = options?.maxTextPreviewLength ?? 160;

  if (!looksLikePdf) {
    throw new Error('当前仅支持 PDF 简历作为浏览器端预处理输入。');
  }
  
  const MAX_WASM_FILE_SIZE = 15 * 1024 * 1024; // 15MB 物理熔断
  if (file.size > MAX_WASM_FILE_SIZE) {
    throw new Error(`文件大小 (${(file.size / 1024 / 1024).toFixed(1)}MB) 超出浏览器 Wasm 层安全处理阈值 (15MB)，已断路保护。请流转为后端服务器解析。`);
  }

  emitProgress(options, {
    stage: 'loading',
    completedPages: 0,
    totalPages: null,
    percentage: 8,
    currentPage: null,
    message: '正在读取本地 PDF，并初始化浏览器端渲染引擎...',
  });

  const fileBytes = await file.arrayBuffer();

  const library = await getPdfiumLibrary();
  const document = await library.loadDocument(new Uint8Array(fileBytes));
  const sanitizedName = sanitizeFileName(file.name || 'resume.pdf');

  try {
    const totalPages = document.getPageCount();
    
    // 页数软熔断：超过 15 页的超长跨页 PDF 在浏览器逐页 render 时极易发生内存溢出
    const MAX_SAFE_PAGES = 15;
    if (totalPages > MAX_SAFE_PAGES) {
      throw new Error(`检测到该 PDF 包含 ${totalPages} 页。为保护浏览器内存 (OOM 熔断)，已阻断 Wasm 预处理，请直接交由后端云端节点解析。`);
    }
    
    const pagePreviews: BrowserResumePagePreview[] = [];
    const warnings: string[] = [];
    const pageInkCoverageRatios: number[] = [];

    if (totalPages > 6) {
      warnings.push('该 PDF 页数较多，浏览器端逐页渲染时间会相应增加。');
    }

    emitProgress(options, {
      stage: 'rendering',
      completedPages: 0,
      totalPages,
      percentage: 15,
      currentPage: 1,
      message: `PDF 已读取完成，准备渲染 ${totalPages} 页预览。`,
    });

    for (let pageNumber = 1; pageNumber <= totalPages; pageNumber += 1) {
      const page = document.getPage(pageNumber - 1);
      const { originalWidth } = page.getOriginalSize();
      const previewScale = Math.min(maxPreviewWidth / Math.max(originalWidth, 1), 1.4);
      const effectiveScale = Number.isFinite(previewScale) && previewScale > 0 ? previewScale : 1;
      const pageText = page.getText();
      const renderedPage = await page.render({
        scale: effectiveScale,
        render: 'bitmap',
      });
      const inkCoverage = estimateInkCoverage(renderedPage.data);
      const textPreview = buildTextPreview(pageText, maxTextPreviewLength);
      const imageDataUrl = rgbaBitmapToJpegDataUrl(renderedPage.data, renderedPage.width, renderedPage.height);

      pageInkCoverageRatios.push(inkCoverage);

      pagePreviews.push({
        pageNumber,
        width: renderedPage.width,
        height: renderedPage.height,
        imageDataUrl,
        textPreview,
      });

      const percentage = Math.round(15 + (pageNumber / totalPages) * 75);
      emitProgress(options, {
        stage: 'rendering',
        completedPages: pageNumber,
        totalPages,
        percentage,
        currentPage: pageNumber,
        message: `正在完成第 ${pageNumber} / ${totalPages} 页的浏览器端渲染。`,
      });
    }

    emitProgress(options, {
      stage: 'finalizing',
      completedPages: totalPages,
      totalPages,
      percentage: 94,
      currentPage: totalPages,
      message: '页面预览已生成，正在汇总轻量引用与文本摘要...',
    });

    const extractedTextPreview = pagePreviews
      .map((page) => page.textPreview)
      .filter(Boolean)
      .join(' ')
      .replace(/\s+/g, ' ')
      .trim()
      .slice(0, 420);

    const maxInkCoverage = pageInkCoverageRatios.length > 0
      ? Math.max(...pageInkCoverageRatios)
      : 0;

    if (!extractedTextPreview && maxInkCoverage < 0.0025) {
      throw new ResumePreprocessValidationError(
        '该 PDF 未检测到可解析文字或有效页面内容，疑似空白文件，已拒绝上传。请更换有效简历后重试。',
      );
    }

    if (!extractedTextPreview) {
      warnings.push('当前 PDF 页面未提取到明显文本，后续解析将更依赖图像或 OCR 能力。');
    }

    const fingerprint = await shortFingerprint([
      file.name,
      String(file.size),
      String(file.lastModified),
      String(totalPages),
      extractedTextPreview,
    ].join(':'));

    const result: BrowserResumePreprocessResult = {
      engine: 'pdfium-wasm-renderer',
      mode: 'pdf-to-page-previews',
      sourceFileName: file.name,
      sourceMimeType: file.type || 'application/pdf',
      sourceFileSize: file.size,
      derivedReference: `browser-pdf-preview://${fingerprint}/${totalPages}p/${sanitizedName}`,
      pageCountEstimate: totalPages,
      generatedAt: new Date().toISOString(),
      warnings: warnings.length > 0
        ? warnings
        : ['正式主流程仍只向后端提交轻量引用，避免把原始重 PDF 当作长期传输格式。'],
      pagePreviews,
      extractedTextPreview,
    };

    emitProgress(options, {
      stage: 'done',
      completedPages: totalPages,
      totalPages,
      percentage: 100,
      currentPage: totalPages,
      message: `浏览器端 PDF 渲染完成，共生成 ${totalPages} 页预览。`,
    });

    return result;
  } catch (error) {
    if (error instanceof ResumePreprocessValidationError) {
      throw error;
    }

    console.error('Failed to preprocess PDF in browser', error);
    const fallbackPageCount = (() => {
      try {
        return document.getPageCount();
      } catch {
        return 1;
      }
    })();
    const fallbackWarnings = [
      'PDF 文件已读取成功，但浏览器端 PDFium 页面渲染未完成，已降级为仅上传轻量引用与元数据。',
      `PDFium 渲染失败原因：${describeError(error)}`,
      '后续解析将无法使用本地页预览图，效果会更依赖后端引用、外链信息或模型兜底能力。',
    ];

    emitProgress(options, {
      stage: 'done',
      completedPages: 0,
      totalPages: fallbackPageCount,
      percentage: 100,
      currentPage: null,
      message: 'PDF 已以降级模式完成预处理，可继续上传轻量引用。',
    });

    return buildFallbackResult(file, sanitizedName, fallbackPageCount, fallbackWarnings, '');
  } finally {
    document.destroy();
  }
};

export const buildBrowserResumeUploadPayload = (
  result: BrowserResumePreprocessResult,
): BrowserResumeUploadPayload => {
  const uploadPagePreviews = result.pagePreviews.slice(0, MAX_BROWSER_UPLOAD_PREVIEW_PAGES);
  const uploadWarnings = [...result.warnings];

  if (result.pagePreviews.length > MAX_BROWSER_UPLOAD_PREVIEW_PAGES) {
    uploadWarnings.push(
      `为控制请求体与多模态输入成本，上传负载仅携带前 ${MAX_BROWSER_UPLOAD_PREVIEW_PAGES} 页预览；完整预览仍保留在浏览器本地。`,
    );
  }

  return {
    engine: result.engine,
    mode: result.mode,
    sourceFileName: result.sourceFileName,
    sourceMimeType: result.sourceMimeType,
    sourceFileSize: result.sourceFileSize,
    derivedReference: result.derivedReference,
    pageCount: result.pageCountEstimate,
    extractedTextPreview: result.extractedTextPreview,
    generatedAt: result.generatedAt,
    warnings: uploadWarnings,
    pagePreviews: uploadPagePreviews,
  };
};