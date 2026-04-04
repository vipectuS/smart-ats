<template>
  <div class="min-h-screen bg-gray-100 p-8">
    <div class="max-w-7xl mx-auto space-y-8">
      <!-- Welcome Header -->
      <header class="bg-white/50 backdrop-blur-md rounded-2xl p-8 shadow-sm border border-white/20">
        <h1 class="text-3xl font-bold text-gray-800">欢迎回来，候选人 👋</h1>
        <p class="text-gray-600 mt-2">这是根据您的简历为您推荐的专属职位，匹配度基于 AI 分析。</p>
      </header>

      <!-- Main Content and Sidebar -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        <!-- Recommended Jobs List (Left Content) -->
        <div class="lg:col-span-2">
          <h2 class="text-xl font-bold text-gray-800 mb-6 px-1">推荐职位</h2>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div v-for="job in recommendedJobs" :key="job.id" 
                 class="bg-white/50 backdrop-blur-md rounded-2xl p-6 shadow-sm border border-white/20 hover:shadow-md transition-shadow flex flex-col h-full">
          
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
            <button @click="applyJob(job)" 
                    :disabled="job.status === JobApplicationStatus.IGNORE"
                    :class="[
                      'flex-1 py-2 rounded-lg font-medium transition-colors',
                      job.status === JobApplicationStatus.APPLIED ? 'bg-red-500 text-white hover:bg-red-600' : 
                      job.status === JobApplicationStatus.IGNORE ? 'bg-gray-100 text-gray-400 cursor-not-allowed' :
                      'bg-blue-600 text-white hover:bg-blue-700'
                    ]">
              {{ job.status === JobApplicationStatus.APPLIED ? '撤回投递' : '🚀 一键投递' }}
            </button>
            <button @click="toggleFavorite(job)"
                    :disabled="job.status === JobApplicationStatus.IGNORE"
                    :class="[
                      'px-4 py-2 rounded-lg border transition-colors',
                      job.isFavorite ? 'bg-yellow-50 border-yellow-200 text-yellow-600' : 'bg-white border-gray-200 hover:bg-gray-50 text-gray-600',
                      job.status === JobApplicationStatus.IGNORE ? 'opacity-50 cursor-not-allowed' : ''
                    ]">
              {{ job.isFavorite ? '⭐️ 已收藏' : '🌟 收藏' }}
            </button>
            <button @click="ignoreJob(job)"
                    :disabled="job.status === JobApplicationStatus.APPLIED"
                    :class="[
                      'px-4 py-2 rounded-lg border transition-colors',
                      job.status === JobApplicationStatus.IGNORE ? 'bg-red-50 border-red-200 text-red-600' : 'bg-white border-gray-200 hover:bg-red-50 hover:text-red-600 hover:border-red-200',
                      job.status === JobApplicationStatus.APPLIED ? 'opacity-50 cursor-not-allowed' : ''
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
import CandidateTimeline from '@/components/CandidateTimeline.vue';
import { JobApplicationStatus } from '@/types/CandidateActions';

import api from '@/utils/api';

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
  isFavorite: boolean;
}

const recommendedJobs = ref<Job[]>([]);

const fetchRecommendations = async () => {
  try {
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
      status: item.actionState?.applied ? JobApplicationStatus.APPLIED : 
              item.actionState?.ignored ? JobApplicationStatus.IGNORE : 
              JobApplicationStatus.NONE,
      isFavorite: item.actionState?.favorited || false
    }));
  } catch (error) {
    console.error('Failed to fetch recommendations:', error);
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

const applyJob = async (job: Job) => {
  if (job.status === JobApplicationStatus.IGNORE) return;
  try {
    if (job.status === JobApplicationStatus.APPLIED) {
      await api.delete(`/jobs/${job.id}/apply`);
      job.status = JobApplicationStatus.NONE;
    } else {
      await api.post(`/jobs/${job.id}/apply`);
      job.status = JobApplicationStatus.APPLIED;
    }
  } catch (error) {
    console.error('Failed to apply/withdraw:', error);
  }
};

const toggleFavorite = async (job: Job) => {
  if (job.status === JobApplicationStatus.IGNORE) return;
  try {
    if (job.isFavorite) {
      await api.delete(`/jobs/${job.id}/favorite`);
      job.isFavorite = false;
    } else {
      await api.post(`/jobs/${job.id}/favorite`);
      job.isFavorite = true;
    }
  } catch (error) {
    console.error('Failed to toggle favorite:', error);
  }
};

const ignoreJob = async (job: Job) => {
  if (job.status === JobApplicationStatus.APPLIED) return;
  try {
    if (job.status === JobApplicationStatus.IGNORE) {
      await api.delete(`/jobs/${job.id}/ignore`);
      job.status = JobApplicationStatus.NONE;
    } else {
      await api.post(`/jobs/${job.id}/ignore`);
      job.status = JobApplicationStatus.IGNORE;
      job.isFavorite = false;
    }
  } catch (error) {
    console.error('Failed to ignore job:', error);
  }
};
</script>
