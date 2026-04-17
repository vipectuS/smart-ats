<template>
  <div class="h-full overflow-y-auto bg-gray-50 p-8">
    <div class="mx-auto max-w-7xl space-y-8">
      <div v-if="feedback" :class="feedback.type === 'error' ? 'border-rose-200 bg-rose-50 text-rose-700' : 'border-emerald-200 bg-emerald-50 text-emerald-700'" class="rounded-2xl border px-5 py-4 text-sm shadow-sm">
        {{ feedback.message }}
      </div>

      <div v-if="loading" class="rounded-2xl border border-white/20 bg-white/70 p-10 text-center text-gray-500 shadow-sm">
        <i class="fas fa-spinner fa-spin text-2xl text-blue-500 mb-3"></i>
        <p>正在加载简历详情...</p>
      </div>

      <div v-else-if="errorMsg" class="rounded-2xl border border-rose-200 bg-rose-50 p-6 text-center text-rose-700 shadow-sm">
        <p class="font-semibold">简历详情加载失败</p>
        <p class="mt-2 text-sm">{{ errorMsg }}</p>
        <button @click="refreshResumeDetail" class="mt-4 rounded-lg bg-rose-600 px-4 py-2 text-white transition hover:bg-rose-700">重试</button>
      </div>

      <template v-else-if="resume">
        <header class="rounded-2xl border border-white/20 bg-white/70 p-8 shadow-sm backdrop-blur-md">
          <div class="flex flex-col gap-6 xl:flex-row xl:items-start xl:justify-between">
            <div class="space-y-4">
              <button @click="router.push({ name: 'resumes' })" class="inline-flex items-center text-sm font-medium text-slate-500 transition hover:text-blue-600">
                <i class="fas fa-arrow-left mr-2"></i>
                返回简历库
              </button>
              <div>
                <h1 class="text-3xl font-bold text-slate-900">{{ displayName }}</h1>
                <p class="mt-2 text-sm text-slate-500 break-all">{{ resume.rawContentReference }}</p>
              </div>
              <div class="flex flex-wrap gap-3 text-sm text-slate-600">
                <span class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1">联系方式：{{ resume.contactInfo || '未提供' }}</span>
                <span class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1">更新时间：{{ formatDateTime(resume.updatedAt) }}</span>
                <span class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1">入库时间：{{ formatDateTime(resume.createdAt) }}</span>
              </div>
            </div>

            <div class="flex min-w-[260px] flex-col gap-3">
              <div :class="statusClass" class="rounded-2xl border px-5 py-4 text-center shadow-sm">
                <p class="text-xs font-semibold uppercase tracking-[0.24em]">当前状态</p>
                <p class="mt-3 text-2xl font-black">{{ statusLabel }}</p>
                <p class="mt-2 text-sm opacity-80">{{ statusHint }}</p>
              </div>
              <div class="flex gap-3">
                <button @click="refreshResumeDetail" :disabled="refreshing" class="flex-1 rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 disabled:opacity-70 disabled:cursor-not-allowed">
                  {{ refreshing ? '刷新中...' : '刷新状态' }}
                </button>
                <button v-if="resume.status !== 'PARSED'" @click="triggerParse" :disabled="parsing" class="flex-1 rounded-xl bg-blue-600 px-4 py-3 text-sm font-semibold text-white shadow-md shadow-blue-500/20 transition hover:bg-blue-700 disabled:opacity-70 disabled:cursor-not-allowed">
                  {{ parsing ? '投递中...' : '重新投递解析' }}
                </button>
              </div>
            </div>
          </div>
        </header>

        <div class="grid grid-cols-1 gap-8 xl:grid-cols-[1.15fr_0.85fr]">
          <section class="space-y-8">
            <div class="rounded-2xl border border-white/20 bg-white/80 p-8 shadow-sm">
              <div class="flex items-center justify-between gap-4">
                <div>
                  <h2 class="text-xl font-bold text-slate-800">解析摘要</h2>
                  <p class="mt-1 text-sm text-slate-500">展示当前已结构化的候选人画像，便于 HR 快速判断简历质量。</p>
                </div>
                <span class="rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">Talent Profile</span>
              </div>

              <div v-if="hasParsedData" class="mt-6 space-y-6">
                <div class="rounded-2xl border border-slate-200 bg-slate-50/70 p-5">
                  <p class="text-sm font-semibold text-slate-700">候选人摘要</p>
                  <p class="mt-2 text-sm leading-7 text-slate-600">{{ summaryText }}</p>
                </div>

                <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
                  <div class="rounded-2xl border border-slate-200 bg-white p-5">
                    <p class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Headline</p>
                    <p class="mt-2 text-sm font-medium text-slate-800">{{ basicInfo.headline || '未提取到职位概述' }}</p>
                  </div>
                  <div class="rounded-2xl border border-slate-200 bg-white p-5">
                    <p class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Location</p>
                    <p class="mt-2 text-sm font-medium text-slate-800">{{ basicInfo.location || '未提取到地点' }}</p>
                  </div>
                </div>

                <div>
                  <p class="mb-3 text-sm font-semibold text-slate-700">技能标签</p>
                  <div class="flex flex-wrap gap-2">
                    <span v-for="skill in skillNames" :key="skill" class="rounded-full border border-blue-200 bg-blue-50 px-3 py-1 text-xs font-medium text-blue-700">
                      {{ skill }}
                    </span>
                    <span v-if="skillNames.length === 0" class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-medium text-slate-500">
                      当前没有解析到技能标签
                    </span>
                  </div>
                </div>

                <div>
                  <p class="mb-3 text-sm font-semibold text-slate-700">工作经历</p>
                  <div class="space-y-4">
                    <article v-for="(item, index) in workExperiences" :key="`${item.company || 'company'}-${index}`" class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                      <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                        <div>
                          <h3 class="text-sm font-semibold text-slate-800">{{ item.title || '未命名岗位' }}</h3>
                          <p class="mt-1 text-xs text-slate-500">{{ item.company || '未知公司' }}</p>
                        </div>
                        <span class="rounded-full bg-slate-50 px-3 py-1 text-xs font-medium text-slate-500">
                          {{ [item.startDate, item.endDate].filter(Boolean).join(' - ') || '时间未提取' }}
                        </span>
                      </div>
                      <ul v-if="item.responsibilities?.length" class="mt-4 space-y-2 text-sm text-slate-600">
                        <li v-for="responsibility in item.responsibilities" :key="responsibility">{{ responsibility }}</li>
                      </ul>
                      <p v-else class="mt-4 text-sm text-slate-500">当前没有解析到职责详情。</p>
                    </article>
                    <div v-if="workExperiences.length === 0" class="rounded-2xl border border-dashed border-slate-200 bg-slate-50/70 p-8 text-center text-slate-500">
                      当前没有解析到工作经历。
                    </div>
                  </div>
                </div>

                <div>
                  <p class="mb-3 text-sm font-semibold text-slate-700">教育经历</p>
                  <div class="space-y-4">
                    <article v-for="(item, index) in educationExperiences" :key="`${item.school || 'school'}-${index}`" class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                      <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                        <div>
                          <h3 class="text-sm font-semibold text-slate-800">{{ item.school || '未知院校' }}</h3>
                          <p class="mt-1 text-xs text-slate-500">{{ [item.degree, item.fieldOfStudy].filter(Boolean).join(' / ') || '学历字段未提取' }}</p>
                        </div>
                        <span class="rounded-full bg-slate-50 px-3 py-1 text-xs font-medium text-slate-500">
                          {{ [item.startDate, item.endDate].filter(Boolean).join(' - ') || '时间未提取' }}
                        </span>
                      </div>
                    </article>
                    <div v-if="educationExperiences.length === 0" class="rounded-2xl border border-dashed border-slate-200 bg-slate-50/70 p-8 text-center text-slate-500">
                      当前没有解析到教育经历。
                    </div>
                  </div>
                </div>
              </div>

              <div v-else class="mt-6 rounded-2xl border border-dashed border-slate-200 bg-slate-50/70 p-8 text-center text-slate-500">
                <div class="mb-3 text-4xl">🧠</div>
                <p class="font-semibold text-slate-700">当前还没有可展示的解析画像</p>
                <p class="mt-2 text-sm">如果简历仍处于待解析或解析异常状态，可以在右上角重新投递解析。</p>
              </div>
            </div>

            <div class="rounded-2xl border border-white/20 bg-white/80 p-8 shadow-sm">
              <div class="flex items-center justify-between gap-4">
                <div>
                  <h2 class="text-xl font-bold text-slate-800">原始结构化 JSON</h2>
                  <p class="mt-1 text-sm text-slate-500">保留一份原始解析结果，方便答辩演示和调试字段映射。</p>
                </div>
              </div>
              <pre class="mt-6 overflow-auto rounded-2xl bg-slate-950 p-5 text-xs leading-6 text-slate-100">{{ parsedJson }}</pre>
            </div>
          </section>

          <section class="space-y-8">
            <div class="rounded-2xl border border-white/20 bg-white/80 p-8 shadow-sm">
              <h2 class="text-xl font-bold text-slate-800">简历元信息</h2>
              <dl class="mt-6 space-y-4 text-sm text-slate-600">
                <div>
                  <dt class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Resume ID</dt>
                  <dd class="mt-1 break-all font-medium text-slate-800">{{ resume.id }}</dd>
                </div>
                <div>
                  <dt class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Raw Reference</dt>
                  <dd class="mt-1 break-all font-medium text-slate-800">{{ resume.rawContentReference }}</dd>
                </div>
                <div>
                  <dt class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Candidate Name</dt>
                  <dd class="mt-1 font-medium text-slate-800">{{ displayName }}</dd>
                </div>
                <div>
                  <dt class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Contact</dt>
                  <dd class="mt-1 font-medium text-slate-800">{{ resume.contactInfo || '未提供' }}</dd>
                </div>
              </dl>
            </div>

            <div class="rounded-2xl border border-white/20 bg-white/80 p-8 shadow-sm">
              <h2 class="text-xl font-bold text-slate-800">状态说明</h2>
              <div class="mt-5 space-y-4 text-sm text-slate-600">
                <div class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
                  <p class="font-semibold text-slate-700">当前阶段</p>
                  <p class="mt-1">{{ statusHint }}</p>
                </div>
                <div v-if="resume.parseFailureReason" class="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-rose-700">
                  <p class="font-semibold">失败原因</p>
                  <p class="mt-1">{{ resume.parseFailureReason }}</p>
                </div>
                <ul class="space-y-2">
                  <li>如果状态长期停留在待解析或解析中，优先检查 AI 服务消费者是否正常运行。</li>
                  <li>如果当前已解析完成，可以直接把这页作为“结构化画像结果”演示入口。</li>
                  <li>如果 JSON 结构与前端展示有偏差，优先以本页的原始结构化结果为准回溯字段映射。</li>
                </ul>
              </div>
            </div>
          </section>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../utils/api'

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
  if (!Array.isArray(skills)) {
    return []
  }

  return skills
    .map((skill: any) => typeof skill === 'string' ? skill : skill?.name)
    .filter((skill: string | null | undefined) => Boolean(skill))
})
const displayName = computed(() => {
  return resume.value?.candidateName
    || basicInfo.value?.fullName
    || parsedData.value?.candidateProfile?.name
    || '未识别候选人姓名'
})
const summaryText = computed(() => {
  return basicInfo.value?.summary
    || parsedData.value?.xaiReasoning
    || '当前没有解析到候选人摘要。'
})
const parsedJson = computed(() => JSON.stringify(parsedData.value || {}, null, 2))
const statusLabel = computed(() => {
  switch (resume.value?.status) {
    case 'PARSED':
      return '解析完成'
    case 'PARSING':
      return 'AI 解析中'
    case 'PARSE_FAILED':
      return '解析异常'
    default:
      return '等待解析'
  }
})
const statusHint = computed(() => {
  switch (resume.value?.status) {
    case 'PARSED':
      return '当前结构化画像已可用于 HR 查看、匹配和解释展示。'
    case 'PARSING':
      return 'AI 服务正在消费该简历，页面会自动轮询最新状态。'
    case 'PARSE_FAILED':
      return '最近一次解析失败，可查看失败原因后重新投递解析。'
    default:
      return '该简历已入库，但还未进入稳定的结构化解析完成态。'
  }
})
const statusClass = computed(() => {
  switch (resume.value?.status) {
    case 'PARSED':
      return 'border-emerald-200 bg-emerald-50 text-emerald-800'
    case 'PARSING':
      return 'border-blue-200 bg-blue-50 text-blue-800'
    case 'PARSE_FAILED':
      return 'border-rose-200 bg-rose-50 text-rose-800'
    default:
      return 'border-amber-200 bg-amber-50 text-amber-800'
  }
})

const startPollingIfNeeded = () => {
  const needsPolling = resume.value?.status === 'PENDING_PARSE' || resume.value?.status === 'PARSING'
  if (!needsPolling || pollingTimer) {
    return
  }

  pollingTimer = setInterval(() => {
    fetchResumeDetail(true)
  }, 3000)
}

const stopPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

const formatDateTime = (value: string) => {
  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const fetchResumeDetail = async (silent = false) => {
  try {
    if (!silent) {
      loading.value = true
      refreshing.value = true
      errorMsg.value = ''
    }
    const response: any = await api.get(`/resumes/${resumeId}`)
    resume.value = response.data

    if (resume.value?.status === 'PARSED' || resume.value?.status === 'PARSE_FAILED') {
      stopPolling()
    } else {
      startPollingIfNeeded()
    }
  } catch (error) {
    console.error('Failed to fetch resume detail', error)
    if (!silent) {
      errorMsg.value = '请确认该简历存在，且当前登录角色有权限查看。'
      resume.value = null
      stopPolling()
    }
  } finally {
    if (!silent) {
      loading.value = false
      refreshing.value = false
    }
  }
}

const refreshResumeDetail = () => {
  void fetchResumeDetail()
}

const triggerParse = async () => {
  try {
    parsing.value = true
    feedback.value = null
    await api.post(`/resumes/${resumeId}/parse`)
    feedback.value = { type: 'success', message: '解析任务已重新投递，页面会自动刷新最新状态。' }
    await fetchResumeDetail()
  } catch (error) {
    console.error('Failed to trigger resume parse', error)
    feedback.value = { type: 'error', message: '重新投递解析失败，请稍后重试。' }
  } finally {
    parsing.value = false
  }
}

onMounted(() => {
  fetchResumeDetail()
})

onUnmounted(() => {
  stopPolling()
})
</script>