<script setup lang="ts">
import { computed } from 'vue'
import type { AdminOverview } from '@/types/admin'

const props = defineProps<{
  overview: AdminOverview | null
  organizationCount: number
}>()

const metricCards = computed(() => {
  if (!props.overview) return []
  return [
    { label: '系统用户', value: props.overview.totals.totalUsers, tone: 'from-sky-500 to-cyan-500', note: '覆盖 HR、候选人、管理员账号' },
    { label: '职位总量', value: props.overview.totals.totalJobs, tone: 'from-indigo-500 to-blue-500', note: '当前数据库中的岗位记录' },
    { label: '简历总量', value: props.overview.totals.totalResumes, tone: 'from-emerald-500 to-teal-500', note: '含待解析、已解析与失败样本' },
    { label: '技能词条', value: props.overview.totals.totalSkillEntries, tone: 'from-amber-500 to-orange-500', note: '管理员可维护的规范技能字典' },
    { label: '组织数量', value: props.organizationCount, tone: 'from-fuchsia-500 to-pink-500', note: '可用于 HR 注册和职位归属隔离' },
  ]
})
const userRoleMax = computed(() => Math.max(...(props.overview?.usersByRole.map(item => item.value) ?? [1])))
const resumeStatusMax = computed(() => Math.max(...(props.overview?.resumesByStatus.map(item => item.value) ?? [1])))
const widthPercent = (value: number, max: number) => `${Math.max(10, Math.round((value / Math.max(max, 1)) * 100))}%`
</script>
<template>
  <div class="grid gap-6">
    <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
      <article v-for="card in metricCards" :key="card.label" class="overflow-hidden rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <div class="h-2 rounded-full bg-slate-100">
          <div class="h-2 rounded-full bg-gradient-to-r" :class="card.tone"></div>
        </div>
        <p class="mt-4 text-sm font-medium text-slate-500">{{ card.label }}</p>
        <p class="mt-2 text-4xl font-black text-slate-900">{{ card.value }}</p>
        <p class="mt-2 text-sm text-slate-500">{{ card.note }}</p>
      </article>
    </section>

    <section class="grid gap-6 md:grid-cols-2">
      <article class="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 class="text-lg font-semibold text-slate-900 border-b border-slate-100 pb-3 mb-4">用户角色分布</h2>
        <div class="space-y-4">
          <div v-for="item in overview?.usersByRole || []" :key="item.label">
            <div class="mb-2 flex items-center justify-between text-sm text-slate-600">
              <span>{{ item.label }}</span>
              <span class="font-medium text-slate-900">{{ item.value }}</span>
            </div>
            <div class="h-2 rounded-full bg-slate-100">
              <div class="h-2 rounded-full bg-indigo-500" :style="{ width: widthPercent(item.value, userRoleMax) }"></div>
            </div>
          </div>
        </div>
      </article>

      <article class="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 class="text-lg font-semibold text-slate-900 border-b border-slate-100 pb-3 mb-4">简历状态分布</h2>
        <div class="space-y-4">
          <div v-for="item in overview?.resumesByStatus || []" :key="item.label">
            <div class="mb-2 flex items-center justify-between text-sm text-slate-600">
              <span>{{ item.label }}</span>
              <span class="font-medium text-slate-900">{{ item.value }}</span>
            </div>
            <div class="h-2 rounded-full bg-slate-100">
              <div class="h-2 rounded-full bg-emerald-500" :style="{ width: widthPercent(item.value, resumeStatusMax) }"></div>
            </div>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>
