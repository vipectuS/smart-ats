<template>
  <div class="h-full flex flex-col pt-4 px-6 space-y-6 overflow-hidden bg-slate-50">
    <!-- Header -->
    <div class="flex-shrink-0 flex flex-col bg-white p-6 rounded-2xl border border-slate-200 shadow-sm gap-4" v-if="job">
      <div class="flex justify-between items-center">
        <div>
          <div class="flex items-center gap-3 mb-1">
            <button @click="router.back()" class="text-slate-400 hover:text-blue-600 transition p-1">
              <ArrowLeft class="w-5 h-5" />
            </button>
            <h1 class="text-2xl font-bold text-slate-900">{{ job.title }}</h1>
          </div>
          <p class="text-sm text-slate-500 ml-9">{{ getJobMeta(job) }}</p>
        </div>
        <div class="flex items-center gap-3">
          <button @click="openEditPanel" class="px-4 py-2 border border-slate-200 bg-white text-slate-700 rounded-xl shadow-sm font-medium hover:bg-slate-50 transition flex items-center gap-2">
            <Pen class="w-4 h-4" /> 编辑岗位
          </button>
          <button @click="evaluateJob" :disabled="evaluating"
            class="px-4 py-2 bg-slate-800 text-white rounded-xl shadow-sm font-medium hover:bg-slate-900 transition flex items-center gap-2">
            <Loader2 v-if="evaluating" class="w-4 h-4 animate-spin" />
            <Wand2 v-else class="w-4 h-4" />
            {{ evaluating ? '评估中...' : 'AI 动态画像评估' }}
          </button>
        </div>
      </div>
      
      <!-- Interactive HR Weight Adjustments -->
      <div class="mt-2 pt-4 border-t border-slate-100 flex items-center gap-6 text-sm">
        <h3 class="font-bold text-slate-700 flex items-center gap-2 whitespace-nowrap">
          <SlidersHorizontal class="w-4 h-4 text-blue-500" /> 交互式画像权重雷达调整
        </h3>
        <div class="flex-1 grid grid-cols-1 md:grid-cols-4 gap-6">
          <div class="space-y-2">
            <div class="flex justify-between text-xs text-slate-600 font-medium"><span>技能 (Skill)</span><span class="text-blue-600">{{ weights.skillWeight }}%</span></div>
            <input type="range" v-model.number="weights.skillWeight" min="0" max="100" class="w-full h-1.5 bg-blue-100 rounded-lg appearance-none cursor-pointer accent-blue-600">
          </div>
          <div class="space-y-2">
            <div class="flex justify-between text-xs text-slate-600 font-medium"><span>经验 (Experience)</span><span class="text-indigo-600">{{ weights.experienceWeight }}%</span></div>
            <input type="range" v-model.number="weights.experienceWeight" min="0" max="100" class="w-full h-1.5 bg-indigo-100 rounded-lg appearance-none cursor-pointer accent-indigo-600">
          </div>
          <div class="space-y-2">
            <div class="flex justify-between text-xs text-slate-600 font-medium"><span>教育 (Education)</span><span class="text-purple-600">{{ weights.educationWeight }}%</span></div>
            <input type="range" v-model.number="weights.educationWeight" min="0" max="100" class="w-full h-1.5 bg-purple-100 rounded-lg appearance-none cursor-pointer accent-purple-600">
          </div>
          <div class="space-y-2">
            <div class="flex justify-between text-xs text-slate-600 font-medium"><span>非结构化语义深度</span><span class="text-emerald-600">{{ weights.semanticWeight }}%</span></div>
            <input type="range" v-model.number="weights.semanticWeight" min="0" max="100" class="w-full h-1.5 bg-emerald-100 rounded-lg appearance-none cursor-pointer accent-emerald-600">
          </div>
        </div>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-[minmax(0,1fr)_auto] gap-4 items-end">
        <label class="block">
          <span class="text-xs font-medium text-slate-600">本轮评估备注 / 实验目的</span>
          <textarea
            v-model.trim="evaluationNote"
            rows="2"
            maxlength="300"
            placeholder="例如：验证语义权重上调后，是否能提升 Top1 候选人稳定性"
            class="mt-2 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700 placeholder:text-slate-400 focus:border-slate-400 focus:outline-none focus:ring-2 focus:ring-slate-200 resize-none"
          />
        </label>
        <p class="text-xs text-slate-400 text-right">{{ evaluationNote.length }}/300</p>
      </div>
    </div>

    <!-- Scrollable Content -->
    <div class="flex-1 overflow-y-auto min-h-0 space-y-6 pb-6">
      <div v-if="pageFeedback" :class="pageFeedback.type === 'error' ? 'bg-rose-50 border-rose-200 text-rose-700' : 'bg-emerald-50 border-emerald-200 text-emerald-700'" class="rounded-2xl border px-5 py-4 text-sm shadow-sm flex items-start gap-2">
        <AlertCircle class="w-4 h-4 flex-shrink-0 mt-0.5" />
        {{ pageFeedback.message }}
      </div>

      <div v-if="errorMsg" class="bg-rose-50 border border-rose-200 rounded-2xl p-6 text-center text-rose-700 flex flex-col items-center">
        <p class="font-semibold mb-2">{{ errorMsg }}</p>
        <button @click="reloadPageData" class="px-4 py-2 rounded-lg border border-rose-200 bg-white text-sm hover:bg-rose-100 transition">重试</button>
      </div>

      <JobEvaluationHistory
        :items="evaluationHistory"
        :loading="historyLoading"
        :error="historyError"
        @apply-weights="applyHistoricalWeights"
      />

      <JobRecommendationList 
        :recommendations="recommendations" 
        :loading="loading" 
      />

      <JobApplicationReview 
        :applications="applications"
        :loading="applicationLoading"
        :error="applicationError"
        :recommendations="recommendations"
        :drafts="reviewDrafts"
        @retry="fetchApplications"
        @view-resume="openResumeDetail"
        @save-review="submitApplicationReview"
      />
    </div>

    <JobEditPanel 
      v-if="editPanelOpen"
      :job="job"
      :availableSkills="availableSkills"
      @close="closeEditPanel"
      @saved="handleJobSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowLeft, Pen, Wand2, SlidersHorizontal, AlertCircle, Loader2 } from 'lucide-vue-next';
import api from '@/utils/api';
import { resolveApiError } from '@/utils/apiError';
import JobEditPanel from '@/components/jobs/JobEditPanel.vue';
import JobEvaluationHistory from '@/components/jobs/JobEvaluationHistory.vue';
import JobRecommendationList from '@/components/jobs/JobRecommendationList.vue';
import JobApplicationReview from '@/components/jobs/JobApplicationReview.vue';
import type { PublicSkillCatalogItem } from '@/types/skills';

const route = useRoute();
const router = useRouter();
const jobId = route.params.id;

const job = ref<any>(null);
const recommendations = ref<any[]>([]);
const applications = ref<any[]>([]);
const evaluationHistory = ref<any[]>([]);
const loading = ref(true);
const applicationLoading = ref(false);
const historyLoading = ref(false);
const evaluating = ref(false);
const errorMsg = ref('');
const applicationError = ref('');
const historyError = ref('');
const pageFeedback = ref<{ type: 'success' | 'error'; message: string } | null>(null);
const editPanelOpen = ref(false);
const availableSkills = ref<PublicSkillCatalogItem[]>([]);
const reviewDrafts = reactive<Record<string, { status: string; reviewNote: string; saving: boolean; error: string }>>({});
const lastAppliedWeights = ref<{ skillWeight: number; experienceWeight: number; educationWeight: number; semanticWeight: number } | null>(null)
const evaluationNote = ref('')

const weights = ref({
  skillWeight: 40,
  experienceWeight: 30,
  educationWeight: 10,
  semanticWeight: 20
});

const normalizeWeightValue = (value: unknown) => Number(value || 0)

const cloneWeights = (value: { skillWeight: number; experienceWeight: number; educationWeight: number; semanticWeight: number }) => ({
  skillWeight: normalizeWeightValue(value.skillWeight),
  experienceWeight: normalizeWeightValue(value.experienceWeight),
  educationWeight: normalizeWeightValue(value.educationWeight),
  semanticWeight: normalizeWeightValue(value.semanticWeight),
})

const buildRecommendationSnapshot = (items: any[]) => {
  return Object.fromEntries(
    items.map((item, index) => [String(item.resumeId), { rank: index + 1, matchScore: Number(item.matchScore || 0) }]),
  )
}

const buildWeightShiftAnalysis = (
  recommendation: any,
  index: number,
  previousSnapshot: Record<string, { rank: number; matchScore: number }>,
  previousWeights: { skillWeight: number; experienceWeight: number; educationWeight: number; semanticWeight: number } | null,
  currentWeights: { skillWeight: number; experienceWeight: number; educationWeight: number; semanticWeight: number },
) => {
  if (!previousWeights) return null

  const previous = previousSnapshot[String(recommendation.resumeId)]
  if (!previous) return null

  const breakdown = recommendation.scoreBreakdown || {}
  const drivers = [
    { key: 'skillWeight', label: '技能', deltaWeight: currentWeights.skillWeight - previousWeights.skillWeight, score: Number(breakdown.skillScore || 0) },
    { key: 'experienceWeight', label: '经验', deltaWeight: currentWeights.experienceWeight - previousWeights.experienceWeight, score: Number(breakdown.experienceScore || 0) },
    { key: 'educationWeight', label: '教育', deltaWeight: currentWeights.educationWeight - previousWeights.educationWeight, score: Number(breakdown.educationScore || 0) },
    { key: 'semanticWeight', label: '语义', deltaWeight: currentWeights.semanticWeight - previousWeights.semanticWeight, score: Number(breakdown.semanticScore || 0) },
  ]
    .filter(item => item.deltaWeight !== 0)
    .map(item => ({
      ...item,
      estimatedImpact: Number(((item.deltaWeight * item.score) / 100).toFixed(2)),
    }))
    .sort((left, right) => Math.abs(right.estimatedImpact) - Math.abs(left.estimatedImpact))

  if (drivers.length === 0) return null

  const topDriver = drivers[0]
  const rankDelta = previous.rank - (index + 1)
  const scoreDelta = Number((Number(recommendation.matchScore || 0) - previous.matchScore).toFixed(2))
  const direction = scoreDelta > 0 ? '上升' : scoreDelta < 0 ? '下降' : '基本持平'
  const rankText = rankDelta > 0 ? `，名次上升 ${rankDelta} 位` : rankDelta < 0 ? `，名次下降 ${Math.abs(rankDelta)} 位` : '，名次未变化'
  const driverDirection = topDriver.estimatedImpact >= 0 ? '受益最大' : '受压最大'
  const summary = `本轮总分${direction} ${Math.abs(scoreDelta).toFixed(2)} 分${rankText}。主要原因是${topDriver.label}权重${topDriver.deltaWeight > 0 ? '上调' : '下调'} ${Math.abs(topDriver.deltaWeight)}%，而该候选人在该维度得分为 ${topDriver.score.toFixed(2)}，因此${driverDirection}。`

  return {
    scoreDelta,
    rankDelta,
    summary,
    drivers,
  }
}

const decorateRecommendations = (
  items: any[],
  previousSnapshot: Record<string, { rank: number; matchScore: number }>,
  previousWeights: { skillWeight: number; experienceWeight: number; educationWeight: number; semanticWeight: number } | null,
  currentWeights: { skillWeight: number; experienceWeight: number; educationWeight: number; semanticWeight: number } | null,
) => {
  return items.map((item, index) => ({
    ...item,
    weightShiftAnalysis: currentWeights ? buildWeightShiftAnalysis(item, index, previousSnapshot, previousWeights, currentWeights) : null,
  }))
}

const getJobMeta = (j: any) => {
  const reqs = j?.requirements || {};
  const root = reqs.department || (j?.createdBy?.username ? `发布者: ${j.createdBy.username}` : '未标注部门');
  return `${root} • ${reqs.location || '地点未设置'} • ${reqs.headcount ? reqs.headcount + ' 人' : '人数未设置'}`;
};

const openEditPanel = () => {
  if (job.value) editPanelOpen.value = true;
};

const closeEditPanel = () => {
  editPanelOpen.value = false;
};

const handleJobSaved = (updatedJob: any) => {
  job.value = updatedJob;
  closeEditPanel();
  pageFeedback.value = { type: 'success', message: '岗位信息已更新，建议重新执行一次 AI 评估以刷新推荐结果。' };
  setTimeout(() => pageFeedback.value = null, 3000);
};

const openResumeDetail = (resumeId: string) => {
  router.push({ name: 'resumeDetail', params: { id: resumeId } });
};

const syncReviewDrafts = (items: any[]) => {
  const nextIds = new Set(items.map(i => String(i.applicationId)));
  for (const existingId in reviewDrafts) {
    if (!nextIds.has(existingId)) delete reviewDrafts[existingId];
  }
  for (const item of items) {
    const id = String(item.applicationId);
    reviewDrafts[id] = {
      status: item.status || 'APPLIED',
      reviewNote: item.reviewNote || '',
      saving: reviewDrafts[id]?.saving || false,
      error: '',
    };
  }
};

const submitApplicationReview = async (app: any) => {
  const id = String(app.applicationId);
  const draft = reviewDrafts[id];
  if (!draft) return;
  
  try {
    draft.saving = true; draft.error = ''; pageFeedback.value = null;
    const res: any = await api.put(`/jobs/${jobId}/applications/${id}`, {
      status: draft.status,
      reviewNote: draft.reviewNote.trim() || null,
    });
    applications.value = applications.value.map(item => String(item.applicationId) === id ? res.data : item);
    syncReviewDrafts(applications.value);
    pageFeedback.value = { type: 'success', message: '候选人审核状态已更新。' };
    setTimeout(() => pageFeedback.value = null, 3000);
  } catch (error: any) {
    console.error('Failed to review application', error);
    draft.error = resolveApiError(error, '更新失败，请重试。').summary;
  } finally {
    draft.saving = false;
  }
};

const fetchJob = async () => {
  try {
    const res: any = await api.get(`/jobs/${jobId}`);
    job.value = res.data;
  } catch (error) {
    errorMsg.value = resolveApiError(error, '无法读取岗位详情，请检查岗位是否存在。').summary;
    job.value = null;
  }
};

const fetchRecommendations = async () => {
  loading.value = true; errorMsg.value = '';
  try {
    const res: any = await api.get(`/jobs/${jobId}/recommendations`);
    recommendations.value = decorateRecommendations(res.data || [], {}, null, null);
  } catch (error) {
    errorMsg.value = resolveApiError(error, '无法读取推荐结果，请稍后重试。').summary;
    recommendations.value = [];
  } finally {
    loading.value = false;
  }
};

const fetchApplications = async () => {
  applicationLoading.value = true; applicationError.value = '';
  try {
    const res: any = await api.get(`/jobs/${jobId}/applications`);
    applications.value = res.data || [];
    syncReviewDrafts(applications.value);
  } catch (error) {
    applicationError.value = resolveApiError(error, '无法读取当前岗位的投递记录，请稍后重试。').summary;
    applications.value = [];
  } finally {
    applicationLoading.value = false;
  }
};

const fetchEvaluationHistory = async () => {
  historyLoading.value = true; historyError.value = '';
  try {
    const res: any = await api.get(`/jobs/${jobId}/evaluations`);
    evaluationHistory.value = res.data || [];
    const latestWeights = evaluationHistory.value[0]?.appliedWeights ? cloneWeights(evaluationHistory.value[0].appliedWeights) : null
    if (latestWeights) {
      weights.value = latestWeights
      lastAppliedWeights.value = latestWeights
    }
  } catch (error) {
    historyError.value = resolveApiError(error, '无法读取评估历史，请稍后重试。').summary;
    evaluationHistory.value = [];
  } finally {
    historyLoading.value = false;
  }
};

const applyHistoricalWeights = (appliedWeights: { skillWeight: number; experienceWeight: number; educationWeight: number; semanticWeight: number }) => {
  weights.value = cloneWeights(appliedWeights)
  pageFeedback.value = { type: 'success', message: '已回填历史评估权重，可直接重新执行 AI 评估。' }
  setTimeout(() => pageFeedback.value = null, 3000)
}

const evaluateJob = async () => {
  evaluating.value = true; pageFeedback.value = null;
  try {
    const previousSnapshot = buildRecommendationSnapshot(recommendations.value)
    const previousWeights = lastAppliedWeights.value ? cloneWeights(lastAppliedWeights.value) : null
    const res: any = await api.post(`/jobs/${jobId}/evaluate`, {
      ...weights.value,
      evaluationNote: evaluationNote.value || null,
    });
    const evaluation = res.data || {}
    const persistedPreviousWeights = evaluation.previousEvaluation?.appliedWeights
      ? cloneWeights(evaluation.previousEvaluation.appliedWeights)
      : null
    const appliedWeights = cloneWeights(evaluation.appliedWeights || weights.value)
    weights.value = appliedWeights
    lastAppliedWeights.value = appliedWeights
    recommendations.value = decorateRecommendations(
      evaluation.recommendations || [],
      previousSnapshot,
      previousWeights || persistedPreviousWeights,
      appliedWeights,
    )
    if (evaluation.currentEvaluation) {
      evaluationHistory.value = [
        evaluation.currentEvaluation,
        ...evaluationHistory.value.filter((item) => item.evaluationId !== evaluation.currentEvaluation.evaluationId),
      ]
    }
    evaluationNote.value = ''
    pageFeedback.value = {
      type: 'success',
      message: (previousWeights || persistedPreviousWeights)
        ? 'AI 评估已完成，推荐列表已更新，并已生成本轮调权影响解释。'
        : 'AI 评估已完成，推荐列表已更新。再次调权后可看到分数变化来源。'
    };
    setTimeout(() => pageFeedback.value = null, 3000);
  } catch (error) {
    errorMsg.value = resolveApiError(error, '评估请求失败，请稍后重试。').summary;
  } finally {
    evaluating.value = false;
  }
};

const reloadPageData = async () => {
  errorMsg.value = ''; applicationError.value = ''; historyError.value = ''; pageFeedback.value = null;
  await Promise.all([fetchJob(), fetchRecommendations(), fetchApplications(), fetchEvaluationHistory()]);
};

onMounted(() => {
  reloadPageData();
  api.get('/skills/catalog').then((res) => {
    availableSkills.value = res.data || [];
  }).catch(async () => {
    try {
      const res: any = await api.get('/skills');
      availableSkills.value = (res.data || []).map((name: string) => ({ name, aliases: [] }));
    } catch {
      availableSkills.value = [];
    }
  });
});
</script>
