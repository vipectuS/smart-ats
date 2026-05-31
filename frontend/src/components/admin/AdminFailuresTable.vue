<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import api from '@/utils/api'
import { resolveApiError } from '@/utils/apiError'
import type {
  AdminParseFailureBatchActionResponse,
  AdminParseFailure,
  AdminParseFailureSummary,
  AdminParseFailureReviewActionType,
  AdminParseFailureReviewEvent,
  AdminParseFailureReviewStatus,
} from '@/types/admin'
import { AlertTriangle, Download, RefreshCw, RotateCcw, Save } from 'lucide-vue-next'

const props = defineProps<{
  parseFailures: AdminParseFailure[]
  summary: AdminParseFailureSummary | null
  reviewStatusFilter: AdminParseFailureReviewStatus | ''
}>()
const emit = defineEmits<{
  refresh: []
  'update:reviewStatusFilter': [value: AdminParseFailureReviewStatus | '']
}>()

const noteDrafts = reactive<Record<string, string>>({})
const reviewStatusDrafts = reactive<Record<string, AdminParseFailureReviewStatus>>({})
const savingState = reactive<Record<string, boolean>>({})
const retryingState = reactive<Record<string, boolean>>({})
const exportingState = reactive<Record<string, boolean>>({})
const reviewEventsOpen = reactive<Record<string, boolean>>({})
const reviewEventsLoading = reactive<Record<string, boolean>>({})
const reviewEventsError = reactive<Record<string, string>>({})
const reviewEvents = reactive<Record<string, AdminParseFailureReviewEvent[]>>({})
const rowMessages = reactive<Record<string, string>>({})
const rowErrors = reactive<Record<string, string>>({})
const selectedRows = reactive<Record<string, boolean>>({})
const batchNote = ref('')
const batchReviewStatus = ref<AdminParseFailureReviewStatus>('NEEDS_CANDIDATE_UPDATE')
const batchSaving = ref(false)
const batchRetrying = ref(false)
const exportingSummary = ref(false)
const batchMessage = ref('')
const batchError = ref('')

const reviewStatusOptions: Array<{ value: AdminParseFailureReviewStatus | ''; label: string }> = [
  { value: '', label: '全部结论' },
  { value: 'UNREVIEWED', label: '未复核' },
  { value: 'NEEDS_CANDIDATE_UPDATE', label: '需候选人补充' },
  { value: 'APPROVED_FOR_RETRY', label: '允许重试' },
  { value: 'NO_FURTHER_ACTION', label: '不再处理' },
]

const reviewStatusLabelMap: Record<AdminParseFailureReviewStatus, string> = {
  UNREVIEWED: '未复核',
  NEEDS_CANDIDATE_UPDATE: '需候选人补充',
  APPROVED_FOR_RETRY: '允许重试',
  NO_FURTHER_ACTION: '不再处理',
}

const reviewActionLabelMap: Record<AdminParseFailureReviewActionType, string> = {
  REVIEW_SAVED: '保存复核',
  RETRY_QUEUED: '复核后重试',
}

const syncDrafts = (failures: AdminParseFailure[]) => {
  const activeIds = new Set(failures.map(failure => failure.resumeId))

  failures.forEach((failure) => {
    noteDrafts[failure.resumeId] = failure.adminReviewNote || ''
    reviewStatusDrafts[failure.resumeId] = failure.reviewStatus
  })

  Object.keys(noteDrafts).forEach((resumeId) => {
    if (!activeIds.has(resumeId)) {
      delete noteDrafts[resumeId]
      delete reviewStatusDrafts[resumeId]
      delete savingState[resumeId]
      delete retryingState[resumeId]
      delete exportingState[resumeId]
      delete reviewEventsOpen[resumeId]
      delete reviewEventsLoading[resumeId]
      delete reviewEventsError[resumeId]
      delete reviewEvents[resumeId]
      delete rowMessages[resumeId]
      delete rowErrors[resumeId]
      delete selectedRows[resumeId]
    }
  })
}

watch(() => props.parseFailures, syncDrafts, { immediate: true })

const formatTime = (value: string) => new Date(value).toLocaleString('zh-CN')

const formatReviewStatus = (value: AdminParseFailureReviewStatus) => reviewStatusLabelMap[value]

const formatReviewAction = (value: AdminParseFailureReviewActionType) => reviewActionLabelMap[value]

const onFilterChange = (value: string) => {
  emit('update:reviewStatusFilter', value as AdminParseFailureReviewStatus | '')
}

const selectedResumeIds = computed(() => props.parseFailures
  .map(failure => failure.resumeId)
  .filter(resumeId => selectedRows[resumeId]))

const allRowsSelected = computed(() => props.parseFailures.length > 0 && props.parseFailures.every(failure => selectedRows[failure.resumeId]))

const batchActionDisabled = computed(() => selectedResumeIds.value.length === 0 || batchSaving.value || batchRetrying.value)
const summaryCountMap = computed(() => Object.fromEntries((props.summary?.reviewStatusCounts || []).map(item => [item.label, item.value])))
const summaryTopFailureCodes = computed(() => (props.summary?.failureCodeCounts || []).slice(0, 4))

const toggleAllRows = (checked: boolean) => {
  props.parseFailures.forEach((failure) => {
    selectedRows[failure.resumeId] = checked
  })
}

const clearBatchFeedback = () => {
  batchMessage.value = ''
  batchError.value = ''
}

const clearSelection = () => {
  Object.keys(selectedRows).forEach((resumeId) => {
    selectedRows[resumeId] = false
  })
}

const exportSummary = async () => {
  exportingSummary.value = true
  clearBatchFeedback()

  try {
    const response = await api.get('/admin/parse-failures/summary/export', {
      responseType: 'blob',
    })
    const blob = new Blob([response.data], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = 'parse-failure-summary.csv'
    document.body.appendChild(anchor)
    anchor.click()
    document.body.removeChild(anchor)
    window.URL.revokeObjectURL(url)
  } catch (error: any) {
    batchError.value = resolveApiError(error, '导出 parse-failure 汇总报表失败。').summary
  } finally {
    exportingSummary.value = false
  }
}

const runBatchAction = async (mode: 'review' | 'retry') => {
  const resumeIds = selectedResumeIds.value
  if (resumeIds.length === 0) {
    batchError.value = '请先选择至少一条失败样本。'
    return
  }

  clearBatchFeedback()
  if (mode === 'review') {
    batchSaving.value = true
  } else {
    batchRetrying.value = true
  }

  try {
    const endpoint = mode === 'review' ? '/admin/parse-failures/review' : '/admin/parse-failures/retry'
    const payload = mode === 'review'
      ? { resumeIds, note: batchNote.value || null, reviewStatus: batchReviewStatus.value }
      : { resumeIds, note: batchNote.value || null }
    const response = await api.request<AdminParseFailureBatchActionResponse>({
      url: endpoint,
      method: mode === 'review' ? 'PUT' : 'POST',
      data: payload,
    })
    const processedCount = response.data?.processedCount || resumeIds.length
    batchMessage.value = mode === 'review'
      ? `已批量保存 ${processedCount} 条人工复核结论。`
      : `已批量将 ${processedCount} 条失败样本重新入队。`
    clearSelection()
    batchNote.value = ''
    emit('refresh')
  } catch (error: any) {
    batchError.value = resolveApiError(error, mode === 'review' ? '批量保存复核结论失败。' : '批量重新入队失败。').summary
  } finally {
    batchSaving.value = false
    batchRetrying.value = false
  }
}

const loadReviewEvents = async (resumeId: string, force = false) => {
  if (!force && reviewEvents[resumeId]) {
    return
  }

  reviewEventsLoading[resumeId] = true
  reviewEventsError[resumeId] = ''

  try {
    const response = await api.get(`/admin/parse-failures/${resumeId}/review-events`, { params: { limit: 10 } })
    reviewEvents[resumeId] = response.data as AdminParseFailureReviewEvent[]
  } catch (error: any) {
    reviewEventsError[resumeId] = resolveApiError(error, '加载复核留痕失败。').summary
  } finally {
    reviewEventsLoading[resumeId] = false
  }
}

const toggleReviewEvents = async (resumeId: string) => {
  reviewEventsOpen[resumeId] = !reviewEventsOpen[resumeId]
  if (reviewEventsOpen[resumeId]) {
    await loadReviewEvents(resumeId)
  }
}

const exportReviewEvents = async (resumeId: string, sourceFileName: string | null) => {
  exportingState[resumeId] = true
  rowErrors[resumeId] = ''

  try {
    const response = await api.get(`/admin/parse-failures/${resumeId}/review-events/export`, {
      params: { limit: 100 },
      responseType: 'blob',
    })
    const blob = new Blob([response.data], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    const baseName = (sourceFileName || `resume-${resumeId}`).replace(/\.[^.]+$/, '')
    anchor.href = url
    anchor.download = `${baseName}-review-events.csv`
    document.body.appendChild(anchor)
    anchor.click()
    document.body.removeChild(anchor)
    window.URL.revokeObjectURL(url)
  } catch (error: any) {
    rowErrors[resumeId] = resolveApiError(error, '导出复核留痕失败。').summary
  } finally {
    exportingState[resumeId] = false
  }
}

const saveReview = async (resumeId: string) => {
  savingState[resumeId] = true
  rowErrors[resumeId] = ''
  rowMessages[resumeId] = ''

  try {
    await api.put(`/admin/parse-failures/${resumeId}/review`, {
      note: noteDrafts[resumeId] || null,
      reviewStatus: reviewStatusDrafts[resumeId],
    })
    rowMessages[resumeId] = '人工复核结论已保存。'
    if (reviewEventsOpen[resumeId]) {
      await loadReviewEvents(resumeId, true)
    }
    emit('refresh')
  } catch (error: any) {
    rowErrors[resumeId] = resolveApiError(error, '保存人工复核结论失败。').summary
  } finally {
    savingState[resumeId] = false
  }
}

const retryFailure = async (resumeId: string) => {
  retryingState[resumeId] = true
  rowErrors[resumeId] = ''
  rowMessages[resumeId] = ''

  try {
    await api.post(`/admin/parse-failures/${resumeId}/retry`, { note: noteDrafts[resumeId] || null })
    rowMessages[resumeId] = '已重新入队，等待解析服务处理。'
    if (reviewEventsOpen[resumeId]) {
      await loadReviewEvents(resumeId, true)
    }
    emit('refresh')
  } catch (error: any) {
    rowErrors[resumeId] = resolveApiError(error, '重新入队失败。').summary
  } finally {
    retryingState[resumeId] = false
  }
}
</script>
<template>
  <article class="rounded-xl border border-slate-200 bg-white shadow-sm overflow-hidden flex flex-col">
    <div class="p-6 border-b border-slate-100 flex items-center justify-between">
      <div>
        <h2 class="text-lg font-semibold text-slate-900 flex items-center gap-2"><AlertTriangle class="w-5 h-5 text-rose-500" />最新解析失败流水</h2>
        <p class="mt-1 text-sm text-slate-500">追踪队列异步解析错误日志，并沉淀人工复核结论。</p>
      </div>
      <div class="flex flex-wrap items-center gap-3">
        <label class="text-xs font-semibold uppercase tracking-wide text-slate-500">
          结论筛选
          <select
            :value="reviewStatusFilter"
            @change="onFilterChange(($event.target as HTMLSelectElement).value)"
            class="mt-1 block rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
          >
            <option v-for="option in reviewStatusOptions" :key="option.value || 'ALL'" :value="option.value">{{ option.label }}</option>
          </select>
        </label>
        <button @click="emit('refresh')" class="bg-slate-50 border border-slate-200 text-slate-600 px-3 py-1.5 rounded-lg text-sm font-medium hover:bg-slate-100 transition flex items-center gap-2"><RefreshCw class="w-4 h-4" />刷新</button>
        <button @click="exportSummary" :disabled="exportingSummary" class="bg-white border border-slate-200 text-slate-600 px-3 py-1.5 rounded-lg text-sm font-medium hover:bg-slate-100 transition flex items-center gap-2 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-400"><Download class="w-4 h-4" />{{ exportingSummary ? '导出中...' : '导出汇总' }}</button>
      </div>
    </div>

    <div v-if="summary" class="border-b border-slate-100 bg-white px-6 py-4">
      <div class="grid gap-3 xl:grid-cols-[repeat(4,minmax(0,1fr))_minmax(0,1.4fr)]">
        <div class="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3">
          <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">总失败数</p>
          <p class="mt-2 text-2xl font-bold text-slate-900">{{ summary.totalFailures }}</p>
        </div>
        <div class="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3">
          <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">未复核</p>
          <p class="mt-2 text-2xl font-bold text-rose-600">{{ summaryCountMap.UNREVIEWED || 0 }}</p>
        </div>
        <div class="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3">
          <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">待补充</p>
          <p class="mt-2 text-2xl font-bold text-amber-600">{{ summaryCountMap.NEEDS_CANDIDATE_UPDATE || 0 }}</p>
        </div>
        <div class="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3">
          <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">允许重试</p>
          <p class="mt-2 text-2xl font-bold text-emerald-600">{{ summaryCountMap.APPROVED_FOR_RETRY || 0 }}</p>
        </div>
        <div class="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3">
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">主要失败码</p>
              <p class="mt-1 text-xs text-slate-500">基于当前全量 parse-failure 样本自动聚合。</p>
            </div>
            <span v-if="summary.lastFailureAt" class="text-xs text-slate-400">最近更新 {{ formatTime(summary.lastFailureAt) }}</span>
          </div>
          <div class="mt-3 flex flex-wrap gap-2">
            <span v-for="item in summaryTopFailureCodes" :key="item.label" class="rounded-full border border-slate-200 bg-white px-3 py-1 text-xs font-medium text-slate-700">
              {{ item.label }} · {{ item.value }}
            </span>
            <span v-if="summaryTopFailureCodes.length === 0" class="text-xs text-slate-400">暂无失败码统计。</span>
          </div>
        </div>
      </div>
    </div>

    <div class="border-b border-slate-100 bg-slate-50/80 px-6 py-4 space-y-3">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p class="text-sm font-semibold text-slate-800">批量复核操作</p>
          <p class="mt-1 text-xs text-slate-500">先勾选样本，再统一保存结论或重新入队，减少逐条处理成本。</p>
        </div>
        <span class="rounded-full border border-slate-200 bg-white px-3 py-1 text-xs font-medium text-slate-600">
          已选 {{ selectedResumeIds.length }} / {{ parseFailures.length }}
        </span>
      </div>

      <div class="grid gap-3 lg:grid-cols-[180px_minmax(0,1fr)_auto_auto] lg:items-end">
        <label class="block text-xs font-semibold uppercase tracking-wide text-slate-500">
          批量结论
          <select
            v-model="batchReviewStatus"
            class="mt-2 block w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
          >
            <option
              v-for="option in reviewStatusOptions.filter(item => item.value)"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </option>
          </select>
        </label>

        <label class="block text-xs font-semibold uppercase tracking-wide text-slate-500">
          批量备注
          <textarea
            v-model="batchNote"
            rows="2"
            maxlength="1000"
            class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
            placeholder="例如：这批样本统一等待候选人补充 PDF 或关键时间线。"
          />
        </label>

        <button
          :disabled="batchActionDisabled"
          @click="runBatchAction('review')"
          class="inline-flex items-center justify-center gap-2 rounded-lg border border-slate-200 bg-white px-4 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-400"
        >
          <Save class="w-4 h-4" />{{ batchSaving ? '批量保存中...' : '批量保存结论' }}
        </button>

        <button
          :disabled="batchActionDisabled"
          @click="runBatchAction('retry')"
          class="inline-flex items-center justify-center gap-2 rounded-lg bg-indigo-600 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:bg-slate-400"
        >
          <RotateCcw class="w-4 h-4" />{{ batchRetrying ? '批量重试中...' : '批量重新入队' }}
        </button>
      </div>

      <div class="flex flex-wrap items-center justify-between gap-3 text-xs text-slate-500">
        <label class="inline-flex items-center gap-2">
          <input
            type="checkbox"
            :checked="allRowsSelected"
            @change="toggleAllRows(($event.target as HTMLInputElement).checked)"
            class="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
          >
          <span>全选当前列表</span>
        </label>
        <span>{{ batchNote.length }}/1000</span>
      </div>

      <div v-if="batchError" class="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700">{{ batchError }}</div>
      <div v-else-if="batchMessage" class="rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{{ batchMessage }}</div>
    </div>
    
    <div class="flex-1 overflow-x-auto bg-slate-50/50 p-6 max-h-[600px] overflow-y-auto">
        <div v-if="parseFailures.length === 0" class="text-center py-16 text-slate-400 text-sm">
           当前无任何解析失败样本，队列运作完美。
        </div>
        <ul v-else class="space-y-4">
           <li v-for="failure in parseFailures" :key="failure.resumeId" class="bg-white border border-rose-100 rounded-lg p-4 shadow-sm flex flex-col gap-2">
              <div class="flex items-center justify-between gap-3">
                <label class="inline-flex items-center gap-2 text-xs font-medium text-slate-500">
                  <input
                    v-model="selectedRows[failure.resumeId]"
                    type="checkbox"
                    class="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
                    @change="clearBatchFeedback"
                  >
                  <span>加入批量操作</span>
                </label>
              </div>
              <div class="flex items-center gap-3">
                  <span class="text-sm font-bold text-slate-900">{{ failure.sourceFileName || '未命名简历' }}</span>
                  <span class="text-xs bg-slate-100 px-2 py-0.5 rounded text-slate-600">上传者: {{ failure.ownerUsername || '未知' }}</span>
                  <span class="text-xs bg-indigo-50 px-2 py-0.5 rounded text-indigo-700">{{ formatReviewStatus(failure.reviewStatus) }}</span>
                  <span v-if="failure.parseFailureCode" class="text-xs bg-rose-50 px-2 py-0.5 rounded text-rose-700">{{ failure.parseFailureCode }}</span>
              </div>
              <div class="text-sm text-rose-600 bg-rose-50 px-3 py-2 rounded font-mono">{{ failure.reason || '未捕获的具体失败原因' }}</div>
              <div class="text-xs text-slate-400 font-mono truncate bg-slate-100 p-2 rounded">{{ failure.rawContentReference }}</div>
              <div class="rounded-lg border border-slate-200 bg-slate-50 p-3">
                <div class="grid gap-3 lg:grid-cols-[180px_minmax(0,1fr)]">
                  <label class="block text-xs font-semibold uppercase tracking-wide text-slate-500">
                    复核结论
                    <select
                      v-model="reviewStatusDrafts[failure.resumeId]"
                      class="mt-2 block w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
                    >
                      <option
                        v-for="option in reviewStatusOptions.filter(item => item.value)"
                        :key="option.value"
                        :value="option.value"
                      >
                        {{ option.label }}
                      </option>
                    </select>
                  </label>
                  <label class="block text-xs font-semibold uppercase tracking-wide text-slate-500">
                    人工复核备注
                    <textarea
                      v-model="noteDrafts[failure.resumeId]"
                      rows="3"
                      maxlength="1000"
                      class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
                      placeholder="例如：确认 PDF 内容完整，但时间线字段缺失，建议候选人补充后重试。"
                    />
                  </label>
                </div>
                <div class="mt-2 flex flex-wrap items-center justify-between gap-2 text-xs text-slate-500">
                  <span v-if="failure.reviewedByUsername">最近复核: {{ failure.reviewedByUsername }}<template v-if="failure.reviewedAt"> · {{ formatTime(failure.reviewedAt) }}</template></span>
                  <span v-else>尚无人工备注</span>
                  <span>{{ (noteDrafts[failure.resumeId] || '').length }}/1000</span>
                </div>
              </div>
              <div v-if="rowErrors[failure.resumeId]" class="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700">{{ rowErrors[failure.resumeId] }}</div>
              <div v-else-if="rowMessages[failure.resumeId]" class="rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{{ rowMessages[failure.resumeId] }}</div>
              <div class="flex flex-wrap gap-2">
                <button
                  :disabled="savingState[failure.resumeId] || retryingState[failure.resumeId]"
                  @click="saveReview(failure.resumeId)"
                  class="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-400"
                >
                  <Save class="w-4 h-4" />{{ savingState[failure.resumeId] ? '保存中...' : '保存结论' }}
                </button>
                <button
                  :disabled="savingState[failure.resumeId] || retryingState[failure.resumeId]"
                  @click="retryFailure(failure.resumeId)"
                  class="inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-3 py-2 text-sm font-medium text-white transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:bg-slate-400"
                >
                  <RotateCcw class="w-4 h-4" />{{ retryingState[failure.resumeId] ? '重新入队中...' : '保存并重试' }}
                </button>
                <button
                  :disabled="reviewEventsLoading[failure.resumeId]"
                  @click="toggleReviewEvents(failure.resumeId)"
                  class="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-400"
                >
                  {{ reviewEventsOpen[failure.resumeId] ? '收起留痕' : '查看留痕' }}
                </button>
                <button
                  :disabled="exportingState[failure.resumeId]"
                  @click="exportReviewEvents(failure.resumeId, failure.sourceFileName)"
                  class="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-400"
                >
                  {{ exportingState[failure.resumeId] ? '导出中...' : '导出留痕' }}
                </button>
              </div>
              <div v-if="reviewEventsOpen[failure.resumeId]" class="rounded-lg border border-slate-200 bg-slate-50 p-3">
                <div class="mb-3 flex items-center justify-between gap-2">
                  <span class="text-xs font-semibold uppercase tracking-wide text-slate-500">复核留痕</span>
                  <span v-if="reviewEventsLoading[failure.resumeId]" class="text-xs text-slate-400">加载中...</span>
                </div>
                <div v-if="reviewEventsError[failure.resumeId]" class="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700">{{ reviewEventsError[failure.resumeId] }}</div>
                <div v-else-if="!reviewEvents[failure.resumeId] || reviewEvents[failure.resumeId].length === 0" class="text-sm text-slate-400">当前还没有复核事件。</div>
                <ol v-else class="space-y-3">
                  <li v-for="event in reviewEvents[failure.resumeId]" :key="event.id" class="rounded-lg border border-slate-200 bg-white px-3 py-3">
                    <div class="flex flex-wrap items-center gap-2 text-sm text-slate-700">
                      <span class="font-semibold text-slate-900">{{ formatReviewAction(event.actionType) }}</span>
                      <span class="rounded bg-slate-100 px-2 py-0.5 text-xs text-slate-600">{{ formatReviewStatus(event.previousReviewStatus) }} -> {{ formatReviewStatus(event.nextReviewStatus) }}</span>
                      <span class="rounded bg-indigo-50 px-2 py-0.5 text-xs text-indigo-700">{{ event.resumeStatusAfterAction }}</span>
                    </div>
                    <div class="mt-1 text-xs text-slate-500">{{ event.adminUsername }} · {{ formatTime(event.createdAt) }}</div>
                    <div v-if="event.note" class="mt-2 rounded bg-slate-50 px-3 py-2 text-sm text-slate-600">{{ event.note }}</div>
                  </li>
                </ol>
              </div>
              <div class="text-[10px] text-slate-400 text-right mt-1">{{ formatTime(failure.updatedAt) }}</div>
           </li>
        </ul>
    </div>
  </article>
</template>
