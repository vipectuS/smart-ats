<template>
  <div class="h-full overflow-y-auto bg-slate-50 px-4 py-6 sm:px-6 lg:px-8">
    <div class="mx-auto max-w-[1500px] space-y-6">
      <div v-if="feedback" :class="feedback.type === 'error' ? 'border-rose-200 bg-rose-50 text-rose-700' : 'border-emerald-200 bg-emerald-50 text-emerald-700'" class="rounded-2xl border px-5 py-4 text-sm shadow-sm flex items-center gap-2">
        <AlertCircle class="w-4 h-4 flex-shrink-0" />
        {{ feedback.message }}
      </div>

      <div v-if="loading" class="rounded-2xl border border-slate-200 bg-white p-16 text-center text-slate-500 shadow-sm flex flex-col justify-center items-center min-h-[40vh]">
        <Loader2 class="w-8 h-8 animate-spin text-blue-500 mb-4" />
        <p class="font-medium text-lg">正在加载简历详情...</p>
      </div>

      <div v-else-if="errorMsg" class="rounded-2xl border border-rose-200 bg-rose-50 p-10 text-center text-rose-700 shadow-sm flex flex-col justify-center items-center min-h-[40vh]">
        <AlertCircle class="w-10 h-10 mb-4 text-rose-400" />
        <p class="font-bold text-xl mb-1">简历详情加载失败</p>
        <p class="text-sm opacity-80">{{ errorMsg }}</p>
        <button @click="refreshResumeDetail" class="mt-6 rounded-xl bg-rose-600 px-6 py-2.5 font-semibold text-white transition hover:bg-rose-700 shadow-sm">立即重试</button>
      </div>

      <template v-else-if="resume">
        <header class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm flex flex-col gap-6 xl:flex-row xl:items-start xl:justify-between">
          <div class="min-w-0 flex-1 space-y-4">
            <button @click="router.push({ name: 'resumes' })" class="inline-flex items-center text-sm font-medium text-slate-500 transition hover:text-blue-600 gap-1.5 focus:outline-none">
              <ArrowLeft class="w-4 h-4" />
              返回简历库
            </button>
            <div>
              <h1 class="text-3xl font-bold text-slate-900">{{ displayName }}</h1>
              <p class="mt-2 text-sm font-medium text-slate-500 break-all bg-slate-50 px-2 py-1 rounded inline-block">{{ resume.rawContentReference }}</p>
            </div>
            <div class="flex flex-wrap gap-2 text-xs font-semibold text-slate-600">
               <span class="rounded-md border border-slate-200 bg-white px-2.5 py-1.5 shadow-sm flex items-center gap-1">
                 <MapPin class="w-3.5 h-3.5 text-slate-400" />
                 {{ resume.contactInfo || '联系方式未提供' }}
               </span>
               <span class="rounded-md border border-slate-200 bg-white px-2.5 py-1.5 shadow-sm flex items-center gap-1">
                 <RefreshCw class="w-3.5 h-3.5 text-slate-400" />
                 更新：{{ formatDateTime(resume.updatedAt) }}
               </span>
               <span class="rounded-md border border-slate-200 bg-white px-2.5 py-1.5 shadow-sm flex items-center gap-1">
                 <FileText class="w-3.5 h-3.5 text-slate-400" />
                 入库：{{ formatDateTime(resume.createdAt) }}
               </span>
            </div>
          </div>

          <div class="flex w-full shrink-0 flex-col gap-3 xl:w-[280px]">
            <div :class="statusClass" class="rounded-xl border px-5 py-4 text-center shadow-sm">
              <p class="text-xs font-bold uppercase tracking-[0.2em] opacity-80">当前状态</p>
              <p class="mt-2.5 text-2xl font-black capitalize flex justify-center items-center gap-1.5">
                <CheckCircle v-if="resume.status === 'PARSED'" class="w-6 h-6" />
                <Loader2 v-if="resume.status === 'PARSING'" class="w-6 h-6 animate-spin" />
                <AlertCircle v-if="resume.status === 'PARSE_FAILED'" class="w-6 h-6" />
                {{ statusLabel }}
              </p>
            </div>
            <div class="flex gap-3">
              <button @click="refreshResumeDetail" :disabled="refreshing" class="flex-1 rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 disabled:opacity-70 disabled:cursor-not-allowed shadow-sm flex items-center justify-center gap-1.5">
                <RefreshCw class="w-4 h-4" :class="{'animate-spin': refreshing}" />
                {{ refreshing ? '刷新中' : '刷新' }}
              </button>
              <button v-if="resume.status !== 'PARSED'" @click="triggerParse" :disabled="parsing" class="flex-1 rounded-xl bg-blue-600 px-3 py-2 text-sm font-semibold text-white shadow-sm shadow-blue-500/20 transition hover:bg-blue-700 disabled:opacity-70 disabled:cursor-not-allowed flex items-center justify-center gap-1.5">
                <BrainCircuit class="w-4 h-4" :class="{'animate-pulse': parsing}" />
                {{ parsing ? '投递中' : 'AI 提取' }}
              </button>
            </div>
          </div>
        </header>

        <div class="grid grid-cols-1 items-start gap-6 xl:grid-cols-[minmax(0,1.35fr)_minmax(22rem,0.85fr)] 2xl:grid-cols-[minmax(0,1.45fr)_minmax(24rem,0.8fr)]">
          <div class="min-w-0">
              <ResumeBasicProfile 
                class="min-w-0 xl:col-span-7 2xl:col-span-8"
              :hasParsedData="hasParsedData"
              :summaryText="summaryText"
              :basicInfo="basicInfo"
              :skillNames="skillNames"
              :workExperiences="workExperiences"
              :educationExperiences="educationExperiences"
            />
          </div>
          <div class="min-w-0 xl:sticky xl:top-6">
              <ResumeMetadataSidebar 
                class="min-w-0 xl:col-span-5 2xl:col-span-4"
              :resume="resume"
              :displayName="displayName"
              :statusHint="statusHint"
              :parsedJson="parsedJson"
            />
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Loader2, RefreshCw, FileText, CheckCircle, AlertCircle, BrainCircuit, MapPin } from 'lucide-vue-next'
import api from '../utils/api'
import { resolveApiError } from '@/utils/apiError'
import ResumeBasicProfile from '@/components/resumes/ResumeBasicProfile.vue'
import ResumeMetadataSidebar from '@/components/resumes/ResumeMetadataSidebar.vue'

interface ResumeDetail {
  id: string
  candidateName: string | null
  contactInfo: string | null
  rawContentReference: string
  parsedData: Record<string, any> | null
  parseFailureReason: string | null
  status: string
  createdAt: string
  updatedAt: string
}

const route = useRoute()
const router = useRouter()
const resumeId = String(route.params.id)
const loading = ref(true)
const refreshing = ref(false)
const parsing = ref(false)
const errorMsg = ref('')
const feedback = ref<{ type: 'success' | 'error'; message: string } | null>(null)
const resume = ref<ResumeDetail | null>(null)
let pollingTimer: ReturnType<typeof setInterval> | null = null

const parsedData = computed(() => resume.value?.parsedData || null)
const hasParsedData = computed(() => Boolean(parsedData.value && Object.keys(parsedData.value).length > 0))
const basicInfo = computed(() => parsedData.value?.basicInfo || {})
const workExperiences = computed(() => Array.isArray(parsedData.value?.workExperiences) ? parsedData.value.workExperiences : [])
const educationExperiences = computed(() => Array.isArray(parsedData.value?.educationExperiences) ? parsedData.value.educationExperiences : [])
const skillNames = computed(() => {
  const skills = parsedData.value?.skills
  if (!Array.isArray(skills)) return []
  return skills.map((skill: any) => typeof skill === 'string' ? skill : skill?.name).filter(Boolean)
})
const displayName = computed(() => {
  return resume.value?.candidateName || basicInfo.value?.fullName || parsedData.value?.candidateProfile?.name || '未识别姓名'
})
const summaryText = computed(() => {
  return basicInfo.value?.summary || parsedData.value?.xaiReasoning || '当前没有解析到摘要。'
})
const parsedJson = computed(() => JSON.stringify(parsedData.value || {}, null, 2))

const statusLabel = computed(() => {
  switch (resume.value?.status) {
    case 'PARSED': return '解析已完成'
    case 'PARSING': return 'AI 提取中'
    case 'PARSE_FAILED': return '解析异常'
    default: return '等待进入队列'
  }
})
const statusHint = computed(() => {
  switch (resume.value?.status) {
    case 'PARSED': return '当前的结构化画像已可用于 HR 查看与人岗匹配。'
    case 'PARSING': return 'AI 正在消费简历上下文，请稍候。'
    case 'PARSE_FAILED': return 'AI 推理遇到问题或文档格式异常。'
    default: return '该简历已入库，但还未进行智能化大模型处理。'
  }
})
const statusClass = computed(() => {
  switch (resume.value?.status) {
    case 'PARSED': return 'border-emerald-200 bg-emerald-50 text-emerald-800 shadow-emerald-100'
    case 'PARSING': return 'border-blue-200 bg-blue-50 text-blue-800 shadow-blue-100'
    case 'PARSE_FAILED': return 'border-rose-200 bg-rose-50 text-rose-800 shadow-rose-100'
    default: return 'border-amber-200 bg-amber-50 text-amber-800 shadow-amber-100'
  }
})

const startPollingIfNeeded = () => {
  const needsPolling = resume.value?.status === 'PENDING_PARSE' || resume.value?.status === 'PARSING'
  if (!needsPolling || pollingTimer) return
  pollingTimer = setInterval(() => fetchResumeDetail(true), 3000)
}

const stopPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

const formatDateTime = (value: string) => {
  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

const fetchResumeDetail = async (silent = false) => {
  try {
    if (!silent) { loading.value = true; refreshing.value = true; errorMsg.value = ''; }
    const response: any = await api.get(`/resumes/${resumeId}`)
    resume.value = response.data
    if (resume.value?.status === 'PARSED' || resume.value?.status === 'PARSE_FAILED') stopPolling()
    else startPollingIfNeeded()
  } catch (error) {
    if (!silent) {
      errorMsg.value = resolveApiError(error, '请确认该简历存在，或网络连接正常。').summary
      resume.value = null
      stopPolling()
    }
  } finally {
    if (!silent) { loading.value = false; refreshing.value = false; }
  }
}

const refreshResumeDetail = () => void fetchResumeDetail()

const triggerParse = async () => {
  try {
    parsing.value = true
    feedback.value = null
    await api.post(`/resumes/${resumeId}/parse`)
    feedback.value = { type: 'success', message: '解析任务已投递。' }
    await fetchResumeDetail()
  } catch (error) {
    feedback.value = { type: 'error', message: resolveApiError(error, '重新投递解析失败。').summary }
  } finally {
    parsing.value = false
  }
}

onMounted(() => fetchResumeDetail())
onUnmounted(() => stopPolling())
</script>
