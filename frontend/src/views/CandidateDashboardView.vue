<template>
  <div class="min-h-screen bg-gray-100 p-8">
    <div class="max-w-7xl mx-auto space-y-8">
      <!-- Welcome Header -->
      <header class="bg-white/50 backdrop-blur-md rounded-2xl p-8 shadow-sm border border-white/20">
        <h1 class="text-3xl font-bold text-gray-800">欢迎回来，候选人 👋</h1>
        <p class="text-gray-600 mt-2">这是根据您的简历为您推荐的专属职位，匹配度基于 AI 分析。</p>
      </header>

      <div v-if="actionFeedback" :class="actionFeedback.type === 'error' ? 'border-rose-200 bg-rose-50 text-rose-700' : 'border-emerald-200 bg-emerald-50 text-emerald-700'" class="rounded-2xl border px-5 py-4 text-sm shadow-sm">
        {{ actionFeedback.message }}
      </div>

      <!-- Main Content and Sidebar -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        <!-- Recommended Jobs List (Left Content) -->
        <div class="lg:col-span-2">
          <h2 class="text-xl font-bold text-gray-800 mb-6 px-1">推荐职位</h2>
          <div v-if="loading" class="bg-white/60 backdrop-blur-md rounded-2xl border border-white/20 p-10 text-center text-gray-500">
            <i class="fas fa-spinner fa-spin text-2xl text-blue-500 mb-3"></i>
            <p>正在生成推荐结果...</p>
          </div>
          <div v-else-if="errorMsg" class="bg-rose-50 border border-rose-200 rounded-2xl p-6 text-center text-rose-700">
            <p class="font-semibold">推荐结果加载失败</p>
            <p class="text-sm mt-2">{{ errorMsg }}</p>
            <button @click="fetchRecommendations" class="mt-4 px-4 py-2 rounded-lg bg-rose-600 text-white hover:bg-rose-700 transition">重试</button>
          </div>
          <div v-else-if="recommendedJobs.length === 0" class="bg-white/60 backdrop-blur-md rounded-2xl border border-white/20 p-10 text-center text-gray-500">
            <div class="text-4xl mb-3">📭</div>
            <p>当前还没有可展示的推荐结果。</p>
            <p class="text-sm mt-2">请先完善候选人资料并上传简历，再重新生成匹配结果。</p>
            <button @click="router.push({ name: 'candidateProfile' })" class="mt-4 rounded-lg bg-blue-600 px-4 py-2 text-white transition hover:bg-blue-700">前往完善资料</button>
          </div>
          <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div v-for="job in recommendedJobs" :key="job.id" 
                @click="openJobDetail(job.id)"
                class="bg-white/50 backdrop-blur-md rounded-2xl p-6 shadow-sm border border-white/20 hover:shadow-md transition-shadow flex flex-col h-full cursor-pointer">
          
          <div class="flex justify-between items-start mb-4">
            <div>
              <h2 class="text-xl font-bold text-gray-800">{{ job.title }}</h2>
              <p class="text-gray-500 text-sm">{{ job.company }} · {{ job.location }}</p>
            </div>
            <div class="flex flex-col items-end">
              <span class="text-2xl font-black" :class="getScoreColor(job.matchScore)">{{ job.matchScore }}<span class="text-sm font-normal">%</span></span>
              <span class="text-xs text-gray-400">匹配度</span>
            </div>
          </div>

          <div class="flex-1 space-y-4">
            <!-- XAI Reasoning -->
            <div class="bg-gray-50/50 rounded-lg p-3 text-sm text-gray-700">
              <p><strong>💡 AI 分析：</strong> {{ job.reasoning }}</p>
            </div>

            <!-- Missing Skills -->
            <div v-if="job.missingSkills.length > 0">
              <p class="text-xs font-semibold text-gray-500 mb-2">需要提升的技能：</p>
              <div class="flex flex-wrap gap-2">
                <span v-for="skill in job.missingSkills" :key="skill" 
                      class="px-2 py-1 bg-red-100 text-red-700 rounded-md text-xs font-medium border border-red-200">
                  {{ skill }}
                </span>
              </div>
            </div>

            <!-- Suggestions -->
            <div v-if="job.suggestions.length > 0">
              <p class="text-xs font-semibold text-gray-500 mb-2">学习建议：</p>
              <ul class="list-disc list-inside text-sm text-gray-600 space-y-1">
                <li v-for="suggestion in job.suggestions" :key="suggestion">{{ suggestion }}</li>
              </ul>
            </div>
          </div>

          <!-- Actions -->
          <div class="mt-6 pt-4 border-t border-gray-200/50 flex gap-3">
            <button @click.stop="applyJob(job)" 
                    :disabled="isApplyDisabled(job)"
                    :class="[
                      'flex-1 py-2 rounded-lg font-medium transition-colors',
                      getApplyButtonClass(job),
                      actionJobId === job.id ? 'opacity-70 cursor-wait' : ''
                    ]">
              {{ getApplyButtonLabel(job) }}
            </button>
                <button @click.stop="toggleFavorite(job)"
                    :disabled="job.status === JobApplicationStatus.IGNORE || actionJobId === job.id"
                    :class="[
                      'px-4 py-2 rounded-lg border transition-colors',
                      job.isFavorite ? 'bg-yellow-50 border-yellow-200 text-yellow-600' : 'bg-white border-gray-200 hover:bg-gray-50 text-gray-600',
                      job.status === JobApplicationStatus.IGNORE ? 'opacity-50 cursor-not-allowed' : '',
                      actionJobId === job.id ? 'opacity-70 cursor-wait' : ''
                    ]">
              {{ job.isFavorite ? '⭐️ 已收藏' : '🌟 收藏' }}
            </button>
                <button @click.stop="ignoreJob(job)"
                    :disabled="isIgnoreDisabled(job)"
                    :class="[
                      'px-4 py-2 rounded-lg border transition-colors',
                      job.status === JobApplicationStatus.IGNORE ? 'bg-red-50 border-red-200 text-red-600' : 'bg-white border-gray-200 hover:bg-red-50 hover:text-red-600 hover:border-red-200',
                      isIgnoreDisabled(job) ? 'opacity-50 cursor-not-allowed' : '',
                      actionJobId === job.id ? 'opacity-70 cursor-wait' : ''
                    ]">
              {{ job.status === JobApplicationStatus.IGNORE ? '已忽略' : '💔 不感兴趣' }}
            </button>
          </div>
        </div>
          </div>
        </div>

        <!-- Sidebar (Timeline) -->
        <div class="lg:col-span-1">
          <CandidateTimeline />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">

import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import CandidateTimeline from '@/components/CandidateTimeline.vue';
import { JobApplicationStatus } from '@/types/CandidateActions';

import api from '@/utils/api';
import { notifyCandidateActivityUpdated } from '@/utils/candidateActivity';

interface Job {
  id: string;
  title: string;
  company: string;
  location: string;
  matchScore: number;
  reasoning: string;
  missingSkills: string[];
  suggestions: string[];
  status: JobApplicationStatus;
  applicationStatus: string | null;
  isFavorite: boolean;
}

const getDisplayApplicationStatus = (actionState: any): JobApplicationStatus => {
  const applicationStatus = actionState?.applicationStatus;
  if (applicationStatus === JobApplicationStatus.INTERVIEW) return JobApplicationStatus.INTERVIEW;
  if (applicationStatus === JobApplicationStatus.REJECTED) return JobApplicationStatus.REJECTED;
  if (actionState?.applied) return JobApplicationStatus.APPLIED;
  if (actionState?.ignored) return JobApplicationStatus.IGNORE;
  return JobApplicationStatus.NONE;
};

const getApplyButtonLabel = (job: Job) => {
  if (actionJobId.value === job.id) return '处理中...';
  if (job.status === JobApplicationStatus.APPLIED) return '撤回投递';
  if (job.status === JobApplicationStatus.INTERVIEW) return '已约面';
  if (job.status === JobApplicationStatus.REJECTED) return '已淘汰';
  if (job.status === JobApplicationStatus.IGNORE) return '已忽略';
  return '🚀 一键投递';
};

const isApplyDisabled = (job: Job) => (
  actionJobId.value === job.id
  || job.status === JobApplicationStatus.IGNORE
  || job.status === JobApplicationStatus.INTERVIEW
  || job.status === JobApplicationStatus.REJECTED
);

const getApplyButtonClass = (job: Job) => {
  if (job.status === JobApplicationStatus.APPLIED) return 'bg-red-500 text-white hover:bg-red-600';
  if (job.status === JobApplicationStatus.INTERVIEW) return 'bg-emerald-100 text-emerald-700 cursor-not-allowed';
  if (job.status === JobApplicationStatus.REJECTED) return 'bg-rose-100 text-rose-700 cursor-not-allowed';
  if (job.status === JobApplicationStatus.IGNORE) return 'bg-gray-100 text-gray-400 cursor-not-allowed';
  return 'bg-blue-600 text-white hover:bg-blue-700';
};

const isIgnoreDisabled = (job: Job) => (
  actionJobId.value === job.id
  || job.status === JobApplicationStatus.APPLIED
  || job.status === JobApplicationStatus.INTERVIEW
  || job.status === JobApplicationStatus.REJECTED
);

const recommendedJobs = ref<Job[]>([]);
const loading = ref(true);
const errorMsg = ref('');
const actionJobId = ref<string | null>(null);
const actionFeedback = ref<{ type: 'success' | 'error'; message: string } | null>(null);
const router = useRouter();

const getRecommendationErrorMessage = (error: any) => {
  const message = error?.response?.data?.message || '';
  if (message.includes('upload and parse a resume before matching jobs')) {
    return '请先前往“我的资料”上传简历，并等待解析完成后再生成推荐结果。';
  }
  if (message.includes('missing a valid talent profile')) {
    return '当前简历画像尚未准备完成，请回到资料页确认解析结果。';
  }
  return '请确认候选人资料和推荐接口状态。';
};

const fetchRecommendations = async () => {
  try {
    loading.value = true;
    errorMsg.value = '';
    actionFeedback.value = null;
    const response: any = await api.post('/candidate/match-jobs');
    const recommendations = response.data?.recommendations || [];
    
    recommendedJobs.value = recommendations.map((item: any) => ({
      id: item.jobId,
      title: item.title,
      company: item.requirements?.company || '未知公司',
      location: item.requirements?.location || '未知地点',
      matchScore: Math.round(item.matchScore),
      reasoning: (item.xaiReport?.narrative || item.xaiReport?.summary || item.suitabilityReport || '无').substring(0, 150),
      missingSkills: item.missingSkills || [],
      suggestions: item.xaiReport?.improvementSuggestions || [],
            status: getDisplayApplicationStatus(item.actionState),
            applicationStatus: item.actionState?.applicationStatus || null,
      isFavorite: item.actionState?.favorited || false
    }));
  } catch (error) {
    console.error('Failed to fetch recommendations:', error);
    errorMsg.value = getRecommendationErrorMessage(error);
    recommendedJobs.value = [];
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchRecommendations();
});

const getScoreColor = (score: number) => {
  if (score >= 90) return 'text-green-500';
  if (score >= 80) return 'text-blue-500';
  return 'text-orange-500';
};

const openJobDetail = (jobId: string) => {
  router.push({ name: 'candidateJobDetail', params: { id: jobId } });
};

const applyJob = async (job: Job) => {
  if (job.status === JobApplicationStatus.IGNORE || job.status === JobApplicationStatus.INTERVIEW || job.status === JobApplicationStatus.REJECTED) return;
  try {
    actionJobId.value = job.id;
    actionFeedback.value = null;
    if (job.status === JobApplicationStatus.APPLIED) {
      await api.delete(`/jobs/${job.id}/apply`);
      job.status = JobApplicationStatus.NONE;
      job.applicationStatus = JobApplicationStatus.WITHDRAWN;
      actionFeedback.value = { type: 'success', message: `已撤回 ${job.title} 的投递。` };
    } else {
      await api.post(`/jobs/${job.id}/apply`);
      job.status = JobApplicationStatus.APPLIED;
      job.applicationStatus = JobApplicationStatus.APPLIED;
      actionFeedback.value = { type: 'success', message: `已投递 ${job.title}，可前往投递记录查看进度。` };
    }
    notifyCandidateActivityUpdated();
  } catch (error) {
    console.error('Failed to apply/withdraw:', error);
    actionFeedback.value = { type: 'error', message: `无法更新 ${job.title} 的投递状态，请稍后重试。` };
  } finally {
    actionJobId.value = null;
  }
};

const toggleFavorite = async (job: Job) => {
  if (job.status === JobApplicationStatus.IGNORE) return;
  try {
    actionJobId.value = job.id;
    actionFeedback.value = null;
    if (job.isFavorite) {
      await api.delete(`/jobs/${job.id}/favorite`);
      job.isFavorite = false;
      actionFeedback.value = { type: 'success', message: `已取消收藏 ${job.title}。` };
    } else {
      await api.post(`/jobs/${job.id}/favorite`);
      job.isFavorite = true;
      actionFeedback.value = { type: 'success', message: `已收藏 ${job.title}，后续可在候选人侧扩展收藏列表。` };
    }
    notifyCandidateActivityUpdated();
  } catch (error) {
    console.error('Failed to toggle favorite:', error);
    actionFeedback.value = { type: 'error', message: `无法更新 ${job.title} 的收藏状态，请稍后重试。` };
  } finally {
    actionJobId.value = null;
  }
};

const ignoreJob = async (job: Job) => {
  if (job.status === JobApplicationStatus.APPLIED || job.status === JobApplicationStatus.INTERVIEW || job.status === JobApplicationStatus.REJECTED) return;
  try {
    actionJobId.value = job.id;
    actionFeedback.value = null;
    if (job.status === JobApplicationStatus.IGNORE) {
      await api.delete(`/jobs/${job.id}/ignore`);
      job.status = JobApplicationStatus.NONE;
      actionFeedback.value = { type: 'success', message: `已恢复 ${job.title} 的推荐展示。` };
    } else {
      await api.post(`/jobs/${job.id}/ignore`);
      job.status = JobApplicationStatus.IGNORE;
      job.isFavorite = false;
      actionFeedback.value = { type: 'success', message: `已忽略 ${job.title}，它将暂时不再出现在推荐结果中。` };
    }
    notifyCandidateActivityUpdated();
  } catch (error) {
    console.error('Failed to ignore job:', error);
    actionFeedback.value = { type: 'error', message: `无法更新 ${job.title} 的忽略状态，请稍后重试。` };
  } finally {
    actionJobId.value = null;
  }
};
</script>
