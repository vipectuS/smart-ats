<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '@/utils/api'
import { resolveApiError } from '@/utils/apiError'
import type { AdminOverview, AdminParseFailure, AdminParseFailureReviewStatus, AdminParseFailureSummary, AdminSkill, AdminOrganization } from '@/types/admin'
import { Activity, Database, Users, AlertCircle, Loader2 } from 'lucide-vue-next'

import AdminOverviewPanel from '@/components/admin/AdminOverviewPanel.vue'
import AdminOrganizations from '@/components/admin/AdminOrganizations.vue'
import AdminSkillsDictionary from '@/components/admin/AdminSkillsDictionary.vue'
import AdminFailuresTable from '@/components/admin/AdminFailuresTable.vue'

const loading = ref(true)
const errorMsg = ref('')
const activeTab = ref('overview')
const parseFailureReviewStatusFilter = ref<AdminParseFailureReviewStatus | ''>('')

const overview = ref<AdminOverview | null>(null)
const parseFailures = ref<AdminParseFailure[]>([])
const parseFailureSummary = ref<AdminParseFailureSummary | null>(null)
const skills = ref<AdminSkill[]>([])
const organizations = ref<AdminOrganization[]>([])

const buildParseFailureParams = () => {
  const params: Record<string, string | number> = { limit: 12 }
  if (parseFailureReviewStatusFilter.value) {
    params.reviewStatus = parseFailureReviewStatusFilter.value
  }
  return params
}

const loadData = async () => {
  loading.value = true
  errorMsg.value = ''
  try {
    const [overviewRes, skillsRes, failuresRes, failureSummaryRes, orgsRes] = await Promise.all([
      api.get('/admin/overview'),
      api.get('/admin/skills'),
      api.get('/admin/parse-failures', { params: buildParseFailureParams() }),
      api.get('/admin/parse-failures/summary'),
      api.get('/admin/organizations'),
    ])
    overview.value = overviewRes.data as AdminOverview
    skills.value = skillsRes.data as AdminSkill[]
    parseFailures.value = failuresRes.data as AdminParseFailure[]
    parseFailureSummary.value = failureSummaryRes.data as AdminParseFailureSummary
    organizations.value = orgsRes.data as AdminOrganization[]
  } catch (error: any) {
    errorMsg.value = resolveApiError(error, '控制台数据加载失败。').summary
  } finally {
    loading.value = false
  }
}

const handleParseFailureFilterChange = (value: AdminParseFailureReviewStatus | '') => {
  parseFailureReviewStatusFilter.value = value
  loadData()
}

onMounted(() => { loadData() })
</script>

<template>
  <div class="h-full flex flex-col space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold tracking-tight text-slate-900">系统运营与平台治理</h1>
        <p class="mt-1 text-sm text-slate-500">掌握全站核心资源流动与系统异步状态。</p>
      </div>
    </div>

    <!-- Tabs Orchestrator -->
    <div class="bg-white border border-slate-200 rounded-xl shadow-sm px-2 pt-2 flex gap-4 overflow-x-auto">
        <button @click="activeTab = 'overview'" :class="activeTab === 'overview' ? 'border-indigo-600 text-indigo-600' : 'border-transparent text-slate-500 hover:text-slate-900'" class="px-4 py-3 border-b-2 text-sm font-medium flex items-center gap-2 transition whitespace-nowrap"><Activity class="w-4 h-4"/>监控看板</button>
        <button @click="activeTab = 'orgs'" :class="activeTab === 'orgs' ? 'border-indigo-600 text-indigo-600' : 'border-transparent text-slate-500 hover:text-slate-900'" class="px-4 py-3 border-b-2 text-sm font-medium flex items-center gap-2 transition whitespace-nowrap"><Users class="w-4 h-4"/>企业组织池</button>
        <button @click="activeTab = 'skills'" :class="activeTab === 'skills' ? 'border-indigo-600 text-indigo-600' : 'border-transparent text-slate-500 hover:text-slate-900'" class="px-4 py-3 border-b-2 text-sm font-medium flex items-center gap-2 transition whitespace-nowrap"><Database class="w-4 h-4"/>元技能词典</button>
        <button @click="activeTab = 'failures'" :class="activeTab === 'failures' ? 'border-indigo-600 text-indigo-600' : 'border-transparent text-slate-500 hover:text-slate-900'" class="px-4 py-3 border-b-2 text-sm font-medium flex items-center gap-2 transition whitespace-nowrap"><AlertCircle class="w-4 h-4"/>失败漏斗</button>
    </div>

    <div v-if="loading" class="flex-1 flex flex-col items-center justify-center text-slate-400">
        <Loader2 class="w-8 h-8 animate-spin mb-4 text-indigo-500" />
        <p class="text-sm">汇聚控制台数据...</p>
    </div>
    <div v-else-if="errorMsg" class="bg-rose-50 text-rose-700 p-4 rounded-xl border border-rose-200">
        <p class="font-semibold mb-1">面板阻断</p>
        <p class="text-sm">{{ errorMsg }}</p>
        <button @click="loadData" class="mt-3 bg-rose-600 text-white px-4 py-1.5 rounded-lg text-sm transition hover:bg-rose-700">重试</button>
    </div>
    <div v-else class="flex-1 min-h-0">
        <AdminOverviewPanel v-if="activeTab === 'overview'" :overview="overview" :organizationCount="organizations.length" />
        <AdminOrganizations v-else-if="activeTab === 'orgs'" :organizations="organizations" @refresh="loadData" />
        <AdminSkillsDictionary v-else-if="activeTab === 'skills'" :skills="skills" @refresh="loadData" />
        <AdminFailuresTable
          v-else-if="activeTab === 'failures'"
          :parseFailures="parseFailures"
          :summary="parseFailureSummary"
          :reviewStatusFilter="parseFailureReviewStatusFilter"
          @refresh="loadData"
          @update:reviewStatusFilter="handleParseFailureFilterChange"
        />
    </div>
  </div>
</template>
