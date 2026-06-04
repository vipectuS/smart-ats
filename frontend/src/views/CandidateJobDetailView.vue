<template>
  <div class="min-h-full flex flex-col space-y-6 bg-slate-50 px-6 pt-4">
    <div v-if="feedback" :class="feedback.type === 'error' ? 'border-rose-200 bg-rose-50 text-rose-700' : 'border-emerald-200 bg-emerald-50 text-emerald-700'" class="rounded-2xl border px-5 py-4 text-sm shadow-sm flex items-start gap-2">
      <AlertCircle class="w-4 h-4 flex-shrink-0 mt-0.5" />
      {{ feedback.message }}
    </div>

    <div v-if="loading" class="rounded-2xl border border-slate-200 bg-white p-10 text-center text-slate-500 shadow-sm flex flex-col items-center justify-center min-h-[50vh]">
      <Loader2 class="w-8 h-8 animate-spin mb-4 text-blue-500" />
      <p class="text-lg font-medium text-slate-700">正在加载岗位详情...</p>
      <p class="mt-2 text-sm text-slate-400 max-w-sm">如果需要同步生成真实模型 分析，首次加载可能会持续 30 到 90 秒，请耐心等待。</p>
    </div>

    <div v-else-if="errorMsg" class="rounded-2xl border border-rose-200 bg-rose-50 p-8 text-center text-rose-700 shadow-sm min-h-[40vh] gap-3 flex flex-col justify-center items-center">
      <p class="font-bold text-xl">候选人岗位详情加载失败</p>
      <p class="text-sm mt-1 mb-4 opacity-80">{{ errorMsg }}</p>
      <button @click="reloadPage" class="rounded-xl bg-rose-600 px-6 py-2.5 text-sm font-semibold text-white transition hover:bg-rose-700 shadow-sm">重试加载</button>
    </div>

    <template v-else-if="job">
      <!-- 固定的头部区域 -->
      <div class="flex-shrink-0 bg-white p-6 rounded-2xl border border-slate-200 shadow-sm flex flex-col lg:flex-row gap-6 justify-between lg:items-start">
        <div class="space-y-4 flex-1">
          <button @click="router.push({ name: 'candidateDashboard' })" class="inline-flex items-center text-sm font-medium text-slate-500 transition hover:text-blue-600 gap-1.5 focus:outline-none">
            <ArrowLeft class="w-4 h-4" /> 返回推荐列表
          </button>
          <div>
            <h1 class="text-3xl font-bold text-slate-900">{{ job.title }}</h1>
            <p class="mt-2 text-sm text-slate-500 font-medium">{{ jobMeta }}</p>
          </div>
          <p class="max-w-3xl text-sm leading-7 text-slate-600 pt-2 border-t border-slate-100">{{ cleanedJobDescription }}</p>
        </div>

        <div class="flex flex-col gap-4 flex-shrink-0 w-full lg:w-72">
          <div class="rounded-2xl border border-blue-100 bg-blue-50 p-5 text-center flex flex-col justify-center items-center shadow-sm">
            <p class="text-xs font-bold uppercase tracking-[0.2em] text-blue-500">当前匹配度</p>
            <p class="mt-2 text-[2.5rem] leading-none font-black text-blue-700">{{ displayScore }}<span class="text-lg font-semibold ml-1">%</span></p>
            <p class="mt-1 text-sm font-medium text-blue-700">{{ fitBandLabel }}</p>
          </div>
          
          <div class="flex flex-col gap-3">
             <button
                @click="toggleApply"
                :disabled="applyActionDisabled"
                class="w-full rounded-xl px-5 py-3 text-sm font-semibold transition shadow-sm disabled:cursor-not-allowed disabled:opacity-70 flex justify-center items-center gap-2"
                :class="applyButtonClass"
              >
                <Briefcase class="w-4 h-4" />
                {{ applyButtonLabel }}
             </button>
             <div class="grid grid-cols-2 gap-3">
               <button
                @click="toggleFavorite"
                :disabled="actionLoading || actionState.ignored"
                :class="actionState.favorited ? 'border-yellow-300 bg-yellow-50 text-yellow-700' : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'"
                class="rounded-xl border px-3 py-2 text-sm font-medium transition disabled:cursor-not-allowed disabled:opacity-60 flex justify-center items-center gap-1.5"
              >
                <Bookmark class="w-4 h-4" />
                {{ actionState.favorited ? '已收藏' : '收藏' }}
              </button>
              <button
                @click="toggleIgnore"
                :disabled="ignoreActionDisabled"
                :class="actionState.ignored ? 'border-rose-300 bg-rose-50 text-rose-700' : 'border-slate-200 bg-white text-slate-700 hover:bg-rose-50 hover:text-rose-600 hover:border-rose-200'"
                class="rounded-xl border px-3 py-2 text-sm font-medium transition disabled:cursor-not-allowed disabled:opacity-60 flex justify-center items-center gap-1.5"
              >
                <Ban class="w-4 h-4" />
                {{ actionState.ignored ? '已忽略' : '忽略' }}
              </button>
             </div>
             <button
              @click="refreshRecommendation"
              :disabled="reloadingRecommendation"
              class="w-full rounded-xl border border-slate-200 bg-white px-5 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60 flex justify-center items-center gap-2 shadow-sm"
            >
               <RefreshCw class="w-4 h-4" :class="{'animate-spin text-blue-500': reloadingRecommendation}" />
              {{ reloadingRecommendation ? '重新生成画像中...' : '刷新个性化模型画像' }}
            </button>
          </div>
        </div>
      </div>

      <div class="space-y-6 pb-8">
        <div class="grid grid-cols-1 gap-6 xl:grid-cols-[1.1fr_0.9fr]">
           <CandidateMatchAnalysis 
             :recommendation="recommendation"
             :fitBandLabel="fitBandLabel"
             :fitBandClass="fitBandClass"
             :errorMsg="analysisErrorMsg"
           />
           <CandidateJobRequirements 
             :requiredSkills="requiredSkills"
             :matchedSkills="matchedSkills"
             :missingSkills="missingSkills"
           />
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowLeft, Loader2, AlertCircle, Bookmark, Briefcase, Ban, RefreshCw } from 'lucide-vue-next';
import api from '@/utils/api';
import { resolveApiError } from '@/utils/apiError';
import { notifyCandidateActivityUpdated } from '@/utils/candidateActivity';
import CandidateMatchAnalysis from '@/components/candidate/CandidateMatchAnalysis.vue';
import CandidateJobRequirements from '@/components/candidate/CandidateJobRequirements.vue';

interface JobActionState {
  applied: boolean;
  favorited: boolean;
  ignored: boolean;
  applicationStatus?: string | null;
}

const route = useRoute();
const router = useRouter();
const jobId = String(route.params.id);

const loading = ref(true);
const reloadingRecommendation = ref(false);
const actionLoading = ref(false);
const errorMsg = ref('');
const feedback = ref<{ type: 'success' | 'error'; message: string } | null>(null);
const job = ref<any>(null);
const recommendation = ref<any>(null);
const actionState = ref<JobActionState>({ applied: false, favorited: false, ignored: false });
const analysisErrorMsg = ref('');
const MATCH_JOBS_TIMEOUT_MS = 180000;

const displayApplicationStatus = computed(() => actionState.value.applicationStatus || null);

const applyButtonLabel = computed(() => {
  if (actionLoading.value) return '处理中...';
  if (actionState.value.ignored) return '已忽略';
  if (displayApplicationStatus.value === 'INTERVIEW') return '简历已通过，正 在约面';
  if (displayApplicationStatus.value === 'REJECTED') return '不匹配撤销';
  return actionState.value.applied ? '撤回申请 (已投)' : '立即一键投递';
});

const applyButtonClass = computed(() => {
  if (actionState.value.ignored) return 'bg-slate-200 text-slate-500 border border-slate-300';
  if (displayApplicationStatus.value === 'INTERVIEW') return 'bg-emerald-600 text-white shadow-emerald-500/30';
  if (displayApplicationStatus.value === 'REJECTED') return 'bg-slate-800 text-white line-through opacity-80';
  return actionState.value.applied ? 'bg-rose-500 hover:bg-rose-600 text-white' : 'bg-blue-600 hover:bg-blue-700 text-white shadow-blue-500/30';
});

const applyActionDisabled = computed(() => actionLoading.value || actionState.value.ignored || displayApplicationStatus.value === 'INTERVIEW' || displayApplicationStatus.value === 'REJECTED');
const ignoreActionDisabled = computed(() => actionLoading.value || actionState.value.applied || displayApplicationStatus.value === 'INTERVIEW' || displayApplicationStatus.value === 'REJECTED');

const requiredSkills = computed(() => Array.isArray(job.value?.requirements?.skills) ? job.value.requirements.skills : []);
const matchedSkills = computed(() => recommendation.value?.matchedSkills || []);
const missingSkills = computed(() => recommendation.value?.missingSkills || []);
const displayScore = computed(() => recommendation.value ? Math.round(recommendation.value.matchScore) : 0);

const fitBandLabel = computed(() => {
  switch (recommendation.value?.xaiReport?.fitBand) {
    case 'HIGH': return '极度匹配 (S级)';
    case 'MEDIUM': return '较好匹配 (A级)';
    case 'LOW': return '边缘匹配 (B级)';
    default: return recommendation.value ? '待确认' : '未生成画像';
  }
});

const fitBandClass = computed(() => {
  switch (recommendation.value?.xaiReport?.fitBand) {
    case 'HIGH': return 'bg-emerald-100 text-emerald-800';
    case 'MEDIUM': return 'bg-blue-100 text-blue-800';
    case 'LOW': return 'bg-amber-100 text-amber-800';
    default: return 'bg-slate-100 text-slate-700';
  }
});

const cleanedJobDescription = computed(() => {
  const rawDescription = String(job.value?.description || '').trim();
  return rawDescription.replace(/\n*\[Dataset\][\s\S]*$/i, '').trim();
});

const jobMeta = computed(() => {
  const reqs = job.value?.requirements || {};
  const department = reqs.department || job.value?.organization?.name || (job.value?.createdBy?.username ? `发布者: ${job.value.createdBy.username}` : '未标注部门');
  const location = reqs.location || '地点不限';
  const hc = reqs.headcount ? `${reqs.headcount} 人` : '招满即止';
  return `${department} • ${location} • ${hc}`;
});

const getRecommendationFallbackMessage = (err: any) => {
  const resolvedError = resolveApiError(err, '抱歉，当前无法获取完整的 AI 个性化分析报告。这不影响您的浏览。');
  const msg = resolvedError.message || '';
  if (err?.code === 'ECONNABORTED' || String(err?.message || '').toLowerCase().includes('timeout')) {
    return '多模态 AI 分析耗时较长（冷启动时需数十秒），请耐心稍候刷新重试 。';
  }
  if (msg.includes('upload and parse a resume')) {
    return '系统尚未查找到您的有效简历解析，故而暂时无法输出智能匹配画像。这不影响您正常投递简历。';
  }
  return resolvedError.summary;
};

const hydrateRecommendation = (item: any) => {
  if (!item) {
    recommendation.value = null;
    actionState.value = { applied: false, favorited: false, ignored: false, applicationStatus: null };
    return;
  }
  recommendation.value = {
    ...item,
    xaiReport: item.xaiReport || { fitBand: 'UNKNOWN', headline: '', summary: '', strengths: [], risks: [], improvementSuggestions: [], nextSteps: [], narrative: '' },
    matchedSkills: item.matchedSkills || [],
    missingSkills: item.missingSkills || [],
  };
  actionState.value = {
    applied: Boolean(item.actionState?.applied),
    favorited: Boolean(item.actionState?.favorited),
    ignored: Boolean(item.actionState?.ignored),
    applicationStatus: item.actionState?.applicationStatus || null,
  };
};

const fetchJob = async () => {
  const res: any = await api.get(`/jobs/${jobId}`);
  job.value = res.data;
};

const fetchRecommendation = async () => {
  const res: any = await api.post('/candidate/match-jobs', undefined, { timeout: MATCH_JOBS_TIMEOUT_MS });
  const items = res.data?.recommendations || [];
  const matchedJob = items.find((i: any) => String(i.jobId) === jobId);
  hydrateRecommendation(matchedJob || null);
};

const reloadPage = async () => {
  try {
    loading.value = true;
    errorMsg.value = '';
    feedback.value = null;
    analysisErrorMsg.value = '';
    await fetchJob();
    try {
      await fetchRecommendation();
    } catch (err) {
      console.error(err);
      hydrateRecommendation(null);
      analysisErrorMsg.value = getRecommendationFallbackMessage(err);
    }
  } catch (err) {
    console.error(err);
    errorMsg.value = resolveApiError(err, '抓取岗位数据失败或该岗位已下线。请检查网络。').summary;
  } finally {
    loading.value = false;
  }
};

const refreshRecommendation = async () => {
  try {
    reloadingRecommendation.value = true;
    feedback.value = null;
    analysisErrorMsg.value = '';
    await fetchRecommendation();
    feedback.value = { type: 'success', message: recommendation.value ? '已通 过最新大模型链路重新刷新您与该岗位的适配度。' : '已重新抓取，但由于数据缺失未能生成成功。' };
    setTimeout(() => { feedback.value = null }, 4000);
  } catch (err) {
    console.error(err);
    hydrateRecommendation(null);
    analysisErrorMsg.value = getRecommendationFallbackMessage(err);
    feedback.value = { type: 'error', message: analysisErrorMsg.value };
  } finally {
    reloadingRecommendation.value = false;
  }
};

const toggleApply = async () => {
  if (actionState.value.ignored || displayApplicationStatus.value === 'INTERVIEW' || displayApplicationStatus.value === 'REJECTED') return;
  try {
    actionLoading.value = true;
    feedback.value = null;
    if (actionState.value.applied) {
      await api.delete(`/jobs/${jobId}/apply`);
      actionState.value.applied = false;
      actionState.value.applicationStatus = 'WITHDRAWN';
      feedback.value = { type: 'success', message: '已撤回投递' };
    } else {
      await api.post(`/jobs/${jobId}/apply`);
      actionState.value.applied = true;
      actionState.value.ignored = false;
      actionState.value.applicationStatus = 'APPLIED';
      feedback.value = { type: 'success', message: '已投递该岗位。HR团队将通 过本系统收到推送！' };
    }
    notifyCandidateActivityUpdated();
  } catch (error) {
    feedback.value = { type: 'error', message: resolveApiError(error, '操作失败，请重试。').summary };
  } finally {
    actionLoading.value = false;
  }
};

// ... toggleFavorite and toggleIgnore mirror logic ...
const toggleFavorite = async () => {
  if (actionState.value.ignored) return;
  try {
    actionLoading.value = true;
    if (actionState.value.favorited) {
      await api.delete(`/jobs/${jobId}/favorite`);
      actionState.value.favorited = false;
    } else {
      await api.post(`/jobs/${jobId}/favorite`);
      actionState.value.favorited = true;
    }
    notifyCandidateActivityUpdated();
  } finally { actionLoading.value = false; }
};

const toggleIgnore = async () => {
  if (actionState.value.applied || displayApplicationStatus.value === 'INTERVIEW' || displayApplicationStatus.value === 'REJECTED') return;
  try {
    actionLoading.value = true;
    if (actionState.value.ignored) {
      await api.delete(`/jobs/${jobId}/ignore`);
      actionState.value.ignored = false;
    } else {
      await api.post(`/jobs/${jobId}/ignore`);
      actionState.value.ignored = true;
      actionState.value.favorited = false;
    }
    notifyCandidateActivityUpdated();
  } finally { actionLoading.value = false; }
};

onMounted(() => reloadPage());
</script>
