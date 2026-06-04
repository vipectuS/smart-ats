<template>
  <div class="min-w-0 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
    <div class="flex items-start justify-between gap-4">
      <div class="min-w-0">
        <h2 class="text-xl font-bold text-slate-800">解析摘要 (Talent Profile)</h2>
        <p class="mt-1 text-sm text-slate-500">展示当前已结构化的候选人画像， 便于 HR 快速判断简历质量。</p>
      </div>
      <div class="flex items-center justify-center rounded-full bg-blue-50 p-2">
         <User class="w-5 h-5 text-blue-600" />
      </div>
    </div>

    <div v-if="hasParsedData" class="mt-6 space-y-6">
      <div class="rounded-xl border border-slate-200 bg-slate-50 p-5">
        <p class="flex items-center gap-2 text-sm font-semibold text-slate-700">
          <BrainCircuit class="w-4 h-4 text-blue-500" /> 候选人 AI 摘要
        </p>
        <p class="mt-2 break-words text-sm leading-7 text-slate-600">{{ summaryText }}</p>
      </div>

      <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
        <div class="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
          <p class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Headline</p>
          <p class="mt-2 break-words text-sm font-medium text-slate-800">{{ basicInfo.headline || '未提取到职位概述' }}</p>
        </div>
        <div class="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
          <p class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400 flex items-center gap-1">
             <MapPin class="w-3.5 h-3.5" /> Location
          </p>
          <p class="mt-2 break-words text-sm font-medium text-slate-800">{{ basicInfo.location || '未提取到地点' }}</p>
        </div>
      </div>

      <div>
        <p class="mb-3 text-sm font-semibold text-slate-700">技能标签</p>
        <div class="flex flex-wrap gap-2">
          <span v-for="skill in skillNames" :key="skill" class="rounded-full border border-blue-200 bg-blue-50 px-3 py-1.5 text-xs font-medium text-blue-700">
            {{ skill }}
          </span>
          <span v-if="skillNames.length === 0" class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-medium text-slate-500">
            当前没有解析到技能标签
          </span>
        </div>
      </div>

      <div>
        <p class="mb-3 flex items-center gap-2 text-sm font-semibold text-slate-700">
          <Briefcase class="w-4 h-4 text-slate-500" /> 工作经历
        </p>
        <div class="space-y-4">
          <article v-for="(item, index) in workExperiences" :key="`${item.company || 'comp'}-${index}`" class="min-w-0 rounded-xl border border-slate-200 bg-white p-5 shadow-sm transition-colors hover:border-slate-300">
            <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between border-b border-slate-100 pb-3">
              <div class="min-w-0">
                <h3 class="break-words text-sm font-bold text-slate-800">{{ item.title || '未命名岗位' }}</h3>
                <p class="mt-1 break-words text-xs font-medium text-slate-500">{{ item.company || '未知公司' }}</p>
              </div>
              <span class="shrink-0 rounded-full border border-slate-200 bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600 shadow-sm">
                {{ [item.startDate, item.endDate].filter(Boolean).join(' - ') || '时间未提取' }}
              </span>
            </div>
            <ul v-if="item.responsibilities?.length" class="mt-3 space-y-1.5 text-sm text-slate-600 list-disc list-inside pl-1">
              <li v-for="resp in item.responsibilities" :key="resp">{{ resp }}</li>
            </ul>
            <p v-else class="mt-3 text-sm italic text-slate-400">当前没有解析到职责详情。</p>
          </article>
          <div v-if="workExperiences.length === 0" class="rounded-xl border border-dashed border-slate-200 bg-slate-50 p-8 text-center text-slate-500">
            当前没有解析到工作经历。
          </div>
        </div>
      </div>

      <div>
        <p class="mb-3 flex items-center gap-2 text-sm font-semibold text-slate-700">
          <GraduationCap class="w-4 h-4 text-slate-500" /> 教育经历
        </p>
        <div class="space-y-4">
          <article v-for="(item, index) in educationExperiences" :key="`${item.school || 'sch'}-${index}`" class="min-w-0 rounded-xl border border-slate-200 bg-white p-5 shadow-sm transition-colors hover:border-slate-300">
            <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
              <div class="min-w-0">
                <h3 class="break-words text-sm font-bold leading-6 text-slate-800">{{ item.school || '未知院校' }}</h3>
                <p class="mt-1 break-words text-xs font-medium text-slate-500">{{ [item.degree, item.fieldOfStudy].filter(Boolean).join(' / ') || '学历字段未提取' }}</p>
              </div>
              <span class="shrink-0 rounded-full border border-slate-200 bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600 shadow-sm">
                {{ [item.startDate, item.endDate].filter(Boolean).join(' - ') || '时间未提取' }}
              </span>
            </div>
          </article>
          <div v-if="educationExperiences.length === 0" class="rounded-xl border border-dashed border-slate-200 bg-slate-50 p-8 text-center text-slate-500">
            当前没有解析到教育经历。
          </div>
        </div>
      </div>
    </div>

    <div v-else class="mt-6 rounded-xl border border-dashed border-slate-200 bg-slate-50 p-10 text-center text-slate-500 flex flex-col items-center justify-center">
      <Brain class="w-12 h-12 text-slate-300 mb-4" />
      <p class="text-lg font-semibold text-slate-700">当前还没有可展示的解析画像</p>
      <p class="mt-2 text-sm text-slate-400">如果简历仍处于待解析或解析异常状态，可以在右上角重新投递解析。</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { BrainCircuit, Briefcase, GraduationCap, MapPin, User, Brain } from 'lucide-vue-next';

defineProps<{
  hasParsedData: boolean;
  summaryText: string;
  basicInfo: any;
  skillNames: string[];
  workExperiences: any[];
  educationExperiences: any[];
}>();
</script>
