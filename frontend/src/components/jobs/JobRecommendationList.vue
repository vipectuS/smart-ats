<template>
  <div class="space-y-6">
    <div v-if="loading" class="text-center py-10 text-slate-500">
      <Loader2 class="w-8 h-8 animate-spin mx-auto mb-3 text-blue-500" />
      <p>正在加载推荐结果...</p>
    </div>

    <div v-else-if="recommendations.length === 0" class="text-center py-10 text-slate-500 bg-white/50 rounded-2xl border border-slate-100 border-dashed">
      <div class="mb-3 text-4xl">📭</div>
      <p>暂无评估的候选人。请点击上方按钮进行 AI 评估。</p>
    </div>

    <div v-else v-for="rec in recommendations" :key="rec.resumeId" class="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 flex flex-col lg:flex-row gap-6 hover:shadow-md transition-shadow">
      <!-- Candidate Info & Reasoning -->
      <div class="flex-1 space-y-4">
        <div class="flex items-center justify-between border-b border-slate-100 pb-4">
          <div class="flex items-center gap-4">
            <div class="w-12 h-12 rounded-full bg-gradient-to-br from-indigo-100 to-purple-100 text-indigo-600 flex items-center justify-center font-bold text-lg border border-indigo-200">
              {{ rec.candidate?.candidateName?.charAt(0) || '?' }}
            </div>
            <div>
              <h3 class="text-lg font-bold text-slate-800">{{ rec.candidate?.candidateName || '未知候选人' }}</h3>
              <p class="text-sm text-slate-500">匹配得分: <span class="font-bold text-blue-600 text-lg">{{ rec.matchScore }}</span><span class="text-slate-400">/100</span></p>
            </div>
          </div>
          <div class="px-3 py-1 bg-blue-50 text-blue-600 rounded-lg text-sm font-medium border border-blue-100 flex items-center gap-1.5">
            <Bot class="w-4 h-4" /> {{ rec.xaiReport?.fitBand === 'HIGH' ? '高匹配' : rec.xaiReport?.fitBand === 'MEDIUM' ? '中匹配' : '待补强' }}
          </div>
        </div>

        <div class="grid gap-3 md:grid-cols-2">
          <div v-for="item in getDimensionCards(rec)" :key="item.key" class="rounded-xl border border-slate-100 bg-slate-50 px-4 py-3">
            <div class="mb-2 flex items-center justify-between text-sm">
              <span class="font-medium text-slate-700">{{ item.label }}</span>
              <span class="font-semibold" :class="item.textClass">{{ item.value.toFixed(2) }}</span>
            </div>
            <div class="h-2 rounded-full bg-white">
              <div class="h-2 rounded-full" :class="item.barClass" :style="{ width: `${Math.max(4, Math.min(100, item.value))}%` }" />
            </div>
          </div>
        </div>

        <div v-if="rec.weightShiftAnalysis" class="bg-amber-50 p-4 rounded-xl border border-amber-200 text-sm text-amber-900">
          <h4 class="font-semibold mb-2">调权影响解释</h4>
          <p class="leading-relaxed">{{ rec.weightShiftAnalysis.summary }}</p>
          <div class="mt-3 flex flex-wrap gap-2">
            <span
              v-for="driver in rec.weightShiftAnalysis.drivers"
              :key="driver.key"
              class="rounded-full border px-3 py-1 text-xs font-medium"
              :class="driver.estimatedImpact >= 0 ? 'border-emerald-200 bg-emerald-50 text-emerald-700' : 'border-rose-200 bg-rose-50 text-rose-700'"
            >
              {{ driver.label }}权重{{ driver.deltaWeight > 0 ? '+' : '' }}{{ driver.deltaWeight }}% · 估计影响 {{ driver.estimatedImpact > 0 ? '+' : '' }}{{ driver.estimatedImpact.toFixed(2) }}
            </span>
          </div>
        </div>
        
        <div v-if="getHrActionSections(rec).length" class="rounded-xl border border-slate-200 bg-slate-50 p-4">
          <div class="mb-3 flex items-center justify-between gap-3">
            <div>
              <h4 class="text-sm font-semibold text-slate-800">建议到动作</h4>
              <p class="mt-1 text-xs text-slate-500">把当前候选人的补强建议直接转成培训、材料和面试关注点，并可导出到外部工作流。</p>
            </div>
            <div class="flex flex-wrap items-center justify-end gap-2">
              <span class="rounded-full border border-slate-200 bg-white px-2.5 py-1 text-xs font-medium text-slate-500">
                {{ getHrActionSections(rec).reduce((total, section) => total + section.items.length, 0) }} 项
              </span>
              <button @click="copyHrActionWorkflow(rec)" class="inline-flex items-center gap-1 rounded-full border border-slate-200 bg-white px-3 py-1 text-xs font-medium text-slate-600 transition hover:bg-slate-100">
                <Copy class="h-3.5 w-3.5" />复制
              </button>
              <button @click="exportHrActionWorkflow(rec)" :disabled="actionExportState[rec.resumeId]" class="inline-flex items-center gap-1 rounded-full border border-slate-200 bg-white px-3 py-1 text-xs font-medium text-slate-600 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-400">
                <Download class="h-3.5 w-3.5" />{{ actionExportState[rec.resumeId] ? '导出中' : '导出 Markdown' }}
              </button>
            </div>
          </div>

          <div v-if="actionMessages[rec.resumeId]" class="mb-3 rounded-lg bg-emerald-50 px-3 py-2 text-xs text-emerald-700">
            {{ actionMessages[rec.resumeId] }}
          </div>

          <div class="grid gap-3 md:grid-cols-2">
            <article
              v-for="section in getHrActionSections(rec)"
              :key="`${rec.resumeId}-${section.key}`"
              class="rounded-xl border px-4 py-4"
              :class="section.panelClass"
            >
              <div class="flex items-start gap-3">
                <component :is="section.icon" class="mt-0.5 h-5 w-5 shrink-0" />
                <div>
                  <p class="text-sm font-semibold text-slate-800">{{ section.title }}</p>
                  <p class="mt-1 text-xs leading-5 text-slate-600">{{ section.description }}</p>
                </div>
              </div>

              <ul class="mt-4 space-y-2 text-sm text-slate-700">
                <li v-for="item in section.items" :key="`${section.key}-${item}`" class="flex items-start gap-2 rounded-xl bg-white/80 px-3 py-2">
                  <span class="mt-1.5 h-1.5 w-1.5 rounded-full" :class="section.bulletClass"></span>
                  <span class="leading-6">{{ item }}</span>
                </li>
              </ul>
            </article>
          </div>
        </div>

        <div class="bg-slate-50 p-4 rounded-xl border border-slate-100 text-sm text-slate-700 leading-relaxed shadow-inner">
          <h4 class="font-semibold text-slate-800 mb-2 flex items-center gap-2">
            <Brain class="w-4 h-4 text-purple-500" /> AI 评估理由
          </h4>
          <p class="text-slate-600 whitespace-pre-line">{{ rec.xaiReasoning }}</p>
        </div>
      </div>

      <!-- Radar Chart -->
      <div class="w-full lg:w-72 h-64 shrink-0 bg-slate-50 rounded-xl border border-slate-100 flex items-center justify-center relative overflow-hidden">
        <v-chart class="w-full h-full" :option="getRadarOption(rec)" autoresize />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { BookOpenCheck, Bot, Brain, Copy, Download, FileText, Loader2, MessagesSquare, Sparkles } from 'lucide-vue-next';
import VChart from 'vue-echarts';
import { use } from 'echarts/core';
import { RadarChart } from 'echarts/charts';
import { TitleComponent, TooltipComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';

use([CanvasRenderer, RadarChart, TitleComponent, TooltipComponent]);

defineProps<{
  recommendations: any[];
  loading: boolean;
}>();

type HrActionSectionKey = 'training' | 'evidence' | 'interview' | 'next'

const hrActionSectionMeta = {
  training: {
    title: '培训补强',
    description: '优先处理技能、工具或认证层面的关键差距。',
    icon: BookOpenCheck,
    panelClass: 'border-emerald-200 bg-emerald-50/80',
    bulletClass: 'bg-emerald-500',
  },
  evidence: {
    title: '材料补充',
    description: '提醒 HR 补看项目、成果与岗位相关证据。',
    icon: FileText,
    panelClass: 'border-blue-200 bg-blue-50/80',
    bulletClass: 'bg-blue-500',
  },
  interview: {
    title: '面试关注点',
    description: '适合直接转成后续沟通或二轮面试问题。',
    icon: MessagesSquare,
    panelClass: 'border-violet-200 bg-violet-50/80',
    bulletClass: 'bg-violet-500',
  },
  next: {
    title: '后续动作',
    description: '当前轮最适合立即执行的推进动作。',
    icon: Sparkles,
    panelClass: 'border-amber-200 bg-amber-50/80',
    bulletClass: 'bg-amber-500',
  },
} as const

const hrActionSectionOrder: HrActionSectionKey[] = ['training', 'evidence', 'interview', 'next']
const actionExportState = reactive<Record<string, boolean>>({})
const actionMessages = reactive<Record<string, string>>({})

const dedupeItems = (items: string[]) => Array.from(new Set(items.map(item => item.trim()).filter(Boolean)))

const classifyActionItem = (item: string, source: 'suggestion' | 'nextStep'): HrActionSectionKey => {
  const isInterview = /面试|二轮|沟通|表达|答辩|故事|interview|story|presentation|communication/i.test(item)
  const isSkillGap = /技能|补强|认证|课程|学习|训练|missing|skill|skills|tool|tools/i.test(item)
  const isEvidence = /简历|项目|成果|证据|作品集|经历|关键词|resume|project|evidence|portfolio|keyword|business result/i.test(item)

  if (isInterview) return 'interview'
  if (isEvidence && !isSkillGap) return 'evidence'
  if (isSkillGap) return 'training'
  if (isEvidence) return 'evidence'
  if (source === 'nextStep') return 'next'
  return 'next'
}

const getHrActionSections = (rec: any) => {
  const report = rec?.xaiReport
  if (!report) {
    return []
  }

  const groupedItems: Record<HrActionSectionKey, string[]> = {
    training: [],
    evidence: [],
    interview: [],
    next: [],
  }

  for (const item of report.improvementSuggestions || []) {
    groupedItems[classifyActionItem(item, 'suggestion')].push(item)
  }
  for (const item of report.nextSteps || []) {
    groupedItems[classifyActionItem(item, 'nextStep')].push(item)
  }

  return hrActionSectionOrder
    .map((key) => ({
      key,
      ...hrActionSectionMeta[key],
      items: dedupeItems(groupedItems[key]),
    }))
    .filter((section) => section.items.length > 0)
}

const buildHrActionWorkflowMarkdown = (rec: any) => {
  const sections = getHrActionSections(rec)
  const candidateName = rec?.candidate?.candidateName || '未知候选人'
  const lines = [
    `# 候选人行动清单 - ${candidateName}`,
    '',
    `- 匹配得分: ${rec?.matchScore ?? 'n/a'}/100`,
    `- 匹配分层: ${rec?.xaiReport?.fitBand || 'n/a'}`,
    '',
  ]

  sections.forEach((section) => {
    lines.push(`## ${section.title}`)
    lines.push('')
    section.items.forEach((item) => lines.push(`- ${item}`))
    lines.push('')
  })

  return lines.join('\n')
}

const setActionMessage = (resumeId: string, message: string) => {
  actionMessages[resumeId] = message
  window.setTimeout(() => {
    if (actionMessages[resumeId] === message) {
      delete actionMessages[resumeId]
    }
  }, 2400)
}

const copyHrActionWorkflow = async (rec: any) => {
  const resumeId = rec.resumeId as string
  try {
    await navigator.clipboard.writeText(buildHrActionWorkflowMarkdown(rec))
    setActionMessage(resumeId, '行动清单已复制，可直接贴到工单或面试协作工具。')
  } catch {
    setActionMessage(resumeId, '当前浏览器未允许剪贴板写入，请改用导出 Markdown。')
  }
}

const exportHrActionWorkflow = (rec: any) => {
  const resumeId = rec.resumeId as string
  actionExportState[resumeId] = true
  try {
    const markdown = buildHrActionWorkflowMarkdown(rec)
    const blob = new Blob([markdown], { type: 'text/markdown;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    const baseName = (rec?.candidate?.candidateName || `candidate-${resumeId}`).replace(/\s+/g, '-').toLowerCase()
    anchor.href = url
    anchor.download = `${baseName}-hr-action-plan.md`
    document.body.appendChild(anchor)
    anchor.click()
    document.body.removeChild(anchor)
    window.URL.revokeObjectURL(url)
    setActionMessage(resumeId, 'Markdown 行动清单已导出。')
  } finally {
    actionExportState[resumeId] = false
  }
}

const getRadarOption = (rec: any) => {
  const breakdown = rec.scoreBreakdown || {};
  
  return {
    tooltip: { trigger: 'item' },
    radar: {
      indicator: [
        { name: '技能匹配', max: 100 },
        { name: '工作经验', max: 100 },
        { name: '教育背景', max: 100 },
        { name: '语义匹配', max: 100 }
      ],
      radius: '60%',
      splitArea: {
        areaStyle: {
          color: ['rgba(250,250,250,0.3)', 'rgba(200,200,200,0.1)']
        }
      }
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: [
              breakdown.skillScore || 0,
              breakdown.experienceScore || 0,
              breakdown.educationScore || 0,
              breakdown.semanticScore || 0
            ],
            name: '候选人画像',
            areaStyle: { color: 'rgba(99, 102, 241, 0.2)' },
            lineStyle: { color: 'rgba(99, 102, 241, 1)' },
            itemStyle: { color: 'rgba(99, 102, 241, 1)' }
          }
        ]
      }
    ]
  };
};

const getDimensionCards = (rec: any) => {
  const breakdown = rec.scoreBreakdown || {}
  return [
    { key: 'skill', label: '技能匹配', value: Number(breakdown.skillScore || 0), textClass: 'text-blue-600', barClass: 'bg-blue-500' },
    { key: 'experience', label: '经验匹配', value: Number(breakdown.experienceScore || 0), textClass: 'text-indigo-600', barClass: 'bg-indigo-500' },
    { key: 'education', label: '教育匹配', value: Number(breakdown.educationScore || 0), textClass: 'text-purple-600', barClass: 'bg-purple-500' },
    { key: 'semantic', label: '语义匹配', value: Number(breakdown.semanticScore || 0), textClass: 'text-emerald-600', barClass: 'bg-emerald-500' },
  ]
}
</script>
