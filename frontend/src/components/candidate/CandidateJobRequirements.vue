<template>
  <div class="rounded-2xl border border-slate-200 bg-white p-8 shadow-sm">
    <h2 class="text-xl font-bold text-slate-800">岗位要求</h2>
    
    <div class="mt-6 grid grid-cols-1 gap-6 md:grid-cols-2">
      <div>
        <p class="mb-3 text-sm font-semibold text-slate-700">技能要求</p>
        <div class="flex flex-wrap gap-2">
          <span v-for="skill in requiredSkills" :key="skill" class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-medium text-slate-700">
            {{ skill }}
          </span>
          <span v-if="requiredSkills.length === 0" class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-medium text-slate-500">未配置技能要 求</span>
        </div>
      </div>
      <div>
        <p class="mb-3 text-sm font-semibold text-slate-700">技术匹配度对照</p>
        <div class="space-y-4">
          <div>
            <span class="text-xs font-semibold text-emerald-600">已具备</span>
            <div class="mt-1 flex flex-wrap gap-1.5">
              <span v-for="skill in matchedSkills" :key="skill" class="rounded-full bg-emerald-100 px-2.5 py-0.5 text-xs text-emerald-800">{{ skill }}</span>
              <span v-if="matchedSkills.length === 0" class="text-xs text-slate-400">无</span>
            </div>
          </div>
          <div>
            <span class="text-xs font-semibold text-rose-600">尚有欠缺</span>
            <div class="mt-1 flex flex-wrap gap-1.5">
              <span v-for="skill in missingSkills" :key="skill" class="rounded-full bg-rose-100 px-2.5 py-0.5 text-xs text-rose-800">{{ skill }}</span>
              <span v-if="missingSkills.length === 0" class="text-xs text-slate-400">无</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  requiredSkills: string[];
  matchedSkills: string[];
  missingSkills: string[];
}>();
</script>
