<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { Users, Briefcase, FileText, Loader2, AlertCircle } from 'lucide-vue-next';
import api from '@/utils/api';
import { resolveApiError } from '@/utils/apiError';

const authStore = useAuthStore();
const router = useRouter();
const loading = ref(true);
const errorMsg = ref('');
const stats = ref<Array<{title: string, value: string | number, icon: any}>>([]);

const goToCreateJob = () => {
  router.push('/jobs?create=1');
};

onMounted(async () => {
  loading.value = true;
  errorMsg.value = '';
  try {
    const res = await api.get('/hr/dashboard/stats');
    const data = res.data;
    stats.value = [
      { title: '收到简历', value: data.keyMetrics?.totalResumes || 0, icon: FileText },
      { title: '解析入库', value: data.keyMetrics?.parsedResumes || 0, icon: Users },
      { title: '进入面试', value: data.keyMetrics?.interviewCount || 0, icon: Briefcase },
    ];
  } catch (err: any) {
    errorMsg.value = resolveApiError(err, '获取看板数据失败').summary;
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold tracking-tight text-slate-900">欢迎回来，{{ authStore.user?.username }}</h1>
      </div>
      <div>
        <button
          @click="goToCreateJob"
          class="bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-lg font-medium shadow-sm transition-colors text-sm"
        >
          发布新职位
        </button>
      </div>
    </div>

    <div v-if="loading" class="flex justify-center items-center py-12">
      <Loader2 class="w-8 h-8 animate-spin text-indigo-600" />
      <span class="ml-2 text-slate-500">正在加载数据...</span>
    </div>

    <div v-else-if="errorMsg" class="bg-rose-50 text-rose-700 p-4 rounded-xl border border-rose-200 flex items-center">
      <AlertCircle class="w-5 h-5 mr-2" />
      {{ errorMsg }}
    </div>

    <div v-else-if="stats.length === 0" class="flex justify-center items-center py-12 text-slate-500">
      无数据
    </div>

    <!-- Quick Stats Blocks -->
    <div v-else class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <div 
        v-for="stat in stats" 
        :key="stat.title"
        class="bg-white rounded-xl shadow-sm border border-slate-200 p-6 flex flex-col justify-between"
      >
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-indigo-50 flex items-center justify-center text-indigo-600">
            <component :is="stat.icon" class="w-5 h-5" />
          </div>
          <h2 class="text-sm font-medium text-slate-500">{{ stat.title }}</h2>
        </div>
        <div class="mt-4">
          <span class="text-3xl font-bold text-slate-900">{{ stat.value }}</span>
        </div>
      </div>
    </div>

  </div>
</template>
