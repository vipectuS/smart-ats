<template>
  <div class="min-h-screen bg-slate-50 p-6 lg:p-12">
    <div class="mx-auto max-w-6xl space-y-8">
      
      <!-- Header -->
      <header class="flex flex-col gap-4 sm:flex-row sm:items-center justify-between">
        <div>
          <h1 class="text-3xl font-extrabold text-slate-900 tracking-tight">我的档案</h1>
          <p class="mt-2 text-slate-500">完善基础资料与简历，以获取更精准的职位推荐。</p>
        </div>
        <div class="flex gap-3">
          <button
            @click="router.push({ name: 'candidateDashboard' })"
            class="rounded-xl border border-slate-200 bg-white px-4 py-2 font-medium text-slate-700 shadow-sm transition-colors hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-slate-200"
          >
            返回推荐页
          </button>
        </div>
      </header>

      <!-- Alerts -->
      <div v-if="feedback" :class="feedback.type === 'error' ? 'border-rose-200 bg-rose-50 text-rose-700' : 'border-emerald-200 bg-emerald-50 text-emerald-700'" class="rounded-xl border p-4 text-sm flex items-start gap-3 shadow-sm">
        <AlertCircle class="h-5 w-5 flex-shrink-0" />
        <span class="mt-0.5">{{ feedback.message }}</span>
      </div>

      <div v-if="loading" class="flex flex-col items-center justify-center rounded-xl border border-slate-200 bg-white p-16 text-slate-500 shadow-sm">
        <Loader2 class="mb-4 h-10 w-10 animate-spin text-blue-500" />
        <p class="font-medium">正在读取档案信息...</p>
      </div>

      <div v-else-if="errorMsg" class="flex flex-col items-center justify-center rounded-xl border border-rose-200 bg-rose-50 p-16 text-rose-700 shadow-sm">
        <AlertCircle class="mb-4 h-10 w-10" />
        <p class="text-lg font-bold">档案读取失败</p>
        <p class="mt-2 text-sm">{{ errorMsg }}</p>
        <button @click="fetchProfile" class="mt-6 rounded-lg bg-white border border-rose-200 px-6 py-2 font-medium text-rose-600 transition hover:bg-rose-100 shadow-sm">重试</button>
      </div>

      <template v-else-if="profile">
        <div class="grid grid-cols-1 gap-8 lg:grid-cols-12">
          
          <!-- Left Column: Basic Info & Resume Detail -->
          <div class="space-y-8 lg:col-span-8">
            <ProfileBasicInfo 
              :profile="profile" 
              @update="handleProfileUpdate" 
            />

            <!-- Separator -->
            <div class="flex items-center gap-4 py-2">
              <div class="h-px flex-1 bg-slate-200"></div>
              <span class="text-sm font-medium text-slate-400">已解析的结构化内容</span>
              <div class="h-px flex-1 bg-slate-200"></div>
            </div>

            <ProfileResumeDetail :resumeDetail="resume" />
          </div>

          <!-- Right Column: Resume Upload -->
          <div class="lg:col-span-4 h-full">
            <ProfileResumeUpload 
              :resumeDetail="resume" 
              @update="fetchProfile" 
            />
          </div>

        </div>
      </template>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { AlertCircle, Loader2 } from 'lucide-vue-next';
import api from '@/utils/api';
import { resolveApiError } from '@/utils/apiError';

import ProfileBasicInfo from '@/components/candidate/ProfileBasicInfo.vue';
import ProfileResumeUpload from '@/components/candidate/ProfileResumeUpload.vue';
import ProfileResumeDetail from '@/components/candidate/ProfileResumeDetail.vue';

const router = useRouter();

const loading = ref(true);
const errorMsg = ref('');
const profile = ref<any>(null);
const resume = ref<any>(null);

const feedback = ref<{ type: 'success' | 'error'; message: string; } | null>(null);

const fetchProfile = async () => {
  loading.value = true;
  errorMsg.value = '';
  feedback.value = null;

  try {
    const profileRes: any = await api.get('/candidate/profile');
    profile.value = profileRes.data;

     const latestResumeId = profile.value?.latestResume?.resumeId;
     if (latestResumeId) {
      try {
        const resumeRes: any = await api.get(`/candidate/resumes/${latestResumeId}`);
         resume.value = resumeRes.data;
      } catch (err: any) {
         console.error('Failed to load resume details:', err);
          feedback.value = { type: 'error', message: resolveApiError(err, '简历详情加载失败。').summary };
         resume.value = null;
      }
    } else {
      resume.value = null;
    }
  } catch (err: any) {
    errorMsg.value = resolveApiError(err, '无法获取个人资料 / Failed to fetch profile.').summary;
  } finally {
    loading.value = false;
  }
};

const handleProfileUpdate = (updatedProfile: any) => {
  profile.value = updatedProfile;
  feedback.value = {
    type: 'success',
    message: '基础资料更新成功！'
  };
  setTimeout(() => {
    feedback.value = null;
  }, 3000);
};

onMounted(() => {
  fetchProfile();
});
</script>
