<template>
  <div class="fixed inset-0 z-40 flex justify-end bg-slate-950/30 backdrop-blur-sm">
    <div class="h-full w-full max-w-2xl overflow-y-auto border-l border-white/50 bg-[linear-gradient(180deg,rgba(255,255,255,0.98),rgba(244,247,251,0.98))] p-6 shadow-2xl">
      <div class="mb-6 flex items-start justify-between gap-4">
        <div>
          <p class="text-xs font-semibold uppercase tracking-[0.25em] text-blue-500">HR Workspace</p>
          <h2 class="mt-2 text-2xl font-bold text-slate-900">编辑岗位</h2>
          <p class="mt-2 text-sm text-slate-500">更新岗位要求后会重新生成岗位向量，建议保存后重新执行一次 AI 评估。</p>
        </div>
        <button @click="emit('close')" class="h-10 w-10 rounded-full border border-slate-200 bg-white text-slate-500 transition hover:border-slate-300 hover:text-slate-700 flex items-center justify-center">
          <X class="w-5 h-5" />
        </button>
      </div>

      <div v-if="editError" class="mb-5 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm flex items-center gap-2 text-rose-700">
        <AlertCircle class="w-4 h-4 flex-shrink-0" />
        <span>{{ editError }}</span>
      </div>

      <form class="space-y-6" @submit.prevent="submitJobUpdate">
        <section class="rounded-2xl border border-white/60 bg-white/80 p-5 shadow-sm">
          <h3 class="mb-4 text-sm font-semibold text-slate-800">岗位主信息</h3>
          <div class="grid grid-cols-1 gap-4">
            <label class="space-y-2">
              <span class="text-sm font-medium text-slate-700">岗位名称</span>
              <input v-model="editForm.title" type="text" maxlength="255" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100">
            </label>
            <label class="space-y-2">
              <span class="text-sm font-medium text-slate-700">岗位描述</span>
              <textarea v-model="editForm.description" rows="6" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100"></textarea>
            </label>
          </div>
        </section>

        <section class="rounded-2xl border border-white/60 bg-white/80 p-5 shadow-sm">
          <h3 class="mb-4 text-sm font-semibold text-slate-800">结构化要求</h3>
          <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
            <label class="space-y-2">
              <span class="text-sm font-medium text-slate-700">部门</span>
              <input v-model="editForm.department" type="text" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100">
            </label>
            <label class="space-y-2">
              <span class="text-sm font-medium text-slate-700">工作地点</span>
              <input v-model="editForm.location" type="text" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100">
            </label>
            <label class="space-y-2">
              <span class="text-sm font-medium text-slate-700">招聘人数</span>
              <input v-model="editForm.headcount" type="number" min="1" max="999" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100">
            </label>
            <label class="space-y-2">
              <span class="text-sm font-medium text-slate-700">职级</span>
              <input v-model="editForm.seniority" type="text" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100">
            </label>
            <label class="space-y-2">
              <span class="text-sm font-medium text-slate-700">用工形式</span>
              <select v-model="editForm.employmentType" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100">
                <option value="">请选择</option>
                <option value="全职">全职</option>
                <option value="实习">实习</option>
                <option value="校招">校招</option>
                <option value="外包/合同">外包/合同</option>
              </select>
            </label>
            <label class="space-y-2">
              <span class="text-sm font-medium text-slate-700">学历要求</span>
              <input v-model="editForm.education" type="text" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100">
            </label>
            <label class="space-y-2 md:col-span-2">
              <span class="text-sm font-medium text-slate-700">薪资区间</span>
              <input v-model="editForm.salaryRange" type="text" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100">
            </label>
            <label class="space-y-2 md:col-span-2">
              <span class="text-sm font-medium text-slate-700">经验年限（XAI评分用）</span>
              <input v-model="editForm.experienceYears" type="number" min="0" max="30" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100">
            </label>
          </div>
        </section>

        <section class="space-y-4 rounded-2xl border border-white/60 bg-white/80 p-5 shadow-sm">
          <div>
            <h3 class="text-sm font-semibold text-slate-800">关键技能与亮点</h3>
            <p class="mt-1 text-xs text-slate-500">技能会参与向量更新和推荐评估，保存后建议重新发起 AI 评估。</p>
          </div>

          <div class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
            <SkillAutoComplete
              v-model="editForm.skills"
              :availableSkills="availableSkills"
              placeholder="输入技能检索 (支持拼音或首字母检索)，按回车确认"
            />
          </div>

          <label class="block space-y-2">
            <span class="text-sm font-medium text-slate-700">核心职责（每行一条）</span>
            <textarea v-model="editForm.responsibilities" rows="4" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100"></textarea>
          </label>

          <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
            <label class="block space-y-2">
              <span class="text-sm font-medium text-slate-700">经验关键词（每行一条）</span>
              <textarea v-model="editForm.experienceKeywords" rows="4" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100"></textarea>
            </label>
            <label class="block space-y-2">
              <span class="text-sm font-medium text-slate-700">教育关键词（每行一条）</span>
              <textarea v-model="editForm.educationKeywords" rows="4" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100"></textarea>
            </label>
          </div>

          <label class="block space-y-2">
            <span class="text-sm font-medium text-slate-700">补充亮点</span>
            <textarea v-model="editForm.highlights" rows="4" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100"></textarea>
          </label>
        </section>

        <div class="flex items-center justify-end gap-3 pt-2">
          <button type="button" @click="emit('close')" class="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-slate-700 transition hover:bg-slate-50 font-medium">取消</button>
          <button type="submit" :disabled="savingJob" class="flex items-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 px-5 py-2.5 font-medium text-white shadow-md shadow-blue-500/20 transition hover:shadow-lg disabled:cursor-not-allowed disabled:opacity-70">
            <Loader2 v-if="savingJob" class="w-5 h-5 animate-spin" />
            <Save v-else class="w-5 h-5" />
            {{ savingJob ? '保存中...' : '保存岗位更新' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { X, Save, AlertCircle, Loader2 } from 'lucide-vue-next';
import api from '@/utils/api';
import { resolveApiError } from '@/utils/apiError';
import SkillAutoComplete from '@/components/SkillAutoComplete.vue';
import type { PublicSkillCatalogItem } from '@/types/skills';
import { buildJobRequirementsPayload } from '@/utils/jobRequirements';

const props = defineProps<{
  job: any;
  availableSkills: PublicSkillCatalogItem[];
}>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'saved', updatedJob: any): void;
}>();

const savingJob = ref(false);
const editError = ref('');

const editForm = reactive({
  title: '',
  description: '',
  department: '',
  location: '',
  headcount: '1',
  seniority: '',
  employmentType: '',
  education: '',
  salaryRange: '',
  experienceYears: '',
  skills: [] as string[],
  responsibilities: '',
  experienceKeywords: '',
  educationKeywords: '',
  highlights: '',
});

onMounted(() => {
  const jobData = props.job;
  if (!jobData) return;
  const requirements = jobData.requirements || {};
  editForm.title = jobData.title || '';
  editForm.description = jobData.description || '';
  editForm.department = requirements.department || '';
  editForm.location = requirements.location || '';
  editForm.headcount = requirements.headcount ? String(requirements.headcount) : '1';
  editForm.seniority = requirements.seniority || '';
  editForm.employmentType = requirements.employmentType || '';
  editForm.education = requirements.education || '';
  editForm.salaryRange = requirements.salaryRange || '';
  editForm.experienceYears = requirements.experienceYears ? String(requirements.experienceYears) : '';
  editForm.skills = Array.isArray(requirements.skills) ? requirements.skills.filter((skill: unknown) => typeof skill === 'string' && skill.trim()) : [];
  editForm.responsibilities = Array.isArray(requirements.responsibilities) ? requirements.responsibilities.join('\n') : '';
  editForm.experienceKeywords = Array.isArray(requirements.experienceKeywords) ? requirements.experienceKeywords.join('\n') : '';
  editForm.educationKeywords = Array.isArray(requirements.educationKeywords) ? requirements.educationKeywords.join('\n') : '';
  editForm.highlights = Array.isArray(requirements.highlights) ? requirements.highlights.join('\n') : '';
});

const submitJobUpdate = async () => {
  const title = editForm.title.trim();
  const description = editForm.description.trim();
  const requirements = buildJobRequirementsPayload({
    department: editForm.department,
    location: editForm.location,
    headcount: editForm.headcount,
    seniority: editForm.seniority,
    employmentType: editForm.employmentType,
    education: editForm.education,
    salaryRange: editForm.salaryRange,
    experienceYears: editForm.experienceYears,
    skills: editForm.skills,
    responsibilitiesText: editForm.responsibilities,
    experienceKeywordsText: editForm.experienceKeywords,
    educationKeywordsText: editForm.educationKeywords,
    highlightsText: editForm.highlights,
  });

  if (!title) {
    editError.value = '岗位名称不能为空。';
    return;
  }
  if (!description) {
    editError.value = '岗位描述不能为空。';
    return;
  }
  if (Object.keys(requirements).length === 0) {
    editError.value = '请至少保留一项结构化要求，例如地点、技能或招聘人数。';
    return;
  }

  try {
    savingJob.value = true;
    editError.value = '';
    const res: any = await api.put(`/jobs/${props.job.id}`, {
      title,
      description,
      requirements,
    });
    emit('saved', res.data);
  } catch (error: any) {
    console.error('Failed to update job', error);
    editError.value = resolveApiError(error, '岗位更新失败，请稍后重试。').summary;
  } finally {
    savingJob.value = false;
  }
};
</script>
