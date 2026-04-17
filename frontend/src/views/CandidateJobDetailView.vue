<template>
  <div class="min-h-screen bg-gray-100 p-8">
    <div class="mx-auto max-w-6xl space-y-8">
      <div v-if="feedback" :class="feedback.type === 'error' ? 'border-rose-200 bg-rose-50 text-rose-700' : 'border-emerald-200 bg-emerald-50 text-emerald-700'" class="rounded-2xl border px-5 py-4 text-sm shadow-sm">
        {{ feedback.message }}
      </div>

      <div v-if="loading" class="rounded-2xl border border-white/20 bg-white/60 p-10 text-center text-gray-500 shadow-sm backdrop-blur-md">
        <i class="fas fa-spinner fa-spin mb-3 text-2xl text-blue-500"></i>
        <p>正在加载岗位详情...</p>
      </div>

      <div v-else-if="errorMsg" class="rounded-2xl border border-rose-200 bg-rose-50 p-6 text-center text-rose-700 shadow-sm">
        <p class="font-semibold">候选人岗位详情加载失败</p>
        <p class="mt-2 text-sm">{{ errorMsg }}</p>
        <button @click="reloadPage" class="mt-4 rounded-lg bg-rose-600 px-4 py-2 text-white transition hover:bg-rose-700">重试</button>
      </div>

      <template v-else-if="job">
        <header class="rounded-2xl border border-white/20 bg-white/50 p-8 shadow-sm backdrop-blur-md">
          <div class="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
            <div class="space-y-4">
              <button @click="router.push({ name: 'candidateDashboard' })" class="inline-flex items-center text-sm font-medium text-slate-500 transition hover:text-blue-600">
                <i class="fas fa-arrow-left mr-2"></i>
                返回推荐列表
              </button>
              <div>
                <h1 class="text-3xl font-bold text-slate-800">{{ job.title }}</h1>
                <p class="mt-2 text-sm text-slate-500">{{ jobMeta }}</p>
              </div>
              <p class="max-w-3xl text-sm leading-7 text-slate-600">{{ job.description }}</p>
            </div>

            <div class="min-w-[220px] rounded-2xl border border-blue-100 bg-blue-50/80 p-5 text-center shadow-sm">
              <p class="text-xs font-semibold uppercase tracking-[0.2em] text-blue-500">当前匹配度</p>
              <p class="mt-3 text-4xl font-black text-blue-700">{{ displayScore }}<span class="text-base font-semibold">%</span></p>
              <p class="mt-2 text-sm text-blue-700">{{ fitBandLabel }}</p>
            </div>
          </div>

          <div class="mt-6 flex flex-wrap gap-3 border-t border-slate-200 pt-6">
            <button
              @click="toggleApply"
              :disabled="applyActionDisabled"
              :class="applyButtonClass"
              class="rounded-xl px-5 py-3 text-sm font-semibold shadow-md shadow-blue-500/20 transition disabled:cursor-not-allowed disabled:opacity-70"
            >
              {{ applyButtonLabel }}
            </button>
            <button
              @click="toggleFavorite"
              :disabled="actionLoading || actionState.ignored"
              :class="actionState.favorited ? 'border-yellow-200 bg-yellow-50 text-yellow-700' : 'border-slate-200 bg-white text-slate-700'"
              class="rounded-xl border px-5 py-3 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-60"
            >
              {{ actionState.favorited ? '取消收藏' : '收藏岗位' }}
            </button>
            <button
              @click="toggleIgnore"
              :disabled="ignoreActionDisabled"
              :class="actionState.ignored ? 'border-rose-200 bg-rose-50 text-rose-700' : 'border-slate-200 bg-white text-slate-700'"
              class="rounded-xl border px-5 py-3 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-60"
            >
              {{ actionState.ignored ? '取消忽略' : '暂不感兴趣' }}
            </button>
            <button
              @click="refreshRecommendation"
              :disabled="reloadingRecommendation"
              class="rounded-xl border border-slate-200 bg-white px-5 py-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {{ reloadingRecommendation ? '更新中...' : '刷新个性化分析' }}
            </button>
          </div>
        </header>

        <div class="grid grid-cols-1 gap-8 xl:grid-cols-[1.1fr_0.9fr]">
          <section class="space-y-8">
            <div class="rounded-2xl border border-white/20 bg-white/70 p-8 shadow-sm backdrop-blur-md">
              <div class="flex items-center justify-between gap-4">
                <div>
                  <h2 class="text-xl font-bold text-slate-800">个性化匹配分析</h2>
                  <p class="mt-1 text-sm text-slate-500">基于当前候选人资料与最新简历生成的岗位适配解释。</p>
                </div>
                <span :class="fitBandClass" class="rounded-full px-3 py-1 text-xs font-semibold">{{ fitBandLabel }}</span>
              </div>

              <div v-if="recommendation" class="mt-6 space-y-6">
                <div class="rounded-2xl border border-slate-200 bg-slate-50/70 p-5">
                  <p class="text-sm font-semibold text-slate-700">AI 摘要</p>
                  <p class="mt-2 text-sm leading-7 text-slate-600">{{ recommendation.xaiReport.summary || recommendation.suitabilityReport }}</p>
                </div>

                <div class="grid grid-cols-1 gap-6 md:grid-cols-2">
                  <div>
                    <p class="mb-3 text-sm font-semibold text-slate-700">匹配优势</p>
                    <ul class="space-y-2 text-sm text-slate-600">
                      <li v-for="item in recommendation.xaiReport.strengths" :key="item" class="rounded-xl bg-emerald-50 px-4 py-3 text-emerald-800">{{ item }}</li>
                      <li v-if="recommendation.xaiReport.strengths.length === 0" class="rounded-xl bg-slate-50 px-4 py-3 text-slate-500">当前没有结构化优势说明。</li>
                    </ul>
                  </div>
                  <div>
                    <p class="mb-3 text-sm font-semibold text-slate-700">风险与差距</p>
                    <ul class="space-y-2 text-sm text-slate-600">
                      <li v-for="item in recommendation.xaiReport.risks" :key="item" class="rounded-xl bg-rose-50 px-4 py-3 text-rose-800">{{ item }}</li>
                      <li v-if="recommendation.xaiReport.risks.length === 0" class="rounded-xl bg-slate-50 px-4 py-3 text-slate-500">当前没有结构化风险说明。</li>
                    </ul>
                  </div>
                </div>

                <div>
                  <p class="mb-3 text-sm font-semibold text-slate-700">提升建议</p>
                  <div class="flex flex-wrap gap-2">
                    <span v-for="item in recommendation.xaiReport.improvementSuggestions" :key="item" class="rounded-full border border-amber-200 bg-amber-50 px-3 py-1 text-xs font-medium text-amber-800">
                      {{ item }}
                    </span>
                    <span v-if="recommendation.xaiReport.improvementSuggestions.length === 0" class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-medium text-slate-500">
                      暂无额外建议
                    </span>
                  </div>
                </div>

                <div class="rounded-2xl border border-blue-100 bg-blue-50/80 p-5">
                  <p class="text-sm font-semibold text-blue-800">完整分析</p>
                  <p class="mt-2 text-sm leading-7 text-blue-900/90">{{ recommendation.xaiReport.narrative || recommendation.suitabilityReport }}</p>
                </div>
              </div>

              <div v-else class="mt-6 rounded-2xl border border-dashed border-slate-200 bg-slate-50/70 p-8 text-center text-slate-500">
                <div v-if="analysisErrorMsg" class="mb-4 rounded-2xl border border-amber-200 bg-amber-50/80 p-4 text-left text-sm text-amber-900">
                  {{ analysisErrorMsg }}
                </div>
                <div class="mb-3 text-4xl">🧭</div>
                <p class="font-semibold text-slate-700">当前没有该岗位的个性化分析</p>
                <p class="mt-2 text-sm">可能是资料不完整，或者推荐结果尚未刷新。你可以先完善资料，再点击“刷新个性化分析”。</p>
              </div>
            </div>

            <div class="rounded-2xl border border-white/20 bg-white/70 p-8 shadow-sm backdrop-blur-md">
              <h2 class="text-xl font-bold text-slate-800">岗位要求</h2>
              <div class="mt-6 grid grid-cols-1 gap-6 md:grid-cols-2">
                <div>
                  <p class="mb-3 text-sm font-semibold text-slate-700">技能要求</p>
                  <div class="flex flex-wrap gap-2">
                    <span v-for="skill in requiredSkills" :key="skill" class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-medium text-slate-700">
                      {{ skill }}
                    </span>
                    <span v-if="requiredSkills.length === 0" class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-medium text-slate-500">未配置技能要求</span>
                  </div>
                </div>

                <div>
                  <p class="mb-3 text-sm font-semibold text-slate-700">候选人已匹配技能</p>
                  <div class="flex flex-wrap gap-2">
                    <span v-for="skill in matchedSkills" :key="skill" class="rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-xs font-medium text-emerald-700">
                      {{ skill }}
                    </span>
                    <span v-if="matchedSkills.length === 0" class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-medium text-slate-500">暂无匹配技能</span>
                  </div>
                </div>
              </div>

              <div class="mt-6">
                <p class="mb-3 text-sm font-semibold text-slate-700">仍需提升的技能</p>
                <div class="flex flex-wrap gap-2">
                  <span v-for="skill in missingSkills" :key="skill" class="rounded-full border border-rose-200 bg-rose-50 px-3 py-1 text-xs font-medium text-rose-700">
                    {{ skill }}
                  </span>
                  <span v-if="missingSkills.length === 0" class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-medium text-slate-500">当前没有明显缺口</span>
                </div>
              </div>
            </div>
          </section>

          <section class="space-y-8">
            <div class="rounded-2xl border border-white/20 bg-white/70 p-8 shadow-sm backdrop-blur-md">
              <h2 class="text-xl font-bold text-slate-800">岗位信息速览</h2>
              <dl class="mt-6 space-y-4 text-sm text-slate-600">
                <div>
                  <dt class="text-xs font-semibold uppercase tracking-wider text-slate-400">发布者</dt>
                  <dd class="mt-1 font-medium text-slate-800">{{ job.createdBy?.username || '未知发布者' }}</dd>
                </div>
                <div>
                  <dt class="text-xs font-semibold uppercase tracking-wider text-slate-400">地点</dt>
                  <dd class="mt-1 font-medium text-slate-800">{{ job.requirements?.location || '未设置' }}</dd>
                </div>
                <div>
                  <dt class="text-xs font-semibold uppercase tracking-wider text-slate-400">部门</dt>
                  <dd class="mt-1 font-medium text-slate-800">{{ job.requirements?.department || '未设置' }}</dd>
                </div>
                <div>
                  <dt class="text-xs font-semibold uppercase tracking-wider text-slate-400">需求人数</dt>
                  <dd class="mt-1 font-medium text-slate-800">{{ headcountLabel }}</dd>
                </div>
                <div>
                  <dt class="text-xs font-semibold uppercase tracking-wider text-slate-400">最近更新时间</dt>
                  <dd class="mt-1 font-medium text-slate-800">{{ formatDateTime(job.updatedAt) }}</dd>
                </div>
              </dl>
            </div>

            <div class="rounded-2xl border border-white/20 bg-white/70 p-8 shadow-sm backdrop-blur-md">
              <h2 class="text-xl font-bold text-slate-800">下一步行动</h2>
              <ul class="mt-4 space-y-3 text-sm text-slate-600">
                <li>如果匹配度偏低，先回到候选人资料页补齐 GitHub 和作品集链接。</li>
                <li>如果岗位很合适，直接投递并到时间线里验证动作是否记录成功。</li>
                <li>如果暂时不合适，可以收藏或忽略，后续再回来看推荐变化。</li>
              </ul>
            </div>
          </section>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import api from '@/utils/api';
import { notifyCandidateActivityUpdated } from '@/utils/candidateActivity';

interface StructuredJobFitReport {
  headline: string;
  fitBand: string;
  summary: string;
  strengths: string[];
  risks: string[];
  improvementSuggestions: string[];
  nextSteps: string[];
  narrative: string;
}

interface JobActionState {
  applied: boolean;
  favorited: boolean;
  ignored: boolean;
  applicationStatus?: string | null;
}

interface Recommendation {
  jobId: string;
  title: string;
  description: string;
  requirements: Record<string, any> | null;
  matchScore: number;
  semanticScore: number;
  suitabilityReport: string;
  xaiReport: StructuredJobFitReport;
  matchedSkills: string[];
  missingSkills: string[];
  actionState: JobActionState;
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
const recommendation = ref<Recommendation | null>(null);
const actionState = ref<JobActionState>({ applied: false, favorited: false, ignored: false });
const analysisErrorMsg = ref('');

const displayApplicationStatus = computed(() => actionState.value.applicationStatus || null);
const applyButtonLabel = computed(() => {
  if (actionLoading.value) return '处理中...';
  if (actionState.value.ignored) return '已忽略';
  if (displayApplicationStatus.value === 'INTERVIEW') return '已约面';
  if (displayApplicationStatus.value === 'REJECTED') return '已淘汰';
  return actionState.value.applied ? '撤回投递' : '立即投递';
});
const applyButtonClass = computed(() => {
  if (actionState.value.ignored) return 'bg-slate-100 text-slate-400';
  if (displayApplicationStatus.value === 'INTERVIEW') return 'bg-emerald-100 text-emerald-700';
  if (displayApplicationStatus.value === 'REJECTED') return 'bg-rose-100 text-rose-700';
  return actionState.value.applied ? 'bg-red-500 hover:bg-red-600 text-white' : 'bg-blue-600 hover:bg-blue-700 text-white';
});
const applyActionDisabled = computed(() => actionLoading.value || actionState.value.ignored || displayApplicationStatus.value === 'INTERVIEW' || displayApplicationStatus.value === 'REJECTED');
const ignoreActionDisabled = computed(() => actionLoading.value || actionState.value.applied || displayApplicationStatus.value === 'INTERVIEW' || displayApplicationStatus.value === 'REJECTED');

const requiredSkills = computed(() => Array.isArray(job.value?.requirements?.skills) ? job.value.requirements.skills : []);
const matchedSkills = computed(() => recommendation.value?.matchedSkills || []);
const missingSkills = computed(() => recommendation.value?.missingSkills || []);
const displayScore = computed(() => recommendation.value ? Math.round(recommendation.value.matchScore) : 0);
const fitBandLabel = computed(() => {
  switch (recommendation.value?.xaiReport?.fitBand) {
    case 'HIGH':
      return '高匹配度';
    case 'MEDIUM':
      return '中等匹配度';
    case 'LOW':
      return '低匹配度';
    default:
      return recommendation.value ? '待确认匹配度' : '尚未生成个性化分析';
  }
});
const fitBandClass = computed(() => {
  switch (recommendation.value?.xaiReport?.fitBand) {
    case 'HIGH':
      return 'bg-emerald-100 text-emerald-800';
    case 'MEDIUM':
      return 'bg-amber-100 text-amber-800';
    case 'LOW':
      return 'bg-rose-100 text-rose-800';
    default:
      return 'bg-slate-100 text-slate-700';
  }
});
const jobMeta = computed(() => {
  const requirements = job.value?.requirements || {};
  const department = requirements.department || (job.value?.createdBy?.username ? `发布者: ${job.value.createdBy.username}` : '未标注部门');
  const location = requirements.location || '地点未设置';
  return `${department} • ${location}`;
});
const headcountLabel = computed(() => {
  const headcount = job.value?.requirements?.headcount;
  if (headcount) return `${headcount} 人`;
  if (requiredSkills.value.length > 0) return `${requiredSkills.value.length} 项技能要求`;
  return '未设置';
});

const formatDateTime = (value: string) => new Date(value).toLocaleString('zh-CN', {
  year: 'numeric',
  month: 'short',
  day: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});

const getRecommendationFallbackMessage = (error: any) => {
  const message = error?.response?.data?.message || '';
  if (message.includes('upload and parse a resume before matching jobs')) {
    return '当前账号还没有可用于匹配的已解析简历。岗位基础信息仍可查看，待简历解析完成后再刷新个性化分析即可。';
  }
  if (message.includes('missing a valid talent profile')) {
    return '当前简历画像尚未准备完成，岗位基础信息仍可查看，可稍后重新刷新个性化分析。';
  }
  return '当前暂时无法生成个性化分析，但岗位基础信息仍可正常查看。';
};

const hydrateRecommendation = (item: any) => {
  if (!item) {
    recommendation.value = null;
    actionState.value = { applied: false, favorited: false, ignored: false, applicationStatus: null };
    return;
  }

  recommendation.value = {
    jobId: item.jobId,
    title: item.title,
    description: item.description,
    requirements: item.requirements,
    matchScore: Number(item.matchScore || 0),
    semanticScore: Number(item.semanticScore || 0),
    suitabilityReport: item.suitabilityReport || '',
    xaiReport: item.xaiReport || {
      headline: '',
      fitBand: 'UNKNOWN',
      summary: '',
      strengths: [],
      risks: [],
      improvementSuggestions: [],
      nextSteps: [],
      narrative: '',
    },
    matchedSkills: item.matchedSkills || [],
    missingSkills: item.missingSkills || [],
    actionState: item.actionState || { applied: false, favorited: false, ignored: false },
  };

  actionState.value = {
    applied: Boolean(item.actionState?.applied),
    favorited: Boolean(item.actionState?.favorited),
    ignored: Boolean(item.actionState?.ignored),
    applicationStatus: item.actionState?.applicationStatus || null,
  };
};

const fetchJob = async () => {
  const response: any = await api.get(`/jobs/${jobId}`);
  job.value = response.data;
};

const fetchRecommendation = async () => {
  const response: any = await api.post('/candidate/match-jobs');
  const items = response.data?.recommendations || [];
  const matchedJob = items.find((item: any) => String(item.jobId) === jobId);
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
    } catch (error) {
      console.error('Failed to load candidate recommendation', error);
      hydrateRecommendation(null);
      analysisErrorMsg.value = getRecommendationFallbackMessage(error);
    }
  } catch (error) {
    console.error('Failed to load candidate job detail', error);
    errorMsg.value = '无法读取岗位详情或个性化分析，请稍后重试。';
    job.value = null;
    recommendation.value = null;
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
    feedback.value = { type: 'success', message: recommendation.value ? '个性化分析已刷新。' : '已刷新岗位信息，但当前仍未生成该岗位的个性化分析。' };
  } catch (error) {
    console.error('Failed to refresh candidate recommendation', error);
    hydrateRecommendation(null);
    analysisErrorMsg.value = getRecommendationFallbackMessage(error);
    feedback.value = { type: 'error', message: analysisErrorMsg.value };
  } finally {
    reloadingRecommendation.value = false;
  }
};

const toggleApply = async () => {
  if (actionState.value.ignored || displayApplicationStatus.value === 'INTERVIEW' || displayApplicationStatus.value === 'REJECTED') {
    return;
  }
  try {
    actionLoading.value = true;
    feedback.value = null;
    if (actionState.value.applied) {
      await api.delete(`/jobs/${jobId}/apply`);
      actionState.value.applied = false;
      actionState.value.applicationStatus = 'WITHDRAWN';
      feedback.value = { type: 'success', message: `已撤回 ${job.value?.title || '当前岗位'} 的投递。` };
    } else {
      await api.post(`/jobs/${jobId}/apply`);
      actionState.value.applied = true;
      actionState.value.ignored = false;
      actionState.value.applicationStatus = 'APPLIED';
      feedback.value = { type: 'success', message: `已投递 ${job.value?.title || '当前岗位'}，可前往投递记录查看进度。` };
    }
    notifyCandidateActivityUpdated();
  } catch (error) {
    console.error('Failed to toggle apply state', error);
    feedback.value = { type: 'error', message: '更新投递状态失败，请稍后重试。' };
  } finally {
    actionLoading.value = false;
  }
};

const toggleFavorite = async () => {
  if (actionState.value.ignored) return;
  try {
    actionLoading.value = true;
    feedback.value = null;
    if (actionState.value.favorited) {
      await api.delete(`/jobs/${jobId}/favorite`);
      actionState.value.favorited = false;
      feedback.value = { type: 'success', message: `已取消收藏 ${job.value?.title || '当前岗位'}。` };
    } else {
      await api.post(`/jobs/${jobId}/favorite`);
      actionState.value.favorited = true;
      feedback.value = { type: 'success', message: `已收藏 ${job.value?.title || '当前岗位'}。` };
    }
    notifyCandidateActivityUpdated();
  } catch (error) {
    console.error('Failed to toggle favorite state', error);
    feedback.value = { type: 'error', message: '更新收藏状态失败，请稍后重试。' };
  } finally {
    actionLoading.value = false;
  }
};

const toggleIgnore = async () => {
  if (actionState.value.applied || displayApplicationStatus.value === 'INTERVIEW' || displayApplicationStatus.value === 'REJECTED') return;
  try {
    actionLoading.value = true;
    feedback.value = null;
    if (actionState.value.ignored) {
      await api.delete(`/jobs/${jobId}/ignore`);
      actionState.value.ignored = false;
      feedback.value = { type: 'success', message: `已恢复 ${job.value?.title || '当前岗位'} 的推荐展示。` };
    } else {
      await api.post(`/jobs/${jobId}/ignore`);
      actionState.value.ignored = true;
      actionState.value.favorited = false;
      feedback.value = { type: 'success', message: `已忽略 ${job.value?.title || '当前岗位'}。` };
    }
    notifyCandidateActivityUpdated();
  } catch (error) {
    console.error('Failed to toggle ignore state', error);
    feedback.value = { type: 'error', message: '更新忽略状态失败，请稍后重试。' };
  } finally {
    actionLoading.value = false;
  }
};

onMounted(() => {
  reloadPage();
});
</script>