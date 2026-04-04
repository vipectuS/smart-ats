<template>
  <div class="h-full flex flex-col p-6 space-y-6">
    <div class="flex justify-between items-center bg-white/50 backdrop-blur-xl p-6 rounded-2xl border border-white/20 shadow-sm">
      <div>
        <h1 class="text-2xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-blue-600 to-indigo-600">招聘岗位</h1>
        <p class="text-sm text-gray-500 mt-1">管理所有发布的职位并查看 AI 推荐</p>
      </div>
      <!-- Placeholder button -->
      <button class="px-4 py-2 bg-blue-600 text-white rounded-lg shadow-md font-medium hover:bg-blue-700 transition">
        <i class="fas fa-plus mr-2"></i> 发布职位
      </button>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 overflow-y-auto pb-4">
      <div v-for="job in jobs" :key="job.id" 
           @click="viewJob(job.id)"
           class="bg-white/70 backdrop-blur-md border border-white/40 shadow-sm rounded-xl p-5 hover:shadow-lg hover:-translate-y-1 transition duration-300 cursor-pointer flex flex-col">
        <div class="flex justify-between items-start mb-4">
          <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-100 to-indigo-100 flex items-center justify-center text-blue-600 text-xl font-bold border border-blue-200 shadow-sm">
            {{ job.title.charAt(0) }}
          </div>
          <span :class="job.status === 'PUBLISHED' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'" class="px-2.5 py-1 rounded-full text-xs font-semibold">
            {{ job.status === 'PUBLISHED' ? '已发布' : job.status }}
          </span>
        </div>
        
        <h3 class="text-lg font-bold text-slate-800 mb-1 line-clamp-1">{{ job.title }}</h3>
        <p class="text-sm text-slate-500 mb-4">{{ job.department }} • {{ job.location }}</p>
        
        <div class="mt-auto pt-4 border-t border-slate-100 flex justify-between items-center text-sm text-slate-600">
          <div class="flex space-x-4">
             <span title="招聘人数" class="flex items-center"><i class="fas fa-users mr-1.5 text-blue-400"></i> {{ job.headcount }}</span>
          </div>
          <span class="text-blue-600 font-medium hover:text-blue-700">查看推荐 &rarr;</span>
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
const jobs = ref<any[]>([])

const fetchJobs = async () => {
  try {
    const response: any = await api.get('/jobs')
    // api returns { status, data, message } or similar wrapper, pageable is in data.content?
    // Let's rely on standard backend wrapper or page response
    jobs.value = response.data?.content || response.data || []
  } catch (error) {
    console.error('Failed to fetch jobs', error)
  }
}

const viewJob = (id: number) => {
  router.push(`/jobs/${id}`) // Or '{ name: "jobDetail", params: { id } }'
}

onMounted(() => {
  fetchJobs()
})
</script>
