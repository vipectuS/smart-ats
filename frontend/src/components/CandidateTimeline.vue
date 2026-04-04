<template>
  <div class="bg-white/50 backdrop-blur-md rounded-2xl p-6 shadow-sm border border-white/20 h-full">
    <h2 class="text-xl font-bold text-gray-800 mb-6 px-1">活动时间线</h2>
    <div v-if="loading" class="text-gray-500 text-sm py-4 flex items-center justify-center">
      <i class="fas fa-spinner fa-spin mr-2"></i> 加载中...
    </div>
    <div v-else-if="errorMsg" class="text-red-500 text-sm py-4">
      {{ errorMsg }}
    </div>
    <div v-else-if="events.length === 0" class="text-gray-500 text-sm italic py-4">
      暂无活动记录
    </div>
    <div v-else class="relative border-l-2 border-gray-200 ml-3 space-y-6">
      <div v-for="event in events" :key="event.id" class="relative pl-6">
        <!-- Timeline dot -->
        <span 
          class="absolute -left-[9px] top-1 h-4 w-4 rounded-full border-2 border-white"
          :class="getActionColor(event.action)"
        ></span>
        
        <!-- Content -->
        <div class="flex flex-col">
          <span class="text-sm font-semibold text-gray-800">
            {{ getActionLabel(event.action) }}
          </span>
          <span class="text-sm text-gray-600 mt-1">
            {{ event.jobTitle }} - {{ event.companyName }}
          </span>
          <span class="text-xs text-gray-400 mt-1">
            {{ formatTime(event.timestamp) }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import api from '@/utils/api';

interface TimelineEvent {
  id: string;
  action: 'APPLIED' | 'WITHDRAWN' | 'FAVORITED' | 'UNFAVORITED' | 'IGNORED' | 'UNIGNORED';
  jobTitle: string;
  companyName: string;
  timestamp: string;
}

const events = ref<TimelineEvent[]>([]);
const loading = ref(true);
const errorMsg = ref('');

const fetchTimeline = async () => {
  try {
    loading.value = true;
    // Assuming backend returns an array or an object with data property
    const response: any = await api.get('/candidates/me/timeline');
    // If your backend wrapped the data in a 'data' field, handle it here:
    events.value = response.data || response || [];
  } catch (err: any) {
    console.error('Failed to load timeline:', err);
    errorMsg.value = '无法加载时间线';
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchTimeline();
});

const getActionLabel = (action: string) => {
  const map: Record<string, string> = {
    'APPLIED': '投递了简历',
    'WITHDRAWN': '撤回了投递',
    'FAVORITED': '收藏了职位',
    'UNFAVORITED': '取消收藏',
    'IGNORED': '忽略了职位',
    'UNIGNORED': '取消忽略'
  };
  return map[action] || action;
};

const getActionColor = (action: string) => {
  const map: Record<string, string> = {
    'APPLIED': 'bg-blue-500',
    'WITHDRAWN': 'bg-gray-500',
    'FAVORITED': 'bg-yellow-500',
    'UNFAVORITED': 'bg-gray-400',
    'IGNORED': 'bg-red-500',
    'UNIGNORED': 'bg-gray-400'
  };
  return map[action] || 'bg-gray-300';
};

const formatTime = (isoString: string) => {
  const date = new Date(isoString);
  return date.toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};
</script>