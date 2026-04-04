<template>
  <div class="p-8 max-w-4xl mx-auto w-full">
    <h1 class="text-2xl font-bold mb-6 bg-clip-text text-transparent bg-gradient-to-r from-blue-600 to-indigo-600">我的投递记录</h1>
    <div v-if="loading" class="text-center py-10 text-gray-400">
      <i class="fas fa-spinner fa-spin text-2xl"></i> 加载中...
    </div>
    <div v-else-if="applications.length === 0" class="text-center py-10 text-gray-400">
      暂无投递记录。
    </div>
    <div v-else class="space-y-4">
      <div v-for="app in applications" :key="app.id" class="bg-white/80 backdrop-blur-md rounded-2xl p-6 border border-white/40 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div class="flex items-center gap-3 mb-1">
            <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-100 to-indigo-100 flex items-center justify-center text-blue-600 text-xl font-bold border border-blue-200 shadow-sm">
              {{ app.jobTitle.charAt(0) }}
            </div>
            <div>
              <div class="text-lg font-bold text-slate-800">{{ app.jobTitle }}</div>
              <div class="text-xs text-slate-500">{{ app.company }} • {{ app.location }}</div>
            </div>
          </div>
          <div class="text-sm text-slate-600 mt-2">投递时间：{{ app.appliedAt }}</div>
        </div>
        <div class="flex flex-col gap-2 md:items-end">
          <span class="px-3 py-1 rounded-full text-xs font-semibold" :class="app.status === 'PENDING' ? 'bg-yellow-100 text-yellow-700' : app.status === 'INTERVIEW' ? 'bg-blue-100 text-blue-700' : app.status === 'OFFER' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'">
            {{ app.status === 'PENDING' ? '待处理' : app.status === 'INTERVIEW' ? '面试中' : app.status === 'OFFER' ? '已录用' : '已结束' }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
// import api from '../utils/api' // 等后端接口 ready

const loading = ref(true)
const applications = ref<any[]>([])

const fetchApplications = async () => {
  loading.value = true
  // TODO: 替换为真实 API
  // const { data } = await api.get('/candidate/applications')
  // applications.value = data
  await new Promise(r => setTimeout(r, 500))
  applications.value = [
    { id: 1, jobTitle: 'Java后端开发', company: '字节跳动', location: '北京', appliedAt: '2026-03-30', status: 'PENDING' },
    { id: 2, jobTitle: '前端开发工程师', company: '阿里巴巴', location: '杭州', appliedAt: '2026-03-28', status: 'INTERVIEW' },
    { id: 3, jobTitle: 'AI算法实习生', company: '百度', location: '北京', appliedAt: '2026-03-25', status: 'OFFER' }
  ]
  loading.value = false
}

onMounted(() => {
  fetchApplications()
})
</script>
