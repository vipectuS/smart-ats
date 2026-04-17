<template>
  <div class="relative h-full overflow-hidden">
    <div class="h-full flex flex-col p-6 space-y-6">
      <div v-if="pageFeedback" :class="pageFeedback.type === 'error' ? 'bg-rose-50 border-rose-200 text-rose-700' : 'bg-emerald-50 border-emerald-200 text-emerald-700'" class="rounded-2xl border px-5 py-4 text-sm shadow-sm">
        {{ pageFeedback.message }}
      </div>

      <div class="flex justify-between items-center bg-white/50 backdrop-blur-xl p-6 rounded-2xl border border-white/20 shadow-sm">
        <div>
          <h1 class="text-2xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-blue-600 to-indigo-600">招聘岗位</h1>
          <p class="text-sm text-gray-500 mt-1">管理所有发布的职位并查看 AI 推荐</p>
        </div>
        <div class="flex items-center gap-3">
          <div class="px-4 py-2 bg-white text-slate-600 rounded-lg shadow-sm border border-slate-200 text-sm font-medium">
            当前岗位数：{{ totalElements }}
          </div>
          <button @click="openCreatePanel" class="px-4 py-2.5 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 text-white font-medium shadow-md shadow-blue-500/20 hover:shadow-lg transition flex items-center gap-2">
            <i class="fas fa-plus"></i>
            发布岗位
          </button>
        </div>
      </div>

      <div v-if="loading" class="bg-white/60 backdrop-blur-md rounded-2xl border border-white/20 p-10 text-center text-gray-500">
        <i class="fas fa-spinner fa-spin text-2xl text-blue-500 mb-3"></i>
        <p>正在加载岗位列表...</p>
      </div>

      <div v-else-if="errorMsg" class="bg-rose-50 border border-rose-200 rounded-2xl p-6 text-center text-rose-700">
        <p class="font-semibold">岗位列表加载失败</p>
        <p class="text-sm mt-2">{{ errorMsg }}</p>
        <button @click="fetchJobs" class="mt-4 px-4 py-2 rounded-lg bg-rose-600 text-white hover:bg-rose-700 transition">重试</button>
      </div>

      <div v-else-if="jobs.length === 0" class="bg-white/60 backdrop-blur-md rounded-2xl border border-white/20 p-10 text-center text-gray-500">
        <div class="text-4xl mb-3">🗂️</div>
        <p>当前还没有发布中的岗位。</p>
        <button @click="openCreatePanel" class="mt-4 px-4 py-2 rounded-xl bg-blue-600 text-white hover:bg-blue-700 transition">创建第一个岗位</button>
      </div>

      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 overflow-y-auto pb-4">
        <div v-for="job in jobs" :key="job.id"
             @click="viewJob(job.id)"
             class="bg-white/70 backdrop-blur-md border border-white/40 shadow-sm rounded-xl p-5 hover:shadow-lg hover:-translate-y-1 transition duration-300 cursor-pointer flex flex-col">
          <div class="flex justify-between items-start mb-4 gap-4">
            <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-100 to-indigo-100 flex items-center justify-center text-blue-600 text-xl font-bold border border-blue-200 shadow-sm shrink-0">
              {{ job.title.charAt(0) }}
            </div>
            <span class="px-2.5 py-1 rounded-full text-xs font-semibold bg-green-100 text-green-700 shrink-0">
              已发布
            </span>
          </div>

          <h3 class="text-lg font-bold text-slate-800 mb-1 line-clamp-1">{{ job.title }}</h3>
          <p class="text-sm text-slate-500 mb-3">{{ getJobMeta(job) }}</p>
          <p class="text-sm text-slate-600 line-clamp-3 mb-4 leading-6">{{ job.description }}</p>

          <div class="flex flex-wrap gap-2 mb-4 min-h-[32px]">
            <span v-for="skill in getSkillTags(job).slice(0, 3)" :key="skill" class="px-2.5 py-1 rounded-full text-xs font-medium border border-blue-100 bg-blue-50 text-blue-700">
              {{ skill }}
            </span>
            <span v-if="getSkillTags(job).length > 3" class="px-2.5 py-1 rounded-full text-xs font-medium border border-slate-200 bg-slate-50 text-slate-500">
              +{{ getSkillTags(job).length - 3 }}
            </span>
          </div>

          <div class="mt-auto pt-4 border-t border-slate-100 flex justify-between items-center text-sm text-slate-600">
            <div class="flex space-x-4">
               <span title="技能要求" class="flex items-center"><i class="fas fa-users mr-1.5 text-blue-400"></i> {{ getHeadcountLabel(job) }}</span>
            </div>
            <span class="text-blue-600 font-medium hover:text-blue-700">查看推荐 &rarr;</span>
          </div>
        </div>
      </div>

      <div v-if="!loading && !errorMsg && jobs.length > 0" class="border-t border-gray-100 pt-4 flex items-center justify-between text-sm text-gray-500 bg-gray-50/50 rounded-xl px-4 py-3">
        <span>第 <strong>{{ currentPage + 1 }}</strong> / {{ totalPagesDisplay }} 页，共 <strong>{{ totalElements }}</strong> 个岗位</span>
        <div class="flex space-x-1">
          <button @click="goToPreviousPage" :disabled="currentPage === 0 || loading" class="px-3 py-1 border border-gray-200 rounded bg-white hover:bg-gray-50 disabled:text-gray-400 disabled:cursor-not-allowed">上一页</button>
          <button class="px-3 py-1 border border-gray-200 rounded bg-blue-50 text-blue-600 font-medium">{{ currentPage + 1 }}</button>
          <button @click="goToNextPage" :disabled="isLastPage || loading" class="px-3 py-1 border border-gray-200 rounded bg-white hover:bg-gray-50 disabled:text-gray-400 disabled:cursor-not-allowed">下一页</button>
        </div>
      </div>
    </div>

    <div v-if="createPanelOpen" class="absolute inset-0 z-40 flex justify-end bg-slate-950/30 backdrop-blur-sm">
      <div class="h-full w-full max-w-2xl bg-[linear-gradient(180deg,rgba(255,255,255,0.98),rgba(244,247,251,0.98))] shadow-2xl border-l border-white/50 p-6 overflow-y-auto">
        <div class="flex items-start justify-between gap-4 mb-6">
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.25em] text-blue-500">HR Workspace</p>
            <h2 class="text-2xl font-bold text-slate-900 mt-2">发布新岗位</h2>
            <p class="text-sm text-slate-500 mt-2">填写基础信息后即可生成岗位卡片，并跳转到详情页做 AI 评估。</p>
          </div>
          <button @click="closeCreatePanel" class="w-10 h-10 rounded-full border border-slate-200 bg-white text-slate-500 hover:text-slate-700 hover:border-slate-300 transition">
            <i class="fas fa-times"></i>
          </button>
        </div>

        <div v-if="createError" class="mb-5 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {{ createError }}
        </div>

        <form class="space-y-6" @submit.prevent="submitCreateJob">
          <section class="rounded-2xl border border-white/60 bg-white/80 p-5 shadow-sm">
            <h3 class="text-sm font-semibold text-slate-800 mb-4">岗位主信息</h3>
            <div class="grid grid-cols-1 gap-4">
              <label class="space-y-2">
                <span class="text-sm font-medium text-slate-700">岗位名称</span>
                <input v-model="jobForm.title" type="text" maxlength="255" placeholder="例如：高级前端工程师 / AI 产品经理" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100">
              </label>
              <label class="space-y-2">
                <span class="text-sm font-medium text-slate-700">岗位描述</span>
                <textarea v-model="jobForm.description" rows="6" placeholder="描述岗位职责、业务背景、核心目标和团队协作方式。" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100"></textarea>
              </label>
            </div>
          </section>

          <section class="rounded-2xl border border-white/60 bg-white/80 p-5 shadow-sm">
            <h3 class="text-sm font-semibold text-slate-800 mb-4">结构化要求</h3>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <label class="space-y-2">
                <span class="text-sm font-medium text-slate-700">部门</span>
                <input v-model="jobForm.department" type="text" placeholder="例如：平台研发中心" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100">
              </label>
              <label class="space-y-2">
                <span class="text-sm font-medium text-slate-700">工作地点</span>
                <input v-model="jobForm.location" type="text" placeholder="例如：上海 / 杭州 / Remote" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100">
              </label>
              <label class="space-y-2">
                <span class="text-sm font-medium text-slate-700">招聘人数</span>
                <input v-model="jobForm.headcount" type="number" min="1" max="999" placeholder="1" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100">
              </label>
              <label class="space-y-2">
                <span class="text-sm font-medium text-slate-700">职级</span>
                <input v-model="jobForm.seniority" type="text" placeholder="例如：P6 / Senior / Staff" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100">
              </label>
              <label class="space-y-2">
                <span class="text-sm font-medium text-slate-700">用工形式</span>
                <select v-model="jobForm.employmentType" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100">
                  <option value="">请选择</option>
                  <option value="全职">全职</option>
                  <option value="实习">实习</option>
                  <option value="校招">校招</option>
                  <option value="外包/合同">外包/合同</option>
                </select>
              </label>
              <label class="space-y-2">
                <span class="text-sm font-medium text-slate-700">学历要求</span>
                <input v-model="jobForm.education" type="text" placeholder="例如：本科及以上" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100">
              </label>
              <label class="space-y-2 md:col-span-2">
                <span class="text-sm font-medium text-slate-700">薪资区间</span>
                <input v-model="jobForm.salaryRange" type="text" placeholder="例如：30k-45k x 15" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100">
              </label>
            </div>
          </section>

          <section class="rounded-2xl border border-white/60 bg-white/80 p-5 shadow-sm space-y-4">
            <div>
              <h3 class="text-sm font-semibold text-slate-800">关键技能与亮点</h3>
              <p class="text-xs text-slate-500 mt-1">技能会直接写入岗位 requirements，供后续推荐和语义匹配使用。</p>
            </div>

            <div class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
              <div class="flex flex-col sm:flex-row gap-3">
                <input
                  v-model="skillInput"
                  @keydown.enter.prevent="addSkillsFromInput(skillInput)"
                  type="text"
                  placeholder="输入技能后回车，支持逗号批量录入，如 Vue, TypeScript, ECharts"
                  class="flex-1 rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100"
                >
                <button type="button" @click="addSkillsFromInput(skillInput)" class="rounded-xl bg-slate-900 px-4 py-3 text-sm font-medium text-white hover:bg-slate-800 transition">
                  添加技能
                </button>
              </div>

              <div class="flex flex-wrap gap-2 mt-4 min-h-[32px]">
                <button v-for="skill in jobForm.skills" :key="skill" type="button" @click="removeSkill(skill)" class="inline-flex items-center gap-2 rounded-full border border-blue-100 bg-blue-50 px-3 py-1.5 text-xs font-medium text-blue-700 hover:bg-blue-100 transition">
                  <span>{{ skill }}</span>
                  <i class="fas fa-times text-[10px]"></i>
                </button>
                <span v-if="jobForm.skills.length === 0" class="text-xs text-slate-400 py-2">尚未添加技能标签</span>
              </div>
            </div>

            <label class="space-y-2 block">
              <span class="text-sm font-medium text-slate-700">补充亮点</span>
              <textarea v-model="jobForm.highlights" rows="4" placeholder="输入额外要求或亮点，每行一条，例如：
具备低代码平台经验
熟悉 AI 应用落地
有跨团队协作背景" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100"></textarea>
            </label>
          </section>

          <section class="rounded-2xl border border-indigo-100 bg-indigo-50/70 p-5 shadow-sm">
            <div class="flex items-center justify-between gap-4 mb-4">
              <div>
                <h3 class="text-sm font-semibold text-indigo-900">提交预览</h3>
                <p class="text-xs text-indigo-700/80 mt-1">将发送给后端的 requirements 会按已填写字段自动整理。</p>
              </div>
              <span class="rounded-full bg-white/80 px-3 py-1 text-xs font-semibold text-indigo-700 border border-indigo-100">{{ requirementEntries.length }} 个字段</span>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">
              <div v-for="entry in requirementEntries" :key="entry.key" class="rounded-xl border border-white/60 bg-white/80 px-4 py-3">
                <p class="text-xs uppercase tracking-wide text-slate-400">{{ entry.key }}</p>
                <p class="mt-1 text-slate-700 break-words">{{ entry.value }}</p>
              </div>
              <div v-if="requirementEntries.length === 0" class="rounded-xl border border-dashed border-indigo-200 bg-white/70 px-4 py-6 text-center text-indigo-700 md:col-span-2">
                当前还没有可提交的 requirements 字段，请至少补充一项结构化要求。
              </div>
            </div>
          </section>

          <div class="flex items-center justify-end gap-3 pt-2">
            <button type="button" @click="closeCreatePanel" class="px-4 py-2.5 rounded-xl border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 transition">取消</button>
            <button type="submit" :disabled="submitting" class="px-5 py-2.5 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 text-white font-medium shadow-md shadow-blue-500/20 hover:shadow-lg transition disabled:opacity-70 disabled:cursor-not-allowed flex items-center gap-2">
              <i :class="submitting ? 'fas fa-spinner fa-spin' : 'fas fa-paper-plane'"></i>
              {{ submitting ? '发布中...' : '确认发布并进入详情' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../utils/api'

interface JobRequirements {
  location?: string
  department?: string
  headcount?: number
  skills?: string[]
  seniority?: string
  employmentType?: string
  education?: string
  salaryRange?: string
  highlights?: string[]
}

interface JobSummary {
  id: string
  title: string
  description: string
  requirements?: JobRequirements | null
  createdBy?: {
    username?: string
  } | null
}

interface RequirementEntry {
  key: string
  value: string
}

const router = useRouter()
const jobs = ref<JobSummary[]>([])
const loading = ref(true)
const errorMsg = ref('')
const currentPage = ref(0)
const pageSize = ref(6)
const totalElements = ref(0)
const totalPages = ref(0)
const createPanelOpen = ref(false)
const createError = ref('')
const submitting = ref(false)
const skillInput = ref('')
const pageFeedback = ref<{ type: 'success' | 'error'; message: string } | null>(null)

const jobForm = reactive({
  title: '',
  description: '',
  department: '',
  location: '',
  headcount: '1',
  seniority: '',
  employmentType: '',
  education: '',
  salaryRange: '',
  skills: [] as string[],
  highlights: '',
})

const isLastPage = computed(() => totalPages.value === 0 || currentPage.value >= totalPages.value - 1)
const totalPagesDisplay = computed(() => Math.max(totalPages.value, 1))
const normalizedRequirements = computed<JobRequirements>(() => {
  const requirements: JobRequirements = {}

  if (jobForm.department.trim()) {
    requirements.department = jobForm.department.trim()
  }
  if (jobForm.location.trim()) {
    requirements.location = jobForm.location.trim()
  }

  const parsedHeadcount = Number(jobForm.headcount)
  if (Number.isFinite(parsedHeadcount) && parsedHeadcount > 0) {
    requirements.headcount = parsedHeadcount
  }

  if (jobForm.seniority.trim()) {
    requirements.seniority = jobForm.seniority.trim()
  }
  if (jobForm.employmentType.trim()) {
    requirements.employmentType = jobForm.employmentType.trim()
  }
  if (jobForm.education.trim()) {
    requirements.education = jobForm.education.trim()
  }
  if (jobForm.salaryRange.trim()) {
    requirements.salaryRange = jobForm.salaryRange.trim()
  }
  if (jobForm.skills.length > 0) {
    requirements.skills = [...jobForm.skills]
  }

  const highlightList = jobForm.highlights
    .split(/\n+/)
    .map((item) => item.trim())
    .filter(Boolean)
  if (highlightList.length > 0) {
    requirements.highlights = highlightList
  }

  return requirements
})

const requirementEntries = computed<RequirementEntry[]>(() => {
  return Object.entries(normalizedRequirements.value).map(([key, value]) => ({
    key,
    value: Array.isArray(value) ? value.join(' / ') : String(value),
  }))
})

const getJobMeta = (job: JobSummary) => {
  const requirements = job.requirements || {}
  const location = requirements.location || '地点未设置'
  const department = requirements.department || (job.createdBy?.username ? `发布者: ${job.createdBy.username}` : '未标注部门')
  return `${department} • ${location}`
}

const getSkillTags = (job: JobSummary) => {
  const skills = job.requirements?.skills
  return Array.isArray(skills) ? skills.filter((skill) => typeof skill === 'string' && skill.trim()) : []
}

const getHeadcountLabel = (job: JobSummary) => {
  const headcount = job.requirements?.headcount
  if (headcount) {
    return `${headcount} 人`
  }
  const skills = getSkillTags(job)
  if (skills.length > 0) {
    return `${skills.length} 项技能`
  }
  return '未设置'
}

const resetJobForm = () => {
  jobForm.title = ''
  jobForm.description = ''
  jobForm.department = ''
  jobForm.location = ''
  jobForm.headcount = '1'
  jobForm.seniority = ''
  jobForm.employmentType = ''
  jobForm.education = ''
  jobForm.salaryRange = ''
  jobForm.skills = []
  jobForm.highlights = ''
  skillInput.value = ''
  createError.value = ''
}

const openCreatePanel = () => {
  createPanelOpen.value = true
  createError.value = ''
}

const closeCreatePanel = () => {
  createPanelOpen.value = false
  createError.value = ''
}

const normalizeSkillToken = (value: string) => value.trim().replace(/\s+/g, ' ')

const addSkillsFromInput = (rawValue: string) => {
  const tokens = rawValue
    .split(/[，,\n]/)
    .map(normalizeSkillToken)
    .filter(Boolean)

  if (tokens.length === 0) {
    return
  }

  const existing = new Set(jobForm.skills.map((skill) => skill.toLowerCase()))
  for (const token of tokens) {
    if (!existing.has(token.toLowerCase())) {
      jobForm.skills.push(token)
      existing.add(token.toLowerCase())
    }
  }

  skillInput.value = ''
}

const removeSkill = (skillToRemove: string) => {
  jobForm.skills = jobForm.skills.filter((skill) => skill !== skillToRemove)
}

const fetchJobs = async () => {
  try {
    loading.value = true
    errorMsg.value = ''
    const response: any = await api.get('/jobs', {
      params: {
        page: currentPage.value,
        size: pageSize.value,
      },
    })
    jobs.value = response.data?.content || []
    totalElements.value = response.data?.totalElements || 0
    totalPages.value = response.data?.totalPages || 0
  } catch (error) {
    console.error('Failed to fetch jobs', error)
    errorMsg.value = '请检查岗位接口或登录状态。'
    jobs.value = []
  } finally {
    loading.value = false
  }
}

const submitCreateJob = async () => {
  const title = jobForm.title.trim()
  const description = jobForm.description.trim()
  const requirements = normalizedRequirements.value

  if (!title) {
    createError.value = '岗位名称不能为空。'
    return
  }
  if (!description) {
    createError.value = '岗位描述不能为空。'
    return
  }
  if (Object.keys(requirements).length === 0) {
    createError.value = '请至少填写一项结构化要求，例如地点、技能或招聘人数。'
    return
  }

  try {
    submitting.value = true
    createError.value = ''
    const response: any = await api.post('/jobs', {
      title,
      description,
      requirements,
    })

    const createdJobId = response.data?.id
    pageFeedback.value = { type: 'success', message: `岗位“${title}”已发布，正在进入详情页。` }
    currentPage.value = 0
    await fetchJobs()
    closeCreatePanel()
    resetJobForm()

    if (createdJobId) {
      router.push(`/jobs/${createdJobId}`)
    }
  } catch (error: any) {
    console.error('Failed to create job', error)
    createError.value = error.response?.data?.message || '岗位发布失败，请检查登录状态或稍后重试。'
  } finally {
    submitting.value = false
  }
}

const viewJob = (id: string) => {
  router.push(`/jobs/${id}`)
}

const goToPreviousPage = async () => {
  if (currentPage.value === 0) return
  currentPage.value -= 1
  await fetchJobs()
}

const goToNextPage = async () => {
  if (isLastPage.value) return
  currentPage.value += 1
  await fetchJobs()
}

onMounted(() => {
  fetchJobs()
})
</script>
