<template>
  <div class="min-h-screen bg-gray-100 p-8">
    <div class="mx-auto max-w-6xl space-y-8">
      <header class="rounded-2xl border border-white/20 bg-white/50 p-8 shadow-sm backdrop-blur-md">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h1 class="text-3xl font-bold text-gray-800">候选人资料</h1>
            <p class="mt-2 text-gray-600">维护 GitHub、作品集和当前简历状态，让推荐结果更完整、更稳定。</p>
          </div>
          <div class="flex gap-3">
            <button
              @click="router.push({ name: 'candidateDashboard' })"
              class="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
            >
              返回推荐页
            </button>
            <button
              @click="router.push({ name: 'candidateApplications' })"
              class="rounded-xl bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white shadow-md shadow-blue-500/20 transition hover:bg-blue-700"
            >
              查看投递记录
            </button>
          </div>
        </div>
      </header>

      <div v-if="feedback" :class="feedback.type === 'error' ? 'border-rose-200 bg-rose-50 text-rose-700' : 'border-emerald-200 bg-emerald-50 text-emerald-700'" class="rounded-2xl border px-5 py-4 text-sm shadow-sm">
        {{ feedback.message }}
      </div>

      <div v-if="loading" class="rounded-2xl border border-white/20 bg-white/60 p-10 text-center text-gray-500 shadow-sm backdrop-blur-md">
        <i class="fas fa-spinner fa-spin mb-3 text-2xl text-blue-500"></i>
        <p>正在加载候选人资料...</p>
      </div>

      <div v-else-if="errorMsg" class="rounded-2xl border border-rose-200 bg-rose-50 p-6 text-center text-rose-700 shadow-sm">
        <p class="font-semibold">候选人资料加载失败</p>
        <p class="mt-2 text-sm">{{ errorMsg }}</p>
        <button @click="fetchProfile" class="mt-4 rounded-lg bg-rose-600 px-4 py-2 text-white transition hover:bg-rose-700">重试</button>
      </div>

      <div v-else-if="profile" class="grid grid-cols-1 gap-8 xl:grid-cols-[1.2fr_0.8fr]">
        <section class="rounded-2xl border border-white/20 bg-white/70 p-8 shadow-sm backdrop-blur-md">
          <div class="mb-8 flex items-center justify-between gap-4">
            <div>
              <h2 class="text-xl font-bold text-slate-800">基本资料</h2>
              <p class="mt-1 text-sm text-slate-500">用户名和邮箱来自账号信息；可编辑字段会直接同步到 Candidate Profile。</p>
            </div>
            <span class="rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">Candidate Profile</span>
          </div>

          <form class="space-y-6" @submit.prevent="saveProfile">
            <div class="grid grid-cols-1 gap-6 md:grid-cols-2">
              <div>
                <label class="mb-2 block text-sm font-semibold text-gray-700" for="candidate-username">用户名</label>
                <input
                  id="candidate-username"
                  :value="profile.username"
                  type="text"
                  disabled
                  class="w-full rounded-lg border border-gray-200 bg-gray-100/70 px-4 py-3 text-gray-500 outline-none"
                >
              </div>
              <div>
                <label class="mb-2 block text-sm font-semibold text-gray-700" for="candidate-email">邮箱</label>
                <input
                  id="candidate-email"
                  :value="profile.email"
                  type="email"
                  disabled
                  class="w-full rounded-lg border border-gray-200 bg-gray-100/70 px-4 py-3 text-gray-500 outline-none"
                >
              </div>
            </div>

            <div>
              <label class="mb-2 block text-sm font-semibold text-gray-700" for="candidate-github">GitHub 链接</label>
              <input
                id="candidate-github"
                v-model.trim="form.githubUrl"
                type="url"
                placeholder="https://github.com/your-name"
                class="w-full rounded-lg border border-gray-300 bg-gray-100/50 px-4 py-3 text-gray-800 placeholder-gray-400 transition-all focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              >
            </div>

            <div>
              <label class="mb-2 block text-sm font-semibold text-gray-700" for="candidate-portfolio">作品集链接</label>
              <input
                id="candidate-portfolio"
                v-model.trim="form.portfolioUrl"
                type="url"
                placeholder="https://your-portfolio.example.com"
                class="w-full rounded-lg border border-gray-300 bg-gray-100/50 px-4 py-3 text-gray-800 placeholder-gray-400 transition-all focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              >
            </div>

            <div class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4 text-sm text-slate-600">
              <p class="font-semibold text-slate-700">当前资料更新时间</p>
              <p class="mt-1">{{ profile.updatedAt ? formatDateTime(profile.updatedAt) : '资料尚未建立，首次保存后会生成更新时间。' }}</p>
            </div>

            <div class="flex justify-end">
              <button
                type="submit"
                :disabled="saving"
                class="inline-flex items-center rounded-xl bg-blue-600 px-5 py-3 text-sm font-semibold text-white shadow-md shadow-blue-500/20 transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-70"
              >
                <svg v-if="saving" class="-ml-1 mr-2 h-4 w-4 animate-spin text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                {{ saving ? '保存中...' : '保存资料' }}
              </button>
            </div>
          </form>
        </section>

        <section class="space-y-6">
          <div class="rounded-2xl border border-white/20 bg-white/70 p-8 shadow-sm backdrop-blur-md">
            <div class="mb-5 flex items-start justify-between gap-4">
              <div>
                <h2 class="text-xl font-bold text-slate-800">浏览器端简历上传</h2>
                <p class="mt-1 text-sm text-slate-500">当前上传入口已经接入真实浏览器端 PDF 渲染，可在提交前生成逐页缩略图、文本摘要和轻量引用。</p>
              </div>
              <span class="rounded-full bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-700">Browser PDF Ready</span>
            </div>

            <div class="space-y-5">
              <div class="rounded-2xl border border-blue-100 bg-blue-50/70 p-4 text-sm text-blue-800">
                <p class="font-semibold">上传策略</p>
                <p class="mt-1">正式主流程不直接上传原始重 PDF。当前页面会先在浏览器端渲染页面预览并生成轻量引用，再提交到候选人上传接口。</p>
                <p class="mt-2 text-xs text-blue-700/80">当前默认只把前 {{ uploadPreviewPageLimit }} 页预览附带到上传负载里，完整逐页预览仍保留在本地页面，用来控制请求体大小与多模态输入成本。</p>
              </div>

              <div>
                <label class="mb-2 block text-sm font-semibold text-gray-700" for="candidate-resume-file">选择 PDF 简历</label>
                <input
                  :key="filePickerKey"
                  id="candidate-resume-file"
                  type="file"
                  accept=".pdf,application/pdf"
                  class="block w-full rounded-lg border border-gray-300 bg-gray-100/50 px-4 py-3 text-sm text-gray-700 file:mr-4 file:rounded-md file:border-0 file:bg-blue-600 file:px-4 file:py-2 file:text-sm file:font-semibold file:text-white hover:file:bg-blue-700"
                  @change="handleFileChange"
                >
              </div>

              <div v-if="selectedFile" class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4 text-sm text-slate-600">
                <p class="font-semibold text-slate-700">当前文件</p>
                <p class="mt-1 break-all">{{ selectedFile.name }}</p>
                <p class="mt-1">大小：{{ formatFileSize(selectedFile.size) }}</p>
              </div>

              <div class="flex flex-wrap gap-3">
                <button
                  type="button"
                  :disabled="!selectedFile || preprocessing"
                  class="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
                  @click="runPreprocess"
                >
                  {{ preprocessing ? '渲染中...' : '执行浏览器端渲染' }}
                </button>
                <button
                  type="button"
                  :disabled="!processedResume || uploading"
                  class="rounded-xl bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white shadow-md shadow-blue-500/20 transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
                  @click="submitCandidateResume"
                >
                  {{ uploading ? '提交中...' : '提交预处理结果' }}
                </button>
              </div>

              <div v-if="preprocessProgress" class="rounded-2xl border border-indigo-100 bg-indigo-50/80 p-5 text-sm text-indigo-900">
                <div class="flex items-center justify-between gap-3">
                  <div>
                    <p class="font-semibold text-indigo-800">浏览器端渲染进度</p>
                    <p class="mt-1 text-xs text-indigo-700/80">{{ preprocessProgress.message }}</p>
                  </div>
                  <span class="rounded-full bg-white/80 px-3 py-1 text-xs font-semibold text-indigo-700">
                    {{ preprocessProgress.percentage }}%
                  </span>
                </div>
                <div class="mt-4 h-2 overflow-hidden rounded-full bg-indigo-100">
                  <div
                    class="h-full rounded-full bg-indigo-500 transition-all duration-300"
                    :style="{ width: `${preprocessProgress.percentage}%` }"
                  ></div>
                </div>
                <p v-if="preprocessProgress.totalPages" class="mt-3 text-xs text-indigo-700/80">
                  已完成 {{ preprocessProgress.completedPages }} / {{ preprocessProgress.totalPages }} 页
                </p>
              </div>

              <div v-if="processedResume" class="rounded-2xl border border-emerald-200 bg-emerald-50/80 p-5 text-sm text-emerald-900">
                <p class="font-semibold text-emerald-800">预处理结果已生成</p>
                <p class="mt-2 break-all"><span class="font-medium">轻量引用：</span>{{ processedResume.derivedReference }}</p>
                <p class="mt-2"><span class="font-medium">处理引擎：</span>{{ processedResume.engine }}</p>
                <p class="mt-2"><span class="font-medium">页面数量：</span>{{ processedResume.pageCountEstimate }}</p>
                <p class="mt-2"><span class="font-medium">上传批次：</span>{{ Math.min(processedResume.pagePreviews.length, uploadPreviewPageLimit) }} / {{ processedResume.pagePreviews.length }} 页</p>
                <p class="mt-2"><span class="font-medium">生成时间：</span>{{ formatDateTime(processedResume.generatedAt) }}</p>
                <p v-if="processedResume.extractedTextPreview" class="mt-2 leading-6 text-emerald-900/90">
                  <span class="font-medium">文本摘要：</span>{{ processedResume.extractedTextPreview }}
                </p>
                <ul class="mt-3 space-y-2 text-xs text-emerald-800/90">
                  <li v-for="warning in processedResume.warnings" :key="warning">{{ warning }}</li>
                </ul>
              </div>

              <div v-if="processedResume?.pagePreviews.length" class="space-y-4">
                <div class="flex items-center justify-between gap-3">
                  <h3 class="text-sm font-semibold text-slate-700">逐页预览</h3>
                  <span class="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-600">
                    共 {{ processedResume.pagePreviews.length }} 页
                  </span>
                </div>
                <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
                  <article
                    v-for="pagePreview in processedResume.pagePreviews"
                    :key="pagePreview.pageNumber"
                    class="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"
                  >
                    <div class="border-b border-slate-100 bg-slate-50 px-4 py-3 text-sm font-semibold text-slate-700">
                      第 {{ pagePreview.pageNumber }} 页
                    </div>
                    <div class="bg-slate-100 p-4">
                      <img
                        :src="pagePreview.imageDataUrl"
                        :alt="`第 ${pagePreview.pageNumber} 页预览`"
                        class="mx-auto rounded-lg border border-slate-200 bg-white shadow-sm"
                      >
                    </div>
                    <div class="px-4 py-3 text-xs leading-6 text-slate-600">
                      {{ pagePreview.textPreview || '该页未提取到明确文本摘要。' }}
                    </div>
                  </article>
                </div>
              </div>
            </div>
          </div>

          <div class="rounded-2xl border border-white/20 bg-white/70 p-8 shadow-sm backdrop-blur-md">
            <div class="mb-5 flex items-start justify-between gap-4">
              <div>
                <h2 class="text-xl font-bold text-slate-800">最新简历状态</h2>
                <p class="mt-1 text-sm text-slate-500">这里展示当前账号下最近一次简历记录，后续上传链路会直接复用这部分状态反馈。</p>
              </div>
              <span v-if="profile.latestResume" :class="getResumeStatusClass(profile.latestResume.status)" class="rounded-full px-3 py-1 text-xs font-semibold">
                {{ getResumeStatusLabel(profile.latestResume.status) }}
              </span>
            </div>

            <div v-if="profile.latestResume" class="space-y-4 text-sm text-slate-600">
              <div v-if="profile.latestResume.status === 'PENDING_PARSE'" class="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-amber-800">
                <p class="font-semibold">解析队列监控中</p>
                <p class="mt-1 text-sm">页面会自动刷新当前简历状态，无需手动重载。</p>
              </div>

              <div>
                <p class="text-xs font-semibold uppercase tracking-wider text-slate-400">简历引用</p>
                <p class="mt-1 break-all font-medium text-slate-800">{{ profile.latestResume.rawContentReference }}</p>
              </div>

              <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <p class="text-xs font-semibold uppercase tracking-wider text-slate-400">候选人姓名</p>
                  <p class="mt-1 font-medium text-slate-800">{{ getResumeDisplayName(profile.latestResume) }}</p>
                </div>
                <div>
                  <p class="text-xs font-semibold uppercase tracking-wider text-slate-400">联系方式</p>
                  <p class="mt-1 font-medium text-slate-800">{{ profile.latestResume.contactInfo || '未提供' }}</p>
                </div>
              </div>

              <div>
                <p class="text-xs font-semibold uppercase tracking-wider text-slate-400">最近更新时间</p>
                <p class="mt-1 font-medium text-slate-800">{{ formatDateTime(profile.latestResume.updatedAt) }}</p>
              </div>

              <div v-if="profile.latestResume.parseFailureReason" class="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-rose-700">
                <p class="font-semibold">解析失败原因</p>
                <p class="mt-1 text-sm">{{ profile.latestResume.parseFailureReason }}</p>
              </div>

              <div v-if="resumeSummary(profile.latestResume)" class="rounded-xl border border-slate-200 bg-slate-50/70 px-4 py-3">
                <p class="font-semibold text-slate-700">画像摘要</p>
                <p class="mt-1 text-sm leading-6 text-slate-600">{{ resumeSummary(profile.latestResume) }}</p>
              </div>

              <div v-if="resumeSkills(profile.latestResume).length > 0">
                <p class="mb-2 text-xs font-semibold uppercase tracking-wider text-slate-400">解析技能</p>
                <div class="flex flex-wrap gap-2">
                  <span v-for="skill in resumeSkills(profile.latestResume)" :key="skill" class="rounded-full border border-blue-200 bg-blue-50 px-3 py-1 text-xs font-medium text-blue-700">
                    {{ skill }}
                  </span>
                </div>
              </div>
            </div>

            <div v-else class="rounded-2xl border border-dashed border-slate-200 bg-slate-50/70 p-8 text-center text-slate-500">
              <div class="mb-3 text-4xl">📄</div>
              <p class="font-semibold text-slate-700">当前还没有候选人简历记录</p>
                <p class="mt-2 text-sm">上传并提交浏览器端预处理结果后，这里会展示最新解析状态。</p>
            </div>
          </div>

          <div class="rounded-2xl border border-white/20 bg-white/70 p-8 shadow-sm backdrop-blur-md">
            <h2 class="text-xl font-bold text-slate-800">下一步建议</h2>
            <ul class="mt-4 space-y-3 text-sm text-slate-600">
              <li>先完善 GitHub 和作品集链接，让后续多模态聚合有稳定入口。</li>
              <li>上传前先完成浏览器端 PDF 渲染，确认逐页预览内容与页数正常。</li>
              <li>提交后回看最新简历状态，确认解析已经进入队列再继续推荐与投递演示。</li>
            </ul>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import api from '@/utils/api';
import {
  buildBrowserResumeUploadPayload,
  MAX_BROWSER_UPLOAD_PREVIEW_PAGES,
  preprocessResumePdfInBrowser,
  type BrowserResumePreprocessProgress,
  type BrowserResumePreprocessResult,
} from '@/wasm/resumePreprocessor';

interface CandidateResumeSummary {
  resumeId: string;
  candidateName: string | null;
  contactInfo: string | null;
  rawContentReference: string;
  status: string;
  parseFailureReason: string | null;
  parsedData: Record<string, any> | null;
  updatedAt: string;
}

interface CandidateProfile {
  userId: string;
  username: string;
  email: string;
  githubUrl: string | null;
  portfolioUrl: string | null;
  latestResume: CandidateResumeSummary | null;
  updatedAt: string | null;
}

const router = useRouter();
const loading = ref(true);
const saving = ref(false);
const preprocessing = ref(false);
const uploading = ref(false);
const errorMsg = ref('');
const profile = ref<CandidateProfile | null>(null);
const feedback = ref<{ type: 'success' | 'error'; message: string } | null>(null);
const selectedFile = ref<File | null>(null);
const processedResume = ref<BrowserResumePreprocessResult | null>(null);
const preprocessProgress = ref<BrowserResumePreprocessProgress | null>(null);
const filePickerKey = ref(0);
const uploadPreviewPageLimit = MAX_BROWSER_UPLOAD_PREVIEW_PAGES;
const resumeStatusPollIntervalMs = 3000;
const form = ref({
  githubUrl: '',
  portfolioUrl: '',
});
let profilePollTimer: number | null = null;

const clearProfilePollTimer = () => {
  if (profilePollTimer !== null) {
    window.clearTimeout(profilePollTimer);
    profilePollTimer = null;
  }
};

const shouldPollLatestResume = (nextProfile: CandidateProfile | null) => {
  return nextProfile?.latestResume?.status === 'PENDING_PARSE';
};

const applyProfileResponse = (nextProfile: CandidateProfile | null, syncForm = true) => {
  profile.value = nextProfile;
  if (syncForm && nextProfile) {
    form.value.githubUrl = nextProfile.githubUrl || '';
    form.value.portfolioUrl = nextProfile.portfolioUrl || '';
  }

  clearProfilePollTimer();
  if (!shouldPollLatestResume(nextProfile)) {
    return;
  }

  profilePollTimer = window.setTimeout(async () => {
    try {
      const response: any = await api.get('/candidate/profile');
      applyProfileResponse(response.data, false);
    } catch (error) {
      console.error('Failed to refresh candidate profile status', error);
      applyProfileResponse(profile.value, false);
    }
  }, resumeStatusPollIntervalMs);
};

const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0] || null;

  selectedFile.value = file;
  processedResume.value = null;
  preprocessProgress.value = null;

  if (!file) {
    return;
  }

  const looksLikePdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');
  if (!looksLikePdf) {
    feedback.value = { type: 'error', message: '当前上传入口仅接受 PDF 文件。' };
    selectedFile.value = null;
    target.value = '';
    return;
  }

  feedback.value = null;
};

const fetchProfile = async () => {
  try {
    loading.value = true;
    errorMsg.value = '';
    feedback.value = null;
    const response: any = await api.get('/candidate/profile');
    applyProfileResponse(response.data);
  } catch (error) {
    console.error('Failed to fetch candidate profile', error);
    errorMsg.value = '请检查候选人登录状态或稍后重试。';
    profile.value = null;
  } finally {
    loading.value = false;
  }
};

const saveProfile = async () => {
  try {
    saving.value = true;
    feedback.value = null;
    const response: any = await api.put('/candidate/profile', {
      githubUrl: form.value.githubUrl || null,
      portfolioUrl: form.value.portfolioUrl || null,
    });
    applyProfileResponse(response.data);
    feedback.value = { type: 'success', message: '候选人资料已更新，推荐链路会使用最新资料。' };
  } catch (error: any) {
    console.error('Failed to save candidate profile', error);
    feedback.value = { type: 'error', message: error.response?.data?.message || '保存资料失败，请稍后重试。' };
  } finally {
    saving.value = false;
  }
};

const runPreprocess = async () => {
  if (!selectedFile.value) {
    feedback.value = { type: 'error', message: '请先选择 PDF 简历文件。' };
    return;
  }

  try {
    preprocessing.value = true;
    feedback.value = null;
    preprocessProgress.value = {
      stage: 'loading',
      completedPages: 0,
      totalPages: null,
      percentage: 0,
      currentPage: null,
      message: '准备开始浏览器端 PDF 渲染...',
    };
    processedResume.value = await preprocessResumePdfInBrowser(selectedFile.value, {
      onProgress: (progress) => {
        preprocessProgress.value = progress;
      },
    });
    feedback.value = { type: 'success', message: '浏览器端 PDF 渲染完成，可以提交轻量引用到候选人上传接口。' };
  } catch (error: any) {
    console.error('Failed to preprocess resume in browser', error);
    processedResume.value = null;
    preprocessProgress.value = null;
    feedback.value = { type: 'error', message: error.message || '浏览器端 PDF 渲染失败，请稍后重试。' };
  } finally {
    preprocessing.value = false;
  }
};

const submitCandidateResume = async () => {
  if (!processedResume.value) {
    feedback.value = { type: 'error', message: '请先执行浏览器端预处理，再提交上传结果。' };
    return;
  }

  try {
    uploading.value = true;
    feedback.value = null;
    await api.post('/resumes/upload', {
      candidateName: profile.value?.username || null,
      contactInfo: profile.value?.email || null,
      rawContentReference: processedResume.value.derivedReference,
      browserPreprocessedPayload: buildBrowserResumeUploadPayload(processedResume.value),
    });
    await fetchProfile();
    feedback.value = { type: 'success', message: '候选人简历已上传，并已自动进入解析队列。' };
    selectedFile.value = null;
    processedResume.value = null;
    preprocessProgress.value = null;
    filePickerKey.value += 1;
  } catch (error: any) {
    console.error('Failed to upload candidate resume', error);
    feedback.value = { type: 'error', message: error.response?.data?.message || '候选人简历上传失败，请稍后重试。' };
  } finally {
    uploading.value = false;
  }
};

const formatDateTime = (value: string) => new Date(value).toLocaleString('zh-CN', {
  year: 'numeric',
  month: 'short',
  day: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});

const formatFileSize = (bytes: number) => {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
};

const getResumeStatusLabel = (status: string) => {
  switch (status) {
    case 'PARSED':
      return '解析完成';
    case 'PARSING':
      return 'AI 解析中';
    case 'PENDING_PARSE':
      return '已入解析队列';
    case 'PARSE_FAILED':
      return '解析异常';
    default:
      return '待解析';
  }
};

const getResumeStatusClass = (status: string) => {
  switch (status) {
    case 'PARSED':
      return 'bg-emerald-100 text-emerald-800';
    case 'PARSING':
      return 'bg-blue-100 text-blue-800';
    case 'PENDING_PARSE':
      return 'bg-amber-100 text-amber-800';
    case 'PARSE_FAILED':
      return 'bg-rose-100 text-rose-800';
    default:
      return 'bg-amber-100 text-amber-800';
  }
};

const getResumeDisplayName = (resume: CandidateResumeSummary) => {
  return resume.candidateName
    || resume.parsedData?.basicInfo?.fullName
    || resume.parsedData?.candidateProfile?.name
    || '未识别候选人姓名';
};

const resumeSummary = (resume: CandidateResumeSummary) => {
  return resume.parsedData?.basicInfo?.summary
    || resume.parsedData?.candidateProfile?.summary
    || '';
};

const resumeSkills = (resume: CandidateResumeSummary) => {
  const rawSkills = resume.parsedData?.skills;
  if (!Array.isArray(rawSkills)) return [];
  return rawSkills
    .map((skill: any) => typeof skill === 'string' ? skill : skill?.name)
    .filter((skill: string | undefined | null) => Boolean(skill))
    .slice(0, 8);
};

onMounted(() => {
  fetchProfile();
});

onUnmounted(() => {
  clearProfilePollTimer();
});
</script>