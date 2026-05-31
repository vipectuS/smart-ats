<template>
  <div class="space-y-8">
    <div v-if="!resumeDetail || !resumeDetail.parsedData" class="rounded-xl border border-slate-200 bg-white p-12 text-center shadow-sm">
      <FileQuestion class="mx-auto mb-4 h-12 w-12 text-slate-300" />
      <h3 class="text-lg font-medium text-slate-700">暂无结构化履历</h3>
      <p class="mt-2 text-sm text-slate-500">上传您的简历 PDF，系统将自动提取履历信息。</p>
    </div>
    
    <template v-else>
      <div v-if="basicInfo" class="rounded-xl border border-slate-200 bg-white p-8 shadow-sm">
        <h3 class="mb-6 flex items-center gap-2 text-lg font-bold text-slate-800">
          <BookUser class="h-5 w-5 text-blue-600" /> 提炼画像
        </h3>
        <div class="grid grid-cols-2 gap-4 md:grid-cols-4">
          <div class="rounded-lg bg-slate-50 p-4">
            <p class="text-xs text-slate-500 mb-1">工龄</p>
            <p class="font-semibold text-slate-800">{{ basicInfo.years_of_experience || basicInfo.yearsOfExperience || '未知' }}</p>
          </div>
          <div class="rounded-lg bg-slate-50 p-4">
            <p class="text-xs text-slate-500 mb-1">姓名</p>
            <p class="font-semibold text-slate-800">{{ basicInfo.name || basicInfo.fullName || '未知' }}</p>
          </div>
          <div class="rounded-lg bg-slate-50 p-4">
            <p class="text-xs text-slate-500 mb-1">手机</p>
            <p class="font-semibold text-slate-800">{{ basicInfo.phone_number || basicInfo.phone || '未知' }}</p>
          </div>
          <div class="rounded-lg bg-slate-50 p-4">
            <p class="text-xs text-slate-500 mb-1">邮箱</p>
            <p class="font-semibold text-slate-800">{{ basicInfo.email || '未知' }}</p>
          </div>
        </div>
      </div>
      
      <div v-if="skillNames.length" class="rounded-xl border border-slate-200 bg-white p-8 shadow-sm">
        <h3 class="mb-4 flex items-center gap-2 text-lg font-bold text-slate-800">
          <Wrench class="h-5 w-5 text-blue-600" /> AI 提取技能戳
        </h3>
        <div class="flex flex-wrap gap-2">
          <span
            v-for="(skill, index) in skillNames"
            :key="index"
            class="inline-flex items-center rounded-lg bg-indigo-50 px-3 py-1 text-sm font-medium text-indigo-700 border border-indigo-100"
          >
            {{ skill }}
          </span>
        </div>
      </div>

      <div v-if="workExperiences.length" class="rounded-xl border border-slate-200 bg-white p-8 shadow-sm">
         <h3 class="mb-6 flex items-center gap-2 text-lg font-bold text-slate-800">
          <Briefcase class="h-5 w-5 text-blue-600" /> 工作经历
        </h3>
        <div class="space-y-6">
          <div
            v-for="(work, index) in workExperiences"
            :key="index"
            class="relative pl-6 border-l-2 border-slate-200 last:border-0 pb-6 last:pb-0"
          >
            <div class="absolute -left-[9px] top-0 h-4 w-4 rounded-full bg-white border-2 border-blue-500"></div>
            <div class="flex items-start justify-between">
              <div>
                <h4 class="font-bold text-slate-800">{{ work.company_name || work.company || '未知公司' }}</h4>
                <p class="text-sm font-medium text-slate-600">{{ work.job_title || work.title || '未知岗位' }}</p>
              </div>
              <span class="text-xs font-medium text-slate-500 bg-slate-100 px-2 py-1 rounded">
                {{ formatDate(work.start_date || work.startDate) }} - {{ formatDate(work.end_date || work.endDate) }}
              </span>
            </div>
            <ul class="mt-3 list-inside list-disc space-y-1 text-sm text-slate-600">
               <li v-for="(desc, idx) in normalizeWorkDescriptions(work)" :key="idx">{{ desc }}</li>
            </ul>
          </div>
        </div>
      </div>

      <div v-if="educationExperiences.length" class="rounded-xl border border-slate-200 bg-white p-8 shadow-sm">
         <h3 class="mb-6 flex items-center gap-2 text-lg font-bold text-slate-800">
          <GraduationCap class="h-5 w-5 text-blue-600" /> 教育经历
        </h3>
        <div class="space-y-4">
           <div
            v-for="(edu, index) in educationExperiences"
            :key="index"
            class="flex items-center justify-between rounded-lg border border-slate-100 bg-slate-50 p-4"
          >
            <div>
              <h4 class="font-bold text-slate-800">{{ edu.school_name || edu.school || '未知院校' }}</h4>
              <p class="text-sm text-slate-600">{{ edu.degree || '未知学历' }} · {{ edu.major || edu.fieldOfStudy || '未知专业' }}</p>
            </div>
             <span class="text-xs font-medium text-slate-500">
                {{ formatDate(edu.start_date || edu.startDate) }} - {{ formatDate(edu.end_date || edu.endDate) }}
             </span>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { BookUser, Wrench, Briefcase, GraduationCap, FileQuestion } from 'lucide-vue-next';

const props = defineProps<{
  resumeDetail: any;
}>();

const parsedData = computed(() => props.resumeDetail?.parsedData || null);
const basicInfo = computed(() => {
  if (!parsedData.value) return null;
  return parsedData.value.basicInfo || parsedData.value.basic_info || parsedData.value.candidateProfile || null;
});

const skillNames = computed(() => {
  const rawSkills = parsedData.value?.skills;
  if (!Array.isArray(rawSkills)) return [];
  return rawSkills
    .map((skill: any) => (typeof skill === 'string' ? skill : (skill?.name || '')))
    .filter((name: string) => Boolean(name));
});

const workExperiences = computed(() => {
  if (!parsedData.value) return [];
  const works = parsedData.value.workExperiences || parsedData.value.work_experiences;
  return Array.isArray(works) ? works : [];
});

const educationExperiences = computed(() => {
  if (!parsedData.value) return [];
  const educations = parsedData.value.educationExperiences || parsedData.value.educations;
  return Array.isArray(educations) ? educations : [];
});

const normalizeWorkDescriptions = (work: any): string[] => {
  const base = Array.isArray(work?.description) ? work.description : [];
  const responsibilities = Array.isArray(work?.responsibilities) ? work.responsibilities : [];
  const achievements = Array.isArray(work?.achievements) ? work.achievements : [];
  const merged = [...base, ...responsibilities, ...achievements].filter(Boolean);
  return merged.length > 0 ? merged : ['暂无描述'];
};

const formatDate = (dateStr?: string) => {
  if (!dateStr || dateStr === '至今' || dateStr === 'Present') return dateStr || '至今';
  try {
    const d = new Date(dateStr);
    return isNaN(d.getTime()) ? dateStr : `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}`;
  } catch {
    return dateStr;
  }
};
</script>
