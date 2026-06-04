<template>
  <div class="min-w-0 space-y-6">
    <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm transition-shadow hover:shadow-md">
      <h2 class="text-lg font-bold text-slate-800 flex items-center gap-2 border-b border-slate-100 pb-3">
        <FileText class="w-4 h-4 text-blue-500" />
        简历元信息
      </h2>
      <dl class="mt-4 space-y-4 text-sm text-slate-600">
        <div>
          <dt class="text-[0.65rem] font-bold uppercase tracking-[0.2em] text-slate-400 mb-1">Resume ID</dt>
          <dd class="break-all rounded-lg border border-slate-100 bg-slate-50 px-3 py-2 font-medium text-slate-800">{{ resume.id }}</dd>
        </div>
        <div>
          <dt class="text-[0.65rem] font-bold uppercase tracking-[0.2em] text-slate-400 mb-1">Raw Reference</dt>
          <dd class="break-all rounded-lg border border-slate-100 bg-slate-50 px-3 py-2 font-medium text-slate-800">{{ resume.rawContentReference }}</dd>
        </div>
        <div>
          <dt class="text-[0.65rem] font-bold uppercase tracking-[0.2em] text-slate-400 mb-1">Candidate Name</dt>
          <dd class="break-words rounded-lg border border-slate-100 bg-slate-50 px-3 py-2 font-medium text-slate-800">{{ displayName }}</dd>
        </div>
        <div>
          <dt class="text-[0.65rem] font-bold uppercase tracking-[0.2em] text-slate-400 mb-1">Contact</dt>
          <dd class="break-all rounded-lg border border-slate-100 bg-slate-50 px-3 py-2 font-medium text-slate-800">{{ resume.contactInfo || '未提供' }}</dd>
        </div>
      </dl>
    </div>

    <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm transition-shadow hover:shadow-md">
      <h2 class="text-lg font-bold text-slate-800 flex items-center gap-2 border-b border-slate-100 pb-3">
        <AlertCircle class="w-4 h-4 text-amber-500" />
        解析状态跟踪
      </h2>
      <div class="mt-4 space-y-4 text-sm text-slate-600">
        <div class="rounded-xl border border-slate-200 bg-slate-50/70 p-4">
          <p class="font-semibold text-slate-700 text-xs mb-1">当前阶段</p>
          <p class="mt-1 font-medium">{{ statusHint }}</p>
        </div>
        <div v-if="resume.parseFailureReason" class="rounded-xl border border-rose-200 bg-rose-50 p-4 text-rose-700">
          <p class="font-bold text-xs uppercase tracking-wider mb-1 flex items-center gap-1"><Ban class="w-3.5 h-3.5"/> 解析失败原因</p>
          <p class="mt-1 opacity-90">{{ resume.parseFailureReason }}</p>
        </div>
        <ul class="space-y-2 text-xs italic text-slate-500 pl-1 list-disc list-inside">
          <li>如果长期停留在解析中，请检查 AI Backend。</li>
          <li>解析完成即可作为结构化画像查看。</li>
        </ul>
      </div>
    </div>
    
    <div class="min-w-0 rounded-2xl border border-slate-200 bg-slate-950 p-6 shadow-sm">
      <h2 class="text-lg font-bold text-slate-100 flex items-center gap-2 border-b border-slate-800 pb-3">
        <Code class="w-4 h-4 text-emerald-400" />
        原始 JSON 
      </h2>
      <pre class="mt-4 max-h-[420px] w-full overflow-auto rounded-xl border border-slate-800 bg-black/50 p-4 text-[0.72rem] leading-6 text-emerald-400">{{ parsedJson }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import { FileText, AlertCircle, Ban, Code } from 'lucide-vue-next';

defineProps<{
  resume: any;
  displayName: string;
  statusHint: string;
  parsedJson: string;
}>();
</script>
