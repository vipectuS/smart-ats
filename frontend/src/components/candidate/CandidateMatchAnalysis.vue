<template>
  <div class="rounded-2xl border border-slate-200 bg-white p-8 shadow-sm">
    <div class="flex items-center justify-between gap-4">
      <div>
        <h2 class="text-xl font-bold text-slate-800">个性化匹配分析</h2>
        <p class="mt-1 text-sm text-slate-500">基于当前候选人资料与最新简历生 成的岗位适配解释。</p>
      </div>
      <span :class="fitBandClass" class="rounded-full px-3 py-1 text-xs font-semibold">{{ fitBandLabel }}</span>
    </div>

    <div v-if="recommendation" class="mt-6 space-y-6">
      <div class="rounded-xl border border-slate-200 bg-slate-50 p-5">
        <p class="text-sm font-semibold text-slate-700">AI 综合摘要</p>
        <p class="mt-2 text-sm leading-7 text-slate-600">{{ recommendation.xaiReport.summary || recommendation.suitabilityReport }}</p>
      </div>

      <div class="grid grid-cols-1 gap-6 md:grid-cols-2">
        <div>
          <p class="mb-3 text-sm font-semibold text-slate-700">匹配优势</p>
          <ul class="space-y-2 text-sm text-slate-600">
            <li v-for="item in recommendation.xaiReport.strengths" :key="item" class="rounded-xl bg-emerald-50 px-4 py-3 text-emerald-800">{{ item }}</li>
            <li v-if="recommendation.xaiReport.strengths.length === 0" class="rounded-xl bg-slate-50 px-4 py-3 text-slate-500">尚无明显优势说明。</li>
          </ul>
        </div>
        <div>
          <p class="mb-3 text-sm font-semibold text-slate-700">风险与差距</p>
          <ul class="space-y-2 text-sm text-slate-600">
            <li v-for="item in recommendation.xaiReport.risks" :key="item" class="rounded-xl bg-rose-50 px-4 py-3 text-rose-800">{{ item }}</li>
            <li v-if="recommendation.xaiReport.risks.length === 0" class="rounded-xl bg-slate-50 px-4 py-3 text-slate-500">暂无发现明显风险。</li>
          </ul>
        </div>
      </div>

      <div>
        <div class="mb-3 flex flex-wrap items-center justify-between gap-3">
          <div>
            <p class="text-sm font-semibold text-slate-700">行动建议</p>
            <p class="mt-1 text-xs text-slate-500">将 AI 建议拆成更容易执行的补强动作，而不只是一组文本标签。</p>
          </div>
          <span class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-medium text-slate-500">
            {{ actionPlanSections.reduce((total, section) => total + section.items.length, 0) }} 项建议
          </span>
        </div>

        <div v-if="actionPlanSections.length" class="grid grid-cols-1 gap-4 md:grid-cols-2">
          <article
            v-for="section in actionPlanSections"
            :key="section.key"
            class="rounded-2xl border px-4 py-4"
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

        <div v-else class="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-500">
          暂无额外行动建议。
        </div>
      </div>

      <div class="rounded-xl border border-blue-100 bg-blue-50 p-5">
        <p class="text-sm font-semibold text-blue-800">完整深度分析</p>
        <p class="mt-2 text-sm leading-7 text-blue-900/90">{{ recommendation.xaiReport.narrative || recommendation.suitabilityReport }}</p>
      </div>
    </div>

    <div v-else class="mt-6 rounded-xl border border-dashed border-slate-200 bg-slate-50 p-8 text-center text-slate-500">
      <div v-if="errorMsg" class="mb-4 rounded-xl border border-amber-200 bg-amber-50 p-4 text-left text-sm text-amber-900">
        {{ errorMsg }}
      </div>
      <div class="mb-3 text-4xl flex justify-center"><Compass class="w-10 h-10 text-slate-400" /></div>
      <p class="font-semibold text-slate-700">当前没有该岗位的个性化分析</p>
      <p class="mt-2 text-sm">可能是资料不完整，或者尚未生成。请完善资料后再进行评估。</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { BookOpenCheck, Compass, FileText, MessagesSquare, Sparkles } from 'lucide-vue-next';

interface StructuredJobFitReport {
  headline: string;
  fitBand: string;
  summary: string;
  strengths: string[];
  risks: string[];
  improvementSuggestions: string[];
  nextSteps: string[];
  narrative: string;
}

interface Recommendation {
  jobId: string;
  xaiReport: StructuredJobFitReport;
  suitabilityReport: string;
}

type ActionSectionKey = 'training' | 'evidence' | 'interview' | 'next';

const props = defineProps<{
  recommendation: Recommendation | null;
  fitBandLabel: string;
  fitBandClass: string;
  errorMsg: string;
}>();

const actionSectionMeta = {
  training: {
    title: '培训补强',
    description: '优先处理技能、工具或认证层面的关键差距。',
    icon: BookOpenCheck,
    panelClass: 'border-emerald-200 bg-emerald-50/80',
    bulletClass: 'bg-emerald-500',
  },
  evidence: {
    title: '材料补充',
    description: '把项目、成果和岗位相关证据补进简历与作品说明。',
    icon: FileText,
    panelClass: 'border-blue-200 bg-blue-50/80',
    bulletClass: 'bg-blue-500',
  },
  interview: {
    title: '面试准备',
    description: '提前准备需要在后续沟通中展开说明的重点。',
    icon: MessagesSquare,
    panelClass: 'border-violet-200 bg-violet-50/80',
    bulletClass: 'bg-violet-500',
  },
  next: {
    title: '立即行动',
    description: '适合现在就执行的核对项与下一步动作。',
    icon: Sparkles,
    panelClass: 'border-amber-200 bg-amber-50/80',
    bulletClass: 'bg-amber-500',
  },
} as const;

const actionSectionOrder: ActionSectionKey[] = ['training', 'evidence', 'interview', 'next'];

const dedupeItems = (items: string[]) => Array.from(new Set(items.map((item) => item.trim()).filter(Boolean)));

const classifyActionItem = (item: string, source: 'suggestion' | 'nextStep'): ActionSectionKey => {
  const isInterview = /面试|二轮|沟通|表达|答辩|故事|interview|story|presentation|communication/i.test(item);
  const isSkillGap = /技能|补强|认证|课程|学习|训练|missing|skill|skills|tool|tools/i.test(item);
  const isEvidence = /简历|项目|成果|证据|作品集|经历|关键词|resume|project|evidence|portfolio|keyword|business result/i.test(item);

  if (isInterview) {
    return 'interview';
  }
  if (isEvidence && !isSkillGap) {
    return 'evidence';
  }
  if (isSkillGap) {
    return 'training';
  }
  if (isEvidence) {
    return 'evidence';
  }
  if (source === 'nextStep') {
    return 'next';
  }
  return 'next';
};

const actionPlanSections = computed(() => {
  const report = props.recommendation?.xaiReport;
  if (!report) {
    return [];
  }

  const groupedItems: Record<ActionSectionKey, string[]> = {
    training: [],
    evidence: [],
    interview: [],
    next: [],
  };

  report.improvementSuggestions.forEach((item) => {
    groupedItems[classifyActionItem(item, 'suggestion')].push(item);
  });

  report.nextSteps.forEach((item) => {
    groupedItems[classifyActionItem(item, 'nextStep')].push(item);
  });

  return actionSectionOrder
    .map((key) => ({
      key,
      ...actionSectionMeta[key],
      items: dedupeItems(groupedItems[key]),
    }))
    .filter((section) => section.items.length > 0);
});
</script>
