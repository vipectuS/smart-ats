<template>
  <div class="p-8 max-w-4xl mx-auto w-full">
    <h1 class="text-2xl font-bold mb-6 bg-clip-text text-transparent bg-gradient-to-r from-blue-600 to-indigo-600">我的投递记录</h1>
    <div v-if="loading" class="rounded-2xl border border-white/20 bg-white/70 p-10 text-center text-gray-500 shadow-sm">
      <i class="fas fa-spinner fa-spin text-2xl text-blue-500"></i>
      <p class="mt-3">正在读取投递记录...</p>
    </div>
    <div v-else-if="errorMsg" class="rounded-2xl border border-rose-200 bg-rose-50 p-6 text-center text-rose-700 shadow-sm">
      <p class="font-semibold">投递记录加载失败</p>
      <p class="mt-2 text-sm">{{ errorMsg }}</p>
      <button @click="fetchApplications" class="mt-4 rounded-lg bg-rose-600 px-4 py-2 text-white transition hover:bg-rose-700">重试</button>
    </div>
    <div v-else-if="applications.length === 0" class="rounded-2xl border border-white/20 bg-white/70 p-10 text-center text-gray-500 shadow-sm">
      <div class="mb-3 text-4xl">🧾</div>
      <p>暂无投递记录。</p>
      <p class="mt-2 text-sm">先去推荐页查看适合你的岗位，再完成第一次投递。</p>
      <button @click="router.push({ name: 'candidateDashboard' })" class="mt-4 rounded-lg bg-blue-600 px-4 py-2 text-white transition hover:bg-blue-700">前往推荐页</button>
    </div>
    <div v-else class="space-y-4">
      <div
        v-for="app in applications"
        :key="app.id"
        class="bg-white/80 backdrop-blur-md rounded-2xl p-6 border border-white/40 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4 cursor-pointer transition hover:-translate-y-0.5 hover:shadow-md"
        @click="openJobDetail(app.id)"
      >
        <div>
          <div class="flex items-center gap-3 mb-1">
            <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-100 to-indigo-100 flex items-center justify-center text-blue-600 text-xl font-bold border border-blue-200 shadow-sm">
              {{ app.jobTitle.charAt(0) }}
            </div>
            <div>
              <div class="text-lg font-bold text-slate-800">{{ app.jobTitle }}</div>
              <div class="text-xs text-slate-500">{{ app.meta }}</div>
            </div>
          </div>
          <div class="text-sm text-slate-600 mt-2">最新状态时间：{{ app.updatedAt }}</div>
          <div class="text-sm text-slate-500 mt-2 line-clamp-2">{{ app.description }}</div>
        </div>
        <div class="flex flex-col gap-2 md:items-end">
          <span :class="app.statusClass" class="px-3 py-1 rounded-full text-xs font-semibold">
            {{ app.statusLabel }}
          </span>
          <button @click.stop="openJobDetail(app.id)" class="rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-700 transition hover:bg-slate-50">
            查看岗位详情
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../utils/api'

const router = useRouter()
const loading = ref(true)
const applications = ref<any[]>([])
const errorMsg = ref('')

const formatActionTime = (isoString: string) => {
  return new Date(isoString).toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getMetaText = (requirements: Record<string, any> | null | undefined) => {
  const company = requirements?.company || '未知公司'
  const location = requirements?.location || '未知地点'
  return `${company} • ${location}`
}

const getStatusPresentation = (status: string) => {
  switch (status) {
    case 'APPLIED':
      return { label: '已投递', className: 'bg-blue-100 text-blue-700' }
    case 'INTERVIEW':
      return { label: '已约面', className: 'bg-emerald-100 text-emerald-700' }
    case 'REJECTED':
      return { label: '未通过', className: 'bg-rose-100 text-rose-700' }
    case 'WITHDRAWN':
      return { label: '已撤回', className: 'bg-slate-200 text-slate-700' }
    default:
      return { label: status, className: 'bg-slate-100 text-slate-700' }
  }
}

const fetchApplications = async () => {
  loading.value = true
  errorMsg.value = ''
  try {
    const response: any = await api.get('/candidate/applications')
    const items = response.data || []
    applications.value = items.map((item: any) => ({
      id: item.jobId,
      jobTitle: item.title,
      meta: getMetaText(item.requirements),
      appliedAt: formatActionTime(item.actionCreatedAt),
      updatedAt: formatActionTime(item.actionUpdatedAt || item.actionCreatedAt),
      status: item.actionType,
      statusLabel: getStatusPresentation(item.actionType).label,
      statusClass: getStatusPresentation(item.actionType).className,
      description: item.description,
    }))
  } catch (error) {
    console.error('Failed to fetch applications', error)
    errorMsg.value = '请检查候选人登录状态或稍后重试。'
    applications.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchApplications()
})

const openJobDetail = (jobId: string) => {
  router.push({ name: 'candidateJobDetail', params: { id: jobId } })
}
</script>
