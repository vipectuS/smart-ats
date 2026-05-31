<template>
  <div class="h-full overflow-y-auto bg-slate-50 p-8">
    <div class="mx-auto max-w-5xl space-y-6">
      <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h1 class="text-2xl font-bold text-slate-800">我的投递</h1>
        <p class="mt-1 text-sm text-slate-500">跟踪您投递的所有岗位进度与状态。</p>
      </div>

      <div v-if="loading" class="rounded-2xl border border-slate-200 bg-white p-12 text-center shadow-sm">
        <Loader2 class="w-8 h-8 animate-spin text-blue-500 mx-auto" />
        <p class="mt-4 text-slate-500 font-medium">正在加载投递记录...</p>
      </div>

      <div v-else-if="errorMsg" class="rounded-2xl border border-rose-200 bg-rose-50 p-8 text-center text-rose-700 shadow-sm">
        <AlertCircle class="w-8 h-8 mx-auto mb-3" />
        <p class="font-semibold">{{ errorMsg }}</p>
      </div>

      <div v-else-if="applications.length === 0" class="rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-16 text-center shadow-sm">
        <FileText class="w-12 h-12 mx-auto mb-4 text-slate-300" />
        <p class="text-lg font-semibold text-slate-700">暂无投递记录</p>
        <p class="mt-2 text-sm text-slate-500">去看看为您推荐的职位吧</p>
        <button @click="router.push({ name: 'candidateDashboard' })" class="mt-6 rounded-xl bg-blue-600 px-6 py-2.5 font-semibold text-white transition hover:bg-blue-700 shadow-sm">
          发现推荐机会
        </button>
      </div>

      <div v-else class="space-y-4">
        <article v-for="app in applications" :key="app.id" class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md">
          <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
             <div class="flex-1 min-w-0">
               <h3 class="flex items-center gap-2 text-lg font-bold text-slate-800">
                  <span class="truncate">{{ app.jobTitle || '未知岗位' }}</span>
                  <span :class="getStatusClass(app.status)" class="rounded-full px-2.5 py-0.5 text-xs font-bold whitespace-nowrap">{{ getStatusLabel(app.status) }}</span>
               </h3>
               <div class="mt-2 flex items-center gap-4 text-sm text-slate-500">
                  <span class="flex items-center gap-1.5"><Building2 class="w-4 h-4" /> {{ app.organizationName || '未知企业' }}</span>
                  <span class="flex items-center gap-1.5"><Calendar class="w-4 h-4" /> 投递于 {{ formatDate(app.createdAt) }}</span>
               </div>
             </div>
             
             <div class="flex gap-3">
               <button @click="viewJobDetail(app.jobId)" class="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 hover:border-slate-300 shadow-sm flex items-center gap-1.5 whitespace-nowrap">
                 <Briefcase class="w-4 h-4" /> 岗位详情
               </button>
             </div>
          </div>

          <div v-if="app.fitScore !== null" class="mt-5 border-t border-slate-100 pt-4">
            <div class="flex items-start gap-4">
              <div class="flex flex-col items-center justify-center rounded-lg bg-slate-50 p-3 min-w-[80px]">
                <span class="text-xs font-medium text-slate-500 mb-1">匹配度</span>
                <span class="text-xl font-black" :class="getScoreColor(app.fitScore)">{{ Math.round(app.fitScore) }}</span>
              </div>
              <p class="text-sm leading-relaxed text-slate-600 italic">"{{ app.summary || '暂无 AI 匹配总结' }}"</p>
            </div>
          </div>
        </article>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Loader2, AlertCircle, FileText, Briefcase, Calendar, Building2 } from 'lucide-vue-next'
import api from '@/utils/api'

interface ApplicationRecord {
  id: string
  jobId: string
  jobTitle: string
  organizationName: string
  status: string
  createdAt: string
  fitScore: number | null
  summary: string | null
}

const router = useRouter()
const applications = ref<ApplicationRecord[]>([])
const loading = ref(true)
const errorMsg = ref('')

const fetchApplications = async () => {
  try {
    loading.value = true
    errorMsg.value = ''
    const res: any = await api.get('/candidate/applications')
    applications.value = res.data
  } catch (error) {
    console.error('Failed to fetch applications', error)
    errorMsg.value = '获取投递记录失败，请稍后再试。'
  } finally {
    loading.value = false
  }
}

const formatDate = (val: string) => {
  return new Date(val).toLocaleDateString('zh-CN')
}

const getStatusLabel = (status: string) => {
  const map: Record<string, string> = { PENDING: '已投递', REVIEWING: '查看中', REJECTED: '不合适', ACCEPTED: '已邀请' }
  return map[status] || status
}

const getStatusClass = (status: string) => {
  switch(status) {
    case 'PENDING': return 'bg-slate-100 text-slate-600'
    case 'REVIEWING': return 'bg-blue-100 text-blue-700'
    case 'ACCEPTED': return 'bg-emerald-100 text-emerald-700'
    case 'REJECTED': return 'bg-rose-100 text-rose-700'
    default: return 'bg-slate-100 text-slate-600'
  }
}

const getScoreColor = (score: number) => {
  if (score >= 80) return 'text-emerald-600'
  if (score >= 60) return 'text-amber-600'
  return 'text-rose-600'
}

const viewJobDetail = (jobId: string) => {
  router.push({ name: 'candidateJobDetail', params: { id: jobId } })
}

onMounted(() => {
  fetchApplications()
})
</script>
