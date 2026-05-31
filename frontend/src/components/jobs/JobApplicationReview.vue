<template>
  <section class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm mt-8">
    <div class="flex flex-col gap-3 border-b border-slate-100 pb-4 md:flex-row md:items-end md:justify-between">
      <div>
        <p class="text-xs font-semibold uppercase tracking-[0.24em] text-emerald-500">Application Review</p>
        <h2 class="mt-2 text-xl font-bold text-slate-900">申请审核台</h2>
        <p class="mt-1 text-sm text-slate-500">这里展示当前岗位的有效投递，便于 HR 从 AI 推荐过渡到实际处理。</p>
      </div>
      <div class="inline-flex items-center gap-2 rounded-full border border-emerald-100 bg-emerald-50 px-3 py-1.5 text-xs font-semibold text-emerald-700">
        <Inbox class="w-4 h-4" />
        有效投递 {{ applications.length }} 份
      </div>
    </div>

    <div v-if="loading" class="py-10 text-center text-slate-500">
      <Loader2 class="w-8 h-8 mx-auto mb-3 animate-spin text-emerald-500" />
      <p>正在加载岗位投递记录...</p>
    </div>

    <div v-else-if="error" class="mt-5 rounded-xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700 flex flex-col items-center">
      <p class="font-semibold">{{ error }}</p>
      <button @click="$emit('retry')" class="mt-3 rounded-lg bg-white border border-rose-200 px-4 py-2 text-sm font-medium text-rose-600 transition hover:bg-rose-100">重试</button>
    </div>

    <div v-else-if="applications.length === 0" class="mt-5 rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-6 py-10 text-center text-slate-500">
      <div class="text-4xl">📬</div>
      <p class="mt-3 font-semibold text-slate-700">当前还没有候选人投递这个岗位</p>
      <p class="mt-2 text-sm">你可以先发起 AI 评估并让候选人从推荐岗位详情进入投递，之后这里会聚合有效申请。</p>
    </div>

    <div v-else class="mt-5 space-y-4">
      <article v-for="app in applications" :key="app.applicationId" class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm hover:border-slate-300 transition-colors">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div class="min-w-0 flex-1">
            <div class="flex items-center gap-3">
              <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-slate-100 text-lg font-bold text-slate-700 border border-slate-200">
                {{ getDisplayName(app).charAt(0) }}
              </div>
              <div class="min-w-0">
                <h3 class="truncate text-lg font-bold text-slate-900">{{ getDisplayName(app) }}</h3>
                <p class="truncate text-sm text-slate-500">{{ app.candidate.username }} · {{ app.candidate.email }}</p>
              </div>
            </div>

            <div class="mt-4 flex flex-wrap gap-2">
               <span :class="getStatusClass(app.status)" class="rounded-lg px-2.5 py-1 text-xs font-semibold border">
                 {{ getStatusLabel(app.status) }}
               </span>
               <span v-if="app.latestResume" class="rounded-lg border border-slate-200 bg-slate-50 px-2.5 py-1 text-xs font-semibold text-slate-600">
                 简历 {{ getResumeStatusLabel(app.latestResume.status) }}
               </span>
               <span v-if="getRecommendation(app)" class="rounded-lg border border-emerald-200 bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-700">
                 AI 综合得分 {{ getRecommendation(app)?.matchScore }}/100
               </span>
            </div>

            <div class="mt-4 grid gap-3 text-sm text-slate-600 md:grid-cols-2">
               <div class="rounded-xl bg-slate-50 px-4 py-3 border border-slate-100">
                 <p class="text-xs font-semibold text-slate-400 mb-1">投递时间</p>
                 <p class="font-medium text-slate-800">{{ formatDateTime(app.appliedAt) }}</p>
               </div>
               <div class="rounded-xl bg-slate-50 px-4 py-3 border border-slate-100">
                 <p class="text-xs font-semibold text-slate-400 mb-1">最近更新</p>
                 <p class="font-medium text-slate-800">{{ formatDateTime(app.updatedAt) }}</p>
               </div>
               <div v-if="app.latestResume" class="rounded-xl bg-slate-50 px-4 py-3 md:col-span-2 border border-slate-100">
                  <p class="text-xs font-semibold text-slate-400 mb-1">最近简历</p>
                  <p class="font-medium text-slate-800">{{ app.latestResume.candidateName || getDisplayName(app) }}</p>
                  <p class="text-xs text-slate-500 mt-1">联系方式：{{ app.latestResume.contactInfo || app.candidate.email }}</p>
               </div>
            </div>
            
            <div v-if="app.reviewNote" class="mt-4 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900 line-clamp-3 overflow-hidden text-ellipsis">
              <p class="font-semibold mb-1 flex items-center gap-1"><FileText class="w-4 h-4"/>当前审核备注</p>
              {{ app.reviewNote }}
            </div>
          </div>

          <div class="flex shrink-0 flex-col gap-3 lg:w-72 mt-4 lg:mt-0">
             <button
               v-if="app.latestResume?.resumeId"
               @click="$emit('view-resume', app.latestResume.resumeId)"
               class="w-full rounded-xl bg-slate-800 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-slate-900 border border-slate-900"
             >
               查看简历详情
             </button>
             <div v-else class="rounded-xl border border-dashed border-slate-200 px-4 py-2.5 text-sm text-slate-400 text-center bg-slate-50">
               无可用简历
             </div>

             <div class="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
                <p class="text-xs font-semibold text-slate-500 mb-3 border-b border-slate-100 pb-2">审核动作</p>
                <div class="space-y-3">
                  <div>
                    <label class="block text-xs font-medium text-slate-700 mb-1">处理状态</label>
                    <select v-model="drafts[String(app.applicationId)].status" class="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100">
                      <option value="APPLIED">待处理 (Applied)</option>
                      <option value="INTERVIEW">约面 (Interview)</option>
                      <option value="REJECTED">淘汰 (Rejected)</option>
                    </select>
                  </div>
                  <div>
                    <label class="block text-xs font-medium text-slate-700 mb-1">审核备注</label>
                    <textarea v-model="drafts[String(app.applicationId)].reviewNote" rows="2" placeholder="记录筛选结论或淘汰原因" class="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"></textarea>
                  </div>
                  <button
                    @click="saveReview(app)"
                    :disabled="drafts[String(app.applicationId)].saving"
                    class="w-full rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-blue-700 disabled:opacity-70 flex justify-center items-center"
                  >
                    <Loader2 v-if="drafts[String(app.applicationId)].saving" class="w-4 h-4 animate-spin mr-2" />
                    {{ drafts[String(app.applicationId)].saving ? '保存中...' : '保存审核' }}
                  </button>
                </div>
             </div>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { Inbox, Loader2, FileText } from 'lucide-vue-next';

interface ApplicationProps {
  applications: any[];
  loading: boolean;
  error: string;
  recommendations: any[];
  drafts: Record<string, any>;
}

const props = defineProps<ApplicationProps>();

const emit = defineEmits<{
  (e: 'retry'): void;
  (e: 'view-resume', id: string): void;
  (e: 'save-review', application: any): void;
}>();

const formatDateTime = (value?: string | null) => {
  if (!value) return '未知时间';
  return new Date(value).toLocaleString();
};

const getDisplayName = (app: any) => {
  return app?.candidate?.displayName || app?.candidate?.username || '未知';
};

const getStatusLabel = (status?: string | null) => {
  switch (status) {
    case 'APPLIED': return '待处理投递';
    case 'INTERVIEW': return '已约面';
    case 'REJECTED': return '未通过';
    default: return status || '未知';
  }
};

const getStatusClass = (status?: string | null) => {
  switch (status) {
    case 'APPLIED': return 'border-blue-200 bg-blue-50 text-blue-700';
    case 'INTERVIEW': return 'border-emerald-200 bg-emerald-50 text-emerald-700';
    case 'REJECTED': return 'border-rose-200 bg-rose-50 text-rose-700';
    default: return 'border-slate-200 bg-slate-50 text-slate-700';
  }
};

const getResumeStatusLabel = (status?: string | null) => {
  switch (status) {
    case 'PARSED': return '已解析';
    case 'PARSING': return '解析中';
    case 'PENDING_PARSE': return '待解析';
    case 'PARSE_FAILED': return '解析失败';
    default: return status || '未知';
  }
};

const getRecommendation = (app: any) => {
  const resumeId = app?.latestResume?.resumeId;
  if (!resumeId) return null;
  return props.recommendations.find(r => r.resumeId === resumeId) || null;
};

const saveReview = (app: any) => {
  emit('save-review', app);
};
</script>
