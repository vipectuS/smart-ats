<template>
  <section class="bg-white border border-slate-200 rounded-2xl shadow-sm p-5 space-y-4">
    <div class="flex items-center justify-between gap-3">
      <div>
        <h2 class="text-lg font-bold text-slate-900">评估历史</h2>
        <p class="text-sm text-slate-500">查看跨会话调权版本，并可将历史权重回填到当前滑杆。</p>
      </div>
      <span class="text-xs font-medium px-2.5 py-1 rounded-full bg-slate-100 text-slate-600">{{ items.length }} 条记录</span>
    </div>

    <div v-if="loading" class="py-8 flex flex-col items-center gap-3 text-sm text-slate-500">
      <div class="w-7 h-7 rounded-full border-4 border-slate-100 border-t-slate-500 animate-spin"></div>
      <p>加载评估历史中...</p>
    </div>

    <div v-else-if="error" class="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
      {{ error }}
    </div>

    <div v-else-if="items.length === 0" class="rounded-xl border border-dashed border-slate-200 bg-slate-50 px-4 py-8 text-center text-sm text-slate-500">
      还没有历史评估记录。执行一次 AI 动态画像评估后会在这里沉淀版本轨迹。
    </div>

    <div v-else-if="overview" class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 space-y-3">
      <div class="flex flex-wrap items-center gap-2">
        <span class="text-sm font-semibold text-slate-900">多版本总览</span>
        <span class="rounded-full bg-white px-2.5 py-1 text-xs font-medium text-slate-600 border border-slate-200">首版 v{{ overview.firstVersion }} → 当前 v{{ overview.latestVersion }}</span>
      </div>

      <p class="text-sm text-slate-600 leading-6">{{ overview.summary }}</p>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-3 text-xs">
        <div class="rounded-xl bg-white border border-slate-200 px-3 py-3 text-slate-600">
          <p class="text-slate-400 mb-1">策略主漂移</p>
          <p class="font-medium text-slate-800">{{ overview.primaryShift }}</p>
        </div>
        <div class="rounded-xl bg-white border border-slate-200 px-3 py-3 text-slate-600">
          <p class="text-slate-400 mb-1">Top1 稳定性</p>
          <p class="font-medium text-slate-800">{{ overview.topCandidateStability }}</p>
        </div>
        <div class="rounded-xl bg-white border border-slate-200 px-3 py-3 text-slate-600">
          <p class="text-slate-400 mb-1">最近实验目的</p>
          <p class="font-medium text-slate-800 line-clamp-2">{{ overview.latestNote || '未填写备注' }}</p>
        </div>
      </div>
    </div>

    <div v-else class="space-y-3">
      <article
        v-for="(item, index) in items"
        :key="item.evaluationId"
        class="rounded-2xl border border-slate-200 bg-slate-50/70 px-4 py-4"
      >
        <div class="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
          <div class="space-y-2 min-w-0">
            <div class="flex flex-wrap items-center gap-2">
              <span class="text-sm font-semibold text-slate-900">第 {{ item.versionNumber }} 版</span>
              <span v-if="index === 0" class="rounded-full bg-emerald-100 px-2 py-0.5 text-xs font-medium text-emerald-700">当前结果</span>
              <span class="rounded-full bg-slate-200 px-2 py-0.5 text-xs font-medium text-slate-600">{{ item.evaluatedCount }} 人已评估</span>
            </div>

            <div class="text-xs text-slate-500 flex flex-wrap gap-x-4 gap-y-1">
              <span>时间：{{ formatTime(item.evaluatedAt) }}</span>
              <span>操作人：{{ item.evaluatedByUsername || '系统刷新' }}</span>
            </div>

            <p v-if="item.evaluationNote" class="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-600 leading-5">
              <span class="font-medium text-slate-500">备注：</span>{{ item.evaluationNote }}
            </p>

            <div v-if="item.comparisonToPrevious" class="rounded-xl border border-amber-200 bg-amber-50 px-3 py-3 text-xs text-amber-900 space-y-2">
              <p class="font-medium leading-5">{{ item.comparisonToPrevious.summary }}</p>

              <div v-if="item.comparisonToPrevious.weightChanges.length" class="flex flex-wrap gap-2">
                <span
                  v-for="change in item.comparisonToPrevious.weightChanges"
                  :key="`${item.evaluationId}-${change.dimension}`"
                  class="rounded-full px-2.5 py-1 font-medium"
                  :class="change.deltaWeight > 0 ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'"
                >
                  {{ change.label }} {{ formatSignedPercent(change.deltaWeight) }}%
                </span>
              </div>

              <div v-if="item.comparisonToPrevious.topCandidateChange?.enteredCandidates.length || item.comparisonToPrevious.topCandidateChange?.droppedCandidates.length" class="flex flex-wrap gap-2">
                <span
                  v-for="candidate in item.comparisonToPrevious.topCandidateChange?.enteredCandidates || []"
                  :key="`${item.evaluationId}-entered-${candidate}`"
                  class="rounded-full bg-emerald-100 px-2.5 py-1 text-emerald-700"
                >
                  新进 Top3: {{ candidate }}
                </span>
                <span
                  v-for="candidate in item.comparisonToPrevious.topCandidateChange?.droppedCandidates || []"
                  :key="`${item.evaluationId}-dropped-${candidate}`"
                  class="rounded-full bg-slate-200 px-2.5 py-1 text-slate-700"
                >
                  退出 Top3: {{ candidate }}
                </span>
              </div>
            </div>

            <div class="flex flex-wrap gap-2 pt-1">
              <span class="rounded-full bg-blue-50 px-2.5 py-1 text-xs font-medium text-blue-700">技能 {{ asPercent(item.appliedWeights.skillWeight) }}%</span>
              <span class="rounded-full bg-indigo-50 px-2.5 py-1 text-xs font-medium text-indigo-700">经验 {{ asPercent(item.appliedWeights.experienceWeight) }}%</span>
              <span class="rounded-full bg-violet-50 px-2.5 py-1 text-xs font-medium text-violet-700">教育 {{ asPercent(item.appliedWeights.educationWeight) }}%</span>
              <span class="rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-medium text-emerald-700">语义 {{ asPercent(item.appliedWeights.semanticWeight) }}%</span>
            </div>

            <div v-if="item.topRecommendations?.length" class="text-xs text-slate-600 pt-1 flex flex-wrap gap-2">
              <span class="font-medium text-slate-500">Top 候选人：</span>
              <span
                v-for="candidate in item.topRecommendations"
                :key="`${item.evaluationId}-${candidate.resumeId}`"
                class="rounded-full bg-white px-2.5 py-1 border border-slate-200"
              >
                #{{ candidate.rank }} {{ candidate.candidateName || '未命名候选人' }} {{ formatScore(candidate.matchScore) }}
              </span>
            </div>
          </div>

          <button
            type="button"
            class="self-start rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 transition"
            @click="$emit('apply-weights', item.appliedWeights)"
          >
            回填此版权重
          </button>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface AppliedWeights {
  skillWeight: number
  experienceWeight: number
  educationWeight: number
  semanticWeight: number
}

interface EvaluationRecommendationSnapshot {
  rank: number
  resumeId: string
  candidateName: string | null
  matchScore: number
}

interface JobEvaluationHistoryItem {
  evaluationId: string
  versionNumber: number
  evaluatedAt: string
  evaluatedByUsername: string | null
  evaluatedCount: number
  evaluationNote?: string | null
  appliedWeights: AppliedWeights
  comparisonToPrevious?: JobEvaluationDeltaSummary | null
  topRecommendations: EvaluationRecommendationSnapshot[]
}

interface JobEvaluationDeltaSummary {
  summary: string
  weightChanges: JobEvaluationWeightDelta[]
  topCandidateChange?: JobEvaluationTopCandidateChange | null
}

interface JobEvaluationWeightDelta {
  dimension: string
  label: string
  previousWeight: number
  currentWeight: number
  deltaWeight: number
}

interface JobEvaluationTopCandidateChange {
  changed: boolean
  previousCandidateName: string | null
  currentCandidateName: string | null
  previousMatchScore: number | null
  currentMatchScore: number | null
  scoreDelta: number | null
  enteredCandidates: string[]
  droppedCandidates: string[]
}

const props = defineProps<{
  items: JobEvaluationHistoryItem[]
  loading: boolean
  error: string
}>()

defineEmits<{
  (event: 'apply-weights', weights: AppliedWeights): void
}>()

const formatTime = (isoString: string) => {
  const date = new Date(isoString)
  return date.toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const asPercent = (value: number) => Number(value || 0).toFixed(0)

const formatScore = (value: number) => Number(value || 0).toFixed(2)

const formatSignedPercent = (value: number) => {
  const numeric = Number(value || 0)
  return `${numeric > 0 ? '+' : ''}${numeric.toFixed(0)}`
}

const overview = computed(() => {
  if (props.items.length < 2) {
    return null
  }

  const latest = props.items[0]
  const earliest = props.items[props.items.length - 1]
  const dimensions = [
    { key: 'skillWeight', label: '技能' },
    { key: 'experienceWeight', label: '经验' },
    { key: 'educationWeight', label: '教育' },
    { key: 'semanticWeight', label: '语义' },
  ] as const

  const shifts = dimensions.map((dimension) => ({
    label: dimension.label,
    delta: Number(latest.appliedWeights[dimension.key] || 0) - Number(earliest.appliedWeights[dimension.key] || 0),
  }))
  const dominantShift = [...shifts].sort((left, right) => Math.abs(right.delta) - Math.abs(left.delta))[0]

  const top1Names = props.items.map((item) => item.topRecommendations[0]?.candidateName || '未命名候选人')
  const uniqueTop1 = Array.from(new Set(top1Names))
  const topCandidateStability = uniqueTop1.length === 1
    ? `Top1 一直保持为 ${uniqueTop1[0]}`
    : `Top1 共切换 ${uniqueTop1.length - 1} 次，当前为 ${top1Names[0]}`

  return {
    firstVersion: earliest.versionNumber,
    latestVersion: latest.versionNumber,
    summary: `从首版到当前版，${dominantShift.label}${dominantShift.delta > 0 ? '累计上调' : '累计下调'} ${Math.abs(dominantShift.delta).toFixed(0)}%，共沉淀 ${props.items.length} 次手动评估。`,
    primaryShift: `${dominantShift.label}${dominantShift.delta > 0 ? '上调' : '下调'} ${Math.abs(dominantShift.delta).toFixed(0)}%`,
    topCandidateStability,
    latestNote: latest.evaluationNote,
  }
})
</script>