<template>
  <section class="h-full rounded-xl border border-slate-200 bg-white p-8 shadow-sm flex flex-col">
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h2 class="flex items-center gap-2 text-xl font-bold text-slate-800">
          <FileText class="h-6 w-6 text-blue-600" />
          我的简历
        </h2>
        <p class="mt-1 text-sm text-slate-500">上传智能解析，支持 PDF 格式</p>
      </div>
      <div v-if="resumeDetail" class="flex gap-2">
        <span
          class="inline-flex items-center rounded-full px-3 py-1 text-xs font-medium"
          :class="statusBadgeClasses[resumeDetail.status]"
        >
          <CheckCircle2 v-if="resumeDetail.status === 'COMPLETED'" class="mr-1 h-3 w-3" />
          <Loader2 v-else-if="resumeDetail.status === 'PARSING'" class="mr-1 h-3 w-3 animate-spin" />
          <AlertCircle v-else class="mr-1 h-3 w-3" />
          {{ statusText[resumeDetail.status] || resumeDetail.status }}
        </span>
      </div>
    </div>
    
    <div
      class="group relative flex-1 flex flex-col items-center justify-center rounded-2xl border-2 border-dashed border-slate-300 bg-slate-50 p-8 text-center transition hover:border-blue-500 hover:bg-blue-50/50"
      @dragover.prevent="isDragging = true"
      @dragleave.prevent="isDragging = false"
      @drop.prevent="handleDrop"
      :class="{ 'border-blue-500 bg-blue-50': isDragging, 'cursor-not-allowed opacity-70': isUploading }"
    >
      <input
        type="file"
        class="absolute inset-0 h-full w-full cursor-pointer opacity-0"
        accept="application/pdf"
        @change="handleFileChange"
        :disabled="isUploading"
      />
      
      <CloudUpload class="mb-4 h-12 w-12 text-slate-400 group-hover:text-blue-500 transition-colors" />
      <h3 class="mb-2 text-lg font-semibold text-slate-700">点击或将 PDF 拖拽至此处</h3>
      <p class="text-sm text-slate-500">支持 5MB 以内的 PDF，客户端 Wasm 渲染保障隐私</p>

      <div v-if="isUploading" class="mt-6 w-full max-w-sm rounded-lg bg-white p-4 shadow-sm border border-slate-100">
        <div class="mb-2 flex items-center justify-between text-sm">
          <span class="font-medium text-slate-700">{{ uploadStatusText }}</span>
          <span class="text-blue-600 font-semibold">{{ Math.round(uploadProgress) }}%</span>
        </div>
        <div class="h-2.5 w-full overflow-hidden rounded-full bg-slate-100">
          <div
            class="h-full bg-blue-600 transition-all duration-300"
            :style="{ width: `${uploadProgress}%` }"
          ></div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { 
  FileText, CheckCircle2, Loader2, AlertCircle, CloudUpload 
} from 'lucide-vue-next';
import api from '@/utils/api';
import { preprocessResumePdfInBrowser, buildBrowserResumeUploadPayload } from '@/wasm/resumePreprocessor';

const props = defineProps<{
  resumeDetail: any;
}>();

const emit = defineEmits<{
  (e: 'update'): void;
}>();

const isDragging = ref(false);
const isUploading = ref(false);
const uploadProgress = ref(0);
const uploadStatusText = ref('');

const statusBadgeClasses: Record<string, string> = {
  PENDING_PARSE: 'bg-amber-100 text-amber-800',
  PARSING: 'bg-blue-100 text-blue-800',
  PARSED: 'bg-emerald-100 text-emerald-800',
  PARSE_FAILED: 'bg-rose-100 text-rose-800'
};

const statusText: Record<string, string> = {
  PENDING_PARSE: '待解析',
  PARSING: '解析中',
  PARSED: '解析完成',
  PARSE_FAILED: '解析失败'
};

const processFile = async (file: File) => {
  if (file.type !== 'application/pdf') {
    alert('请上传 PDF 格式的文件');
    return;
  }
  if (file.size > 5 * 1024 * 1024) {
    alert('文件大小不能超过 5MB');
    return;
  }

  isUploading.value = true;
  uploadProgress.value = 0;
  uploadStatusText.value = '客户端 Wasm 转换图集中...';

  try {
    const result = await preprocessResumePdfInBrowser(file, {
      onProgress: (progressInfo: any) => {
        // Scale 0-100 to 0-70% for Wasm preprocessing
        uploadProgress.value = progressInfo.percentage * 0.7;
      }
    });

    uploadStatusText.value = '打包并上传至服务端...';
    const payload = buildBrowserResumeUploadPayload(result);

    await api.post('/resumes/upload', {
      candidateName: null,
      contactInfo: null,
      rawContentReference: payload.derivedReference,
      browserPreprocessedPayload: payload,
      parsedData: null,
    }, {
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total) {
          // The remaining 30% is actual network upload
          const networkProgress = (progressEvent.loaded / progressEvent.total) * 30;
          uploadProgress.value = 70 + networkProgress;
        }
      }
    });

    uploadStatusText.value = '上传成功！';
    uploadProgress.value = 100;
    
    // Notify parent to refresh
    emit('update');
  } catch (error: any) {
    console.error('上传失败:', error);
    const message = error?.response?.data?.message || '上传或处理失败，请重试';
    alert(message);
  } finally {
    isUploading.value = false;
  }
};

const handleDrop = async (e: DragEvent) => {
  isDragging.value = false;
  const file = e.dataTransfer?.files[0];
  if (file) await processFile(file);
};

const handleFileChange = async (e: Event) => {
  const target = e.target as HTMLInputElement;
  const file = target.files?.[0];
  if (file) await processFile(file);
  target.value = '';
};
</script>
