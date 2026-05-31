<template>
  <div class="h-full bg-white flex flex-col shadow-sm border-l border-gray-100">
    <div class="px-6 py-8 border-b border-gray-100 bg-gray-50 flex items-center justify-between">
      <h2 class="text-2xl font-black text-gray-800 tracking-tight flex items-center gap-2">
        <span class="text-blue-500">👣</span> 活动轨迹
      </h2>
    </div>

    <div class="flex-1 p-6 overflow-y-auto relative">
      <div v-if="loading" class="text-gray-500 text-sm py-10 flex flex-col items-center justify-center gap-3">
        <div class="w-8 h-8 rounded-full border-4 border-blue-100 border-t-blue-500 animate-spin"></div>
        <p>加载记录中...</p>
      </div>
      
      <div v-else-if="errorMsg" class="bg-red-50 text-red-600 rounded-xl p-4 text-sm flex items-start gap-3 mt-4">
        <span class="text-red-500 mt-0.5">⚠️</span>
        <p>{{ errorMsg }}</p>
      </div>

      <div v-else-if="events.length === 0" class="text-gray-400 text-sm py-16 flex flex-col items-center justify-center text-center gap-4">
        <div class="text-5xl opacity-40">📭</div>
        <div>
          <p class="font-medium text-gray-500 mb-1">暂无活动记录</p>
          <p class="text-xs">收藏或投递职位后将在此显示</p>
        </div>
      </div>

      <div v-else class="relative space-y-6 before:absolute before:inset-0 before:ml-4 before:-translate-x-px md:before:mx-auto md:before:translate-x-0 before:h-full before:w-0.5 before:bg-gradient-to-b before:from-transparent before:via-gray-200 before:to-transparent pt-2 pb-6">
        <div v-for="event in events" :key="event.id" class="relative flex items-center justify-between md:justify-normal md:odd:flex-row-reverse group">
          
          <!-- Timeline Icon Marker -->
          <div class="flex items-center justify-center w-8 h-8 rounded-full border-4 border-white shadow-sm shrink-0 md:order-1 md:group-odd:-translate-x-1/2 md:group-even:translate-x-1/2 z-10 transition-transform duration-300 group-hover:scale-110"
               :class="getActionColor(event.action)">
            <span class="text-[10px] text-white">{{ getActionIcon(event.action) }}</span>
          </div>
          
          <!-- Content Card -->
          <div class="w-[calc(100%-2.5rem)] md:w-[calc(50%-1.5rem)] pl-3 md:pl-0">
            <div class="bg-white p-4 rounded-xl shadow-[0_2px_10px_-3px_rgba(6,81,237,0.1)] border border-gray-100 group-hover:border-blue-200 group-hover:shadow-md transition-all duration-300 relative"
                 :class="[
                   'md:group-even:text-right md:group-even:pr-4 md:group-odd:pl-4',
                   'before:absolute before:top-4 before:w-0 before:h-0 before:border-[6px] before:border-transparent',
                   // left side arrow (desktop even, mobile all)
                   'before:left-0 before:-translate-x-full before:border-r-white',
                   // right side arrow (desktop odd)
                   'md:group-odd:before:left-auto md:group-odd:before:right-0 md:group-odd:before:translate-x-full md:group-odd:before:border-r-transparent md:group-odd:before:border-l-white'
                 ]">
              <div class="flex flex-col gap-1">
                <div class="flex items-center justify-between md:group-even:flex-row-reverse gap-2 mb-1">
                  <span class="text-xs font-bold px-2 py-0.5 rounded-md" :class="getActionBadgeColor(event.action)">
                    {{ getActionLabel(event.action) }}
                  </span>
                  <span class="text-[10px] text-gray-400 font-mono">
                    {{ formatTime(event.timestamp) }}
                  </span>
                </div>
                <h3 class="text-sm font-bold text-gray-800 line-clamp-1 mt-1 group-hover:text-blue-600 transition-colors" :title="event.jobTitle">
                  {{ event.jobTitle }}
                </h3>
                <p class="text-xs text-gray-500 line-clamp-1 flex items-center md:group-even:justify-end gap-1">
                  <span class="opacity-70">🏢</span> {{ event.companyName }}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';
import api from '@/utils/api';
import { subscribeCandidateActivityUpdated } from '@/utils/candidateActivity';

interface TimelineEvent {
  id: string;
  action: 'APPLIED' | 'INTERVIEW' | 'REJECTED' | 'WITHDRAWN' | 'FAVORITED' | 'UNFAVORITED' | 'IGNORED' | 'UNIGNORED';
  jobTitle: string;
  companyName: string;
  timestamp: string;
}

const events = ref<TimelineEvent[]>([]);
const loading = ref(true);
const errorMsg = ref('');
let unsubscribe: (() => void) | null = null;

const fetchTimeline = async () => {
  try {
    loading.value = true;
    errorMsg.value = '';
    const response: any = await api.get('/candidates/me/timeline');
    const items = response.data || [];
    events.value = items.map((item: any, index: number) => ({
      id: `${item.action}-${item.timestamp}-${index}`,
      ...item,
    }));
  } catch (err: any) {
    console.error('Failed to load timeline:', err);
    errorMsg.value = '无法加载时间线';
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  void fetchTimeline();
  unsubscribe = subscribeCandidateActivityUpdated(() => {
    void fetchTimeline();
  });
});

onBeforeUnmount(() => {
  unsubscribe?.();
  unsubscribe = null;
});

const getActionLabel = (action: string) => {
  const map: Record<string, string> = {
    'APPLIED': '投递简历',
    'INTERVIEW': '已约面试',
    'REJECTED': '遗憾未通过',
    'WITHDRAWN': '撤回投递',
    'FAVORITED': '收藏职位',
    'UNFAVORITED': '取消收藏',
    'IGNORED': '忽略职位',
    'UNIGNORED': '取消忽略'
  };
  return map[action] || action;
};

const getActionIcon = (action: string) => {
  const map: Record<string, string> = {
    'APPLIED': '🚀',
    'INTERVIEW': '🤝',
    'REJECTED': '💔',
    'WITHDRAWN': '↩️',
    'FAVORITED': '⭐',
    'UNFAVORITED': '☆',
    'IGNORED': '🙈',
    'UNIGNORED': '👀'
  };
  return map[action] || '📌';
};

const getActionColor = (action: string) => {
  const map: Record<string, string> = {
    'APPLIED': 'bg-blue-500 shadow-blue-200',
    'INTERVIEW': 'bg-emerald-500 shadow-emerald-200',
    'REJECTED': 'bg-rose-500 shadow-rose-200',
    'WITHDRAWN': 'bg-gray-500 shadow-gray-200',
    'FAVORITED': 'bg-amber-500 shadow-amber-200',
    'UNFAVORITED': 'bg-slate-400 shadow-slate-200',
    'IGNORED': 'bg-red-500 shadow-red-200',
    'UNIGNORED': 'bg-slate-400 shadow-slate-200'
  };
  return map[action] || 'bg-gray-300';
};

const getActionBadgeColor = (action: string) => {
  const map: Record<string, string> = {
    'APPLIED': 'bg-blue-50 text-blue-700',
    'INTERVIEW': 'bg-emerald-50 text-emerald-700 border-emerald-200 border',
    'REJECTED': 'bg-rose-50 text-rose-700',
    'WITHDRAWN': 'bg-gray-100 text-gray-600',
    'FAVORITED': 'bg-amber-50 text-amber-700',
    'UNFAVORITED': 'bg-slate-50 text-slate-500',
    'IGNORED': 'bg-red-50 text-red-700',
    'UNIGNORED': 'bg-slate-50 text-slate-600'
  };
  return map[action] || 'bg-gray-100 text-gray-500';
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