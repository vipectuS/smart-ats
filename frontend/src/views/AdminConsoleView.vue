<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import api from '@/utils/api'
import type { AdminOverview, AdminParseFailure, AdminSkill, AdminSkillUpsertPayload } from '@/types/admin'

const loading = ref(true)
const saving = ref(false)
const errorMsg = ref('')
const saveMsg = ref('')
const overview = ref<AdminOverview | null>(null)
const parseFailures = ref<AdminParseFailure[]>([])
const skills = ref<AdminSkill[]>([])
const editingSkillId = ref<string | null>(null)

const skillForm = reactive({
  name: '',
  category: '',
  aliases: '',
  enabled: true,
})

const metricCards = computed(() => {
  if (!overview.value) return []

  return [
    {
      label: '系统用户',
      value: overview.value.totals.totalUsers,
      tone: 'from-sky-500 to-cyan-500',
      note: '覆盖 HR、候选人、管理员账号',
    },
    {
      label: '职位总量',
      value: overview.value.totals.totalJobs,
      tone: 'from-indigo-500 to-blue-500',
      note: '当前数据库中的岗位记录',
    },
    {
      label: '简历总量',
      value: overview.value.totals.totalResumes,
      tone: 'from-emerald-500 to-teal-500',
      note: '含待解析、已解析与失败样本',
    },
    {
      label: '技能词条',
      value: overview.value.totals.totalSkillEntries,
      tone: 'from-amber-500 to-orange-500',
      note: '管理员可维护的规范技能字典',
    },
  ]
})

const userRoleMax = computed(() => Math.max(...(overview.value?.usersByRole.map((item) => item.value) ?? [1])))
const resumeStatusMax = computed(() => Math.max(...(overview.value?.resumesByStatus.map((item) => item.value) ?? [1])))

const loadData = async () => {
  loading.value = true
  errorMsg.value = ''

  try {
    const [overviewResponse, skillsResponse, failuresResponse] = await Promise.all([
      api.get('/admin/overview'),
      api.get('/admin/skills'),
      api.get('/admin/parse-failures', { params: { limit: 12 } }),
    ])

    overview.value = overviewResponse.data as AdminOverview
    skills.value = skillsResponse.data as AdminSkill[]
    parseFailures.value = failuresResponse.data as AdminParseFailure[]
  } catch (error: any) {
    console.error('Failed to load admin console data', error)
    errorMsg.value = error.response?.data?.message || '管理员控制台加载失败，请检查后端接口与登录角色。'
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  editingSkillId.value = null
  skillForm.name = ''
  skillForm.category = ''
  skillForm.aliases = ''
  skillForm.enabled = true
  saveMsg.value = ''
}

const editSkill = (skill: AdminSkill) => {
  editingSkillId.value = skill.id
  skillForm.name = skill.name
  skillForm.category = skill.category || ''
  skillForm.aliases = skill.aliases.join(', ')
  skillForm.enabled = skill.enabled
  saveMsg.value = ''
}

const buildPayload = (): AdminSkillUpsertPayload => ({
  name: skillForm.name.trim(),
  category: skillForm.category.trim() || null,
  aliases: skillForm.aliases
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean),
  enabled: skillForm.enabled,
})

const submitSkill = async () => {
  saving.value = true
  errorMsg.value = ''
  saveMsg.value = ''

  try {
    const payload = buildPayload()
    if (editingSkillId.value) {
      await api.put(`/admin/skills/${editingSkillId.value}`, payload)
      saveMsg.value = '技能词条已更新。'
    } else {
      await api.post('/admin/skills', payload)
      saveMsg.value = '技能词条已创建。'
    }

    await loadData()
    resetForm()
  } catch (error: any) {
    console.error('Failed to save admin skill', error)
    errorMsg.value = error.response?.data?.message || '保存技能词条失败。'
  } finally {
    saving.value = false
  }
}

const formatTime = (value: string) => new Date(value).toLocaleString('zh-CN')
const widthPercent = (value: number, max: number) => `${Math.max(10, Math.round((value / Math.max(max, 1)) * 100))}%`

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="min-h-full bg-slate-100/70 px-6 py-8">
    <div class="mx-auto flex max-w-7xl flex-col gap-6">
      <section class="overflow-hidden rounded-[28px] border border-slate-200/70 bg-[radial-gradient(circle_at_top_left,_rgba(14,165,233,0.2),_transparent_35%),linear-gradient(135deg,_#0f172a,_#1e3a8a_55%,_#0f766e)] p-8 text-white shadow-xl shadow-slate-300/40">
        <div class="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div class="max-w-2xl">
            <p class="text-sm uppercase tracking-[0.28em] text-cyan-100/80">Admin Console</p>
            <h1 class="mt-3 text-3xl font-black leading-tight">系统运营、技能治理与解析失败闭环</h1>
            <p class="mt-3 text-sm text-cyan-50/85">
              当前页直接对接管理员后端接口，覆盖系统概览、技能词典维护和解析失败样本追踪，作为 Phase 5 到 Phase 7 的最小真实管理台。
            </p>
          </div>
          <div class="grid grid-cols-2 gap-3 text-sm text-cyan-50/90 sm:grid-cols-4">
            <div class="rounded-2xl border border-white/15 bg-white/10 px-4 py-3 backdrop-blur">
              <div class="text-cyan-100/75">接口数量</div>
              <div class="mt-1 text-2xl font-bold">4</div>
            </div>
            <div class="rounded-2xl border border-white/15 bg-white/10 px-4 py-3 backdrop-blur">
              <div class="text-cyan-100/75">失败追踪</div>
              <div class="mt-1 text-2xl font-bold">实时</div>
            </div>
            <div class="rounded-2xl border border-white/15 bg-white/10 px-4 py-3 backdrop-blur">
              <div class="text-cyan-100/75">词典维护</div>
              <div class="mt-1 text-2xl font-bold">在线</div>
            </div>
            <div class="rounded-2xl border border-white/15 bg-white/10 px-4 py-3 backdrop-blur">
              <div class="text-cyan-100/75">刷新方式</div>
              <div class="mt-1 text-2xl font-bold">即时</div>
            </div>
          </div>
        </div>
      </section>

      <div v-if="errorMsg" class="rounded-2xl border border-rose-200 bg-rose-50 px-5 py-4 text-rose-700 shadow-sm">
        <div class="flex items-center justify-between gap-4">
          <div>
            <p class="font-semibold">管理台数据异常</p>
            <p class="mt-1 text-sm">{{ errorMsg }}</p>
          </div>
          <button class="rounded-xl bg-rose-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-rose-700" @click="loadData">重试</button>
        </div>
      </div>

      <div v-if="loading" class="rounded-3xl border border-slate-200 bg-white px-6 py-16 text-center text-slate-500 shadow-sm">
        正在同步管理员视图数据...
      </div>

      <template v-else>
        <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <article v-for="card in metricCards" :key="card.label" class="overflow-hidden rounded-3xl border border-white/50 bg-white p-5 shadow-sm">
            <div class="h-2 rounded-full bg-slate-100">
              <div class="h-2 rounded-full bg-gradient-to-r" :class="card.tone"></div>
            </div>
            <p class="mt-4 text-sm font-medium text-slate-500">{{ card.label }}</p>
            <p class="mt-2 text-4xl font-black text-slate-900">{{ card.value }}</p>
            <p class="mt-2 text-sm text-slate-500">{{ card.note }}</p>
          </article>
        </section>

        <section class="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
          <article class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
            <div class="flex items-center justify-between gap-4">
              <div>
                <h2 class="text-xl font-bold text-slate-900">技能词典治理</h2>
                <p class="mt-1 text-sm text-slate-500">规范技能名称、别名与启停状态，供后续评估与演示数据统一使用。</p>
              </div>
              <button class="rounded-xl border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-50" @click="resetForm">新建词条</button>
            </div>

            <form class="mt-6 grid gap-4 rounded-3xl bg-slate-50/90 p-5" @submit.prevent="submitSkill">
              <div class="grid gap-4 md:grid-cols-2">
                <label class="text-sm font-medium text-slate-700">
                  技能名称
                  <input v-model="skillForm.name" required class="mt-2 w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 outline-none transition focus:border-sky-400 focus:ring-2 focus:ring-sky-100" placeholder="例如 Kotlin" />
                </label>
                <label class="text-sm font-medium text-slate-700">
                  分类
                  <input v-model="skillForm.category" class="mt-2 w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 outline-none transition focus:border-sky-400 focus:ring-2 focus:ring-sky-100" placeholder="例如 backend / frontend" />
                </label>
              </div>

              <label class="text-sm font-medium text-slate-700">
                别名
                <input v-model="skillForm.aliases" class="mt-2 w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 outline-none transition focus:border-sky-400 focus:ring-2 focus:ring-sky-100" placeholder="用英文逗号分隔，例如 TS, Node TS" />
              </label>

              <label class="inline-flex items-center gap-3 text-sm font-medium text-slate-700">
                <input v-model="skillForm.enabled" type="checkbox" class="h-4 w-4 rounded border-slate-300 text-sky-600 focus:ring-sky-500" />
                当前词条启用
              </label>

              <div class="flex flex-wrap items-center gap-3">
                <button :disabled="saving" class="rounded-2xl bg-sky-600 px-5 py-3 text-sm font-semibold text-white transition hover:bg-sky-700 disabled:cursor-not-allowed disabled:bg-slate-400">
                  {{ saving ? '保存中...' : editingSkillId ? '更新词条' : '创建词条' }}
                </button>
                <button v-if="editingSkillId" type="button" class="rounded-2xl border border-slate-200 px-5 py-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-100" @click="resetForm">取消编辑</button>
                <span v-if="saveMsg" class="text-sm font-medium text-emerald-600">{{ saveMsg }}</span>
              </div>
            </form>

            <div class="mt-6 space-y-3">
              <div v-if="skills.length === 0" class="rounded-2xl border border-dashed border-slate-200 px-4 py-8 text-center text-sm text-slate-500">
                还没有技能词条，可以先创建演示词典。
              </div>

              <article v-for="skill in skills" :key="skill.id" class="rounded-2xl border border-slate-200 px-4 py-4 transition hover:border-sky-200 hover:shadow-sm">
                <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                  <div>
                    <div class="flex flex-wrap items-center gap-2">
                      <h3 class="text-lg font-bold text-slate-900">{{ skill.name }}</h3>
                      <span class="rounded-full px-3 py-1 text-xs font-semibold" :class="skill.enabled ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-200 text-slate-600'">
                        {{ skill.enabled ? '启用中' : '已停用' }}
                      </span>
                      <span v-if="skill.category" class="rounded-full bg-sky-100 px-3 py-1 text-xs font-semibold text-sky-700">{{ skill.category }}</span>
                    </div>
                    <p class="mt-2 text-sm text-slate-500">更新时间：{{ formatTime(skill.updatedAt) }}</p>
                    <div class="mt-3 flex flex-wrap gap-2">
                      <span v-for="alias in skill.aliases" :key="alias" class="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-700">{{ alias }}</span>
                      <span v-if="skill.aliases.length === 0" class="text-sm text-slate-400">暂无别名</span>
                    </div>
                  </div>
                  <button class="rounded-xl border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-50" @click="editSkill(skill)">编辑</button>
                </div>
              </article>
            </div>
          </article>

          <div class="grid gap-6">
            <article class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
              <h2 class="text-xl font-bold text-slate-900">用户角色分布</h2>
              <div class="mt-5 space-y-4">
                <div v-for="item in overview?.usersByRole || []" :key="item.label">
                  <div class="mb-2 flex items-center justify-between text-sm font-medium text-slate-700">
                    <span>{{ item.label }}</span>
                    <span>{{ item.value }}</span>
                  </div>
                  <div class="h-3 rounded-full bg-slate-100">
                    <div class="h-3 rounded-full bg-gradient-to-r from-sky-500 to-cyan-400" :style="{ width: widthPercent(item.value, userRoleMax) }"></div>
                  </div>
                </div>
              </div>
            </article>

            <article class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
              <h2 class="text-xl font-bold text-slate-900">简历状态分布</h2>
              <div class="mt-5 space-y-4">
                <div v-for="item in overview?.resumesByStatus || []" :key="item.label">
                  <div class="mb-2 flex items-center justify-between text-sm font-medium text-slate-700">
                    <span>{{ item.label }}</span>
                    <span>{{ item.value }}</span>
                  </div>
                  <div class="h-3 rounded-full bg-slate-100">
                    <div class="h-3 rounded-full bg-gradient-to-r from-indigo-500 to-blue-400" :style="{ width: widthPercent(item.value, resumeStatusMax) }"></div>
                  </div>
                </div>
              </div>
            </article>

            <article class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
              <div class="flex items-center justify-between gap-4">
                <div>
                  <h2 class="text-xl font-bold text-slate-900">最新解析失败</h2>
                  <p class="mt-1 text-sm text-slate-500">用于演示队列失败追踪与回归样本排查。</p>
                </div>
                <button class="rounded-xl border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-50" @click="loadData">刷新</button>
              </div>

              <div class="mt-5 space-y-3">
                <div v-if="parseFailures.length === 0" class="rounded-2xl border border-dashed border-slate-200 px-4 py-8 text-center text-sm text-slate-500">
                  当前没有解析失败样本。
                </div>

                <article v-for="failure in parseFailures" :key="failure.resumeId" class="rounded-2xl border border-rose-100 bg-rose-50/60 px-4 py-4">
                  <div class="flex flex-wrap items-center gap-2 text-xs font-semibold text-rose-700">
                    <span class="rounded-full bg-white px-2.5 py-1">{{ failure.sourceFileName || '未命名文件' }}</span>
                    <span class="rounded-full bg-white px-2.5 py-1">{{ failure.ownerUsername || '匿名上传' }}</span>
                  </div>
                  <p class="mt-3 text-sm font-semibold text-slate-900">{{ failure.reason || '未提供失败原因' }}</p>
                  <p class="mt-2 break-all text-xs text-slate-500">{{ failure.rawContentReference }}</p>
                  <p class="mt-3 text-xs text-slate-500">最后更新时间：{{ formatTime(failure.updatedAt) }}</p>
                </article>
              </div>
            </article>
          </div>
        </section>
      </template>
    </div>
  </div>
</template>