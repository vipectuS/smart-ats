<template>
  <div class="space-y-6">
    <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-slate-800">候选人工作台</h1>
          <p class="mt-1 text-sm text-slate-500">欢迎回来，这是基于您最新简历提取的智能岗位推荐。</p>
        </div>
        <button
          @click="refreshRecommendations"
          :disabled="loading"
          class="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 shadow-sm transition hover:bg-slate-50 disabled:opacity-70"
        >
          <RefreshCw class="w-4 h-4" :class="{'animate-spin text-blue-500': loading}" />
          {{ loading ? '刷新中' : '刷新推荐' }}
        </button>
      </div>
    </div>

    <!-- Error/Info States -->
    <div v-if="errorMsg" class="rounded-xl border border-rose-200 bg-rose-50 p-6 text-center text-rose-700 shadow-sm">
      <AlertCircle class="w-8 h-8 mx-auto mb-3 text-rose-400" />
      <p class="font-semibold text-lg">{{ errorMsg }}</p>
      <p class="mt-1 text-sm opacity-90">{{ secondaryErrorMsg }}</p>
    </div>
    
    <div v-if="loading" class="rounded-2xl border border-slate-200 bg-white p-16 text-center shadow-sm flex flex-col items-center justify-center min-h-[300px]">
      <Loader2 class="w-8 h-8 text-blue-500 animate-spin mb-4" />
      <p class="text-slate-600 font-medium">正在生成个性化匹配推荐...</p>
      <p class="mt-2 text-sm text-slate-400">大模型正在对您的简历特征进行多维度交叉匹配，请稍候</p>
    </div>

    <div v-else-if="!errorMsg && recommendations.length === 0" class="rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-16 text-center shadow-sm">
      <Compass class="w-12 h-12 mx-auto mb-4 text-slate-300" />
      <p class="text-lg font-semibold text-slate-700">当前没有匹配的推荐岗位</p>
      <p class="mt-2 text-sm text-slate-500">可能是简历解析未完成，或者暂无合适的在招岗位，请稍后刷新重试。</p>
    </div>

    <!-- Recommendations Grid -->
    <div v-else-if="!loading && !errorMsg" class="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
      <article v-for="rec in recommendations" :key="rec.jobId" class="flex flex-col rounded-2xl border border-slate-200 bg-white p-6 shadow-sm transition hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-start justify-between gap-4">
          <div class="min-w-0 flex-1">
            <h3 class="truncate text-lg font-bold text-slate-800" :title="rec.title">{{ rec.title }}</h3>
            <p class="mt-1 truncate text-sm text-slate-500">{{ getJobMeta(rec) }}</p>
          </div>
          <div class="flex flex-col items-end">
            <span class="rounded-full bg-blue-50 px-2.5 py-1 text-xs font-bold text-blue-700 whitespace-nowrap">匹配度 {{ Math.round(rec.matchScore) }}%</span>
            <span class="mt-1 text-[0.65rem] font-medium uppercase text-slate-400">{{ rec.xaiReport?.fitBand || 'UNKNOWN' }}</span>
          </div>
        </div>

        <div class="mt-4 flex-1">
           <p class="text-xs font-semibold text-slate-600 line-clamp-3 leading-relaxed opacity-90">{{ rec.xaiReport?.summary || rec.suitabilityReport || '暂无总结' }}</p>
        </div>

        <!-- 增加动作状态标签 -->
        <div class="mt-4 border-t border-slate-100 pt-4 flex items-center justify-between">
          <div class="flex items-center gap-2">
            <span v-if="rec.actionState?.applied" class="inline-flex items-center gap-1 rounded bg-blue-50 px-2 py-0.5 text-xs font-medium text-blue-700">
               <CheckCircle class="w-3.5 h-3.5" /> 已投递
            </span>
            <span v-if="rec.actionState?.favorited" class="inline-flex items-center gap-1 rounded border border-yellow-200 bg-yellow-50 px-2 py-0.5 text-xs font-medium text-yellow-700">
               <Bookmark class="w-3.5 h-3.5" /> 已收藏
            </span>
          </div>
          <button @click="viewDetail(rec.jobId)" class="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 hover:border-slate-300">
            查看详情
          </button>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { RefreshCw, AlertCircle, Loader2, Compass, CheckCircle, Bookmark } from 'lucide-vue-next';
import api from '@/utils/api';
import { resolveApiError } from '@/utils/apiError';

const router = useRouter();
const recommendations = ref<any[]>([]);
const loading = ref(true);
const errorMsg = ref('');
const secondaryErrorMsg = ref('');
const MATCH_JOBS_TIMEOUT_MS = 180000;

const getJobMeta = (rec: any) => {
  const reqs = rec.requirements || {};
  const org = rec.organization?.name || '未知部门';
  const loc = reqs.location || '地点未限';
  return `${org} • ${loc}`;
};

const fetchRecommendations = async () => {
  try {
    loading.value = true;
    errorMsg.value = '';
    secondaryErrorMsg.value = '';
    const res: any = await api.post('/candidate/match-jobs', undefined, { timeout: MATCH_JOBS_TIMEOUT_MS });
    const items = res.data?.recommendations || [];
    recommendations.value = items.filter((item: any) => !item.actionState?.ignored);
  } catch (error: any) {
    console.error('Failed to fetch recommendations', error);
    const resolvedError = resolveApiError(error, '未知错误，服务器可能离线。');
    const msg = resolvedError.message;
    if (error?.code === 'ECONNABORTED' || String(error?.message || '').toLowerCase().includes('timeout')) {
      errorMsg.value = '请求超时';
      secondaryErrorMsg.value = '冷启动期间大模型推理耗时较长，请再次重试。';
    } else if (resolvedError.code === 'CANDIDATE_RESUME_REQUIRED' || msg.includes('upload and parse a resume')) {
      errorMsg.value = '尚未解析到有效简历';
      secondaryErrorMsg.value = resolvedError.userHint || '系统必须拥有一份已提取核心特征的智能简历才能生成结构化诊断报告。请到「我的档案」页面上传并解析您的简历。';
    } else {
      errorMsg.value = '获取岗位匹配推荐失败';
      secondaryErrorMsg.value = resolvedError.summary;
    }
  } finally {
    loading.value = false;
  }
};

const refreshRecommendations = () => {
  fetchRecommendations();
};

const viewDetail = (jobId: string) => {
  router.push({ name: 'candidateJobDetail', params: { id: jobId } });
};

onMounted(() => {
  fetchRecommendations();
});
</script>
