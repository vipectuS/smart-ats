<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Plus, Briefcase, ChevronRight, AlertCircle, RefreshCw, Loader2 } from 'lucide-vue-next';
import api from '@/utils/api';
import { resolveApiError } from '@/utils/apiError';
import SkillAutoComplete from '@/components/SkillAutoComplete.vue';
import type { PublicSkillCatalogItem } from '@/types/skills';
import { buildJobRequirementsPayload } from '@/utils/jobRequirements';

const loading = ref(true);
const errorMsg = ref('');
const jobs = ref<any[]>([]);
const totalElements = ref(0);
const showCreatePanel = ref(false);
const createLoading = ref(false);
const createError = ref('');
const availableSkills = ref<PublicSkillCatalogItem[]>([]);

const createForm = ref({
  title: '',
  description: '',
  department: '',
  location: '',
  headcount: 1,
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

const route = useRoute();
const router = useRouter();

const openCreatePanel = () => {
  createError.value = '';
  showCreatePanel.value = true;
};

const closeCreatePanel = () => {
  showCreatePanel.value = false;
};

const resetCreateForm = () => {
  createForm.value = {
    title: '',
    description: '',
    department: '',
    location: '',
    headcount: 1,
    seniority: '',
    employmentType: '',
    education: '',
    salaryRange: '',
    experienceYears: '',
    skills: [],
    responsibilities: '',
    experienceKeywords: '',
    educationKeywords: '',
    highlights: '',
  };
};

const submitCreateJob = async () => {
  if (!createForm.value.title.trim()) {
    createError.value = '请填写岗位标题';
    return;
  }
  if (!createForm.value.description.trim()) {
    createError.value = '请填写岗位描述';
    return;
  }
  if (createForm.value.skills.length === 0) {
    createError.value = '请至少从技能词典中选择一项技能';
    return;
  }

  createLoading.value = true;
  createError.value = '';
  try {
    const payload = {
      title: createForm.value.title.trim(),
      description: createForm.value.description.trim(),
      requirements: buildJobRequirementsPayload(
        {
          department: createForm.value.department,
          location: createForm.value.location,
          headcount: createForm.value.headcount,
          seniority: createForm.value.seniority,
          employmentType: createForm.value.employmentType,
          education: createForm.value.education,
          salaryRange: createForm.value.salaryRange,
          experienceYears: createForm.value.experienceYears,
          skills: createForm.value.skills,
          responsibilitiesText: createForm.value.responsibilities,
          experienceKeywordsText: createForm.value.experienceKeywords,
          educationKeywordsText: createForm.value.educationKeywords,
          highlightsText: createForm.value.highlights,
        },
        { defaultDepartment: '通用招聘', defaultLocation: '待定' },
      ),
    };

    const res: any = await api.post('/jobs', payload);
    const createdId = res?.data?.id;
    closeCreatePanel();
    resetCreateForm();
    await fetchJobs();
    if (createdId) {
      router.push(`/jobs/${createdId}`);
    }
  } catch (err: any) {
    createError.value = resolveApiError(err, '发布岗位失败').summary;
  } finally {
    createLoading.value = false;
  }
};

const goToJob = (id: number) => {
  router.push(`/jobs/${id}`);
};

const fetchJobs = async () => {
  loading.value = true;
  errorMsg.value = '';
  try {
    const res = await api.get('/jobs', { params: { size: 100 } });
    if (res.data && res.data.content) {
      jobs.value = res.data.content;
      totalElements.value = res.data.totalElements || jobs.value.length;
    } else {
      jobs.value = res.data || [];
      totalElements.value = jobs.value.length;
    }
  } catch (err: any) {
    errorMsg.value = resolveApiError(err, '获取职位列表失败').summary;
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchJobs();
  api.get('/skills/catalog').then((res) => {
    availableSkills.value = res.data || [];
  }).catch(async () => {
    try {
      const res: any = await api.get('/skills');
      availableSkills.value = (res.data || []).map((name: string) => ({ name, aliases: [] }));
    } catch {
      availableSkills.value = [];
    }
  });
  if (route.query.create === '1') {
    openCreatePanel();
  }
});

watch(
  () => route.query.create,
  (value) => {
    if (value === '1') {
      openCreatePanel();
    }
  }
);
</script>

<template>
  <div class="h-full flex flex-col space-y-6">
    <div
      v-if="showCreatePanel"
      class="fixed inset-0 z-50 bg-slate-900/40 flex items-center justify-center p-4"
      @click.self="closeCreatePanel"
    >
      <div class="w-full max-w-2xl max-h-[90vh] rounded-2xl border border-slate-200 bg-white shadow-xl overflow-hidden flex flex-col">
        <div class="px-6 py-4 border-b border-slate-200 flex items-center justify-between">
          <h2 class="text-lg font-semibold text-slate-900">发布岗位</h2>
          <button @click="closeCreatePanel" class="text-slate-400 hover:text-slate-600">关闭</button>
        </div>
        <div class="flex-1 overflow-y-auto px-6 py-5 space-y-4">
          <div v-if="createError" class="bg-rose-50 text-rose-700 border border-rose-200 rounded-lg px-3 py-2 text-sm">
            {{ createError }}
          </div>

          <div class="space-y-2">
            <label class="text-sm font-medium text-slate-700">岗位标题</label>
            <input v-model="createForm.title" type="text" class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" placeholder="例如：高级前端工程师" />
          </div>

          <div class="space-y-2">
            <label class="text-sm font-medium text-slate-700">岗位描述</label>
            <textarea v-model="createForm.description" rows="5" class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" placeholder="请输入岗位职责与任职要求"></textarea>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
            <div class="space-y-2">
              <label class="text-sm font-medium text-slate-700">部门</label>
              <input v-model="createForm.department" type="text" class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" placeholder="例如：技术部" />
            </div>
            <div class="space-y-2">
              <label class="text-sm font-medium text-slate-700">地点</label>
              <input v-model="createForm.location" type="text" class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" placeholder="例如：杭州" />
            </div>
            <div class="space-y-2">
              <label class="text-sm font-medium text-slate-700">招聘人数</label>
              <input v-model.number="createForm.headcount" type="number" min="1" class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div class="space-y-2">
              <label class="text-sm font-medium text-slate-700">职级</label>
              <input v-model="createForm.seniority" type="text" class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" placeholder="例如：中高级" />
            </div>
            <div class="space-y-2">
              <label class="text-sm font-medium text-slate-700">用工形式</label>
              <select v-model="createForm.employmentType" class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500">
                <option value="">请选择</option>
                <option value="全职">全职</option>
                <option value="实习">实习</option>
                <option value="校招">校招</option>
                <option value="外包/合同">外包/合同</option>
              </select>
            </div>
            <div class="space-y-2">
              <label class="text-sm font-medium text-slate-700">学历要求</label>
              <input v-model="createForm.education" type="text" class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" placeholder="例如：本科及以上" />
            </div>
            <div class="space-y-2">
              <label class="text-sm font-medium text-slate-700">薪资范围</label>
              <input v-model="createForm.salaryRange" type="text" class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" placeholder="例如：20k-35k" />
            </div>
            <div class="space-y-2 md:col-span-2">
              <label class="text-sm font-medium text-slate-700">经验年限（XAI评分用）</label>
              <input v-model.number="createForm.experienceYears" type="number" min="0" max="30" class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" placeholder="例如：3" />
            </div>
          </div>

          <div class="space-y-2">
            <label class="text-sm font-medium text-slate-700">技能要求</label>
            <div class="rounded-lg border border-slate-200 bg-slate-50 p-3">
              <SkillAutoComplete
                v-model="createForm.skills"
                :available-skills="availableSkills"
                placeholder="检索技能词典并选择已有技能，不能自由录入"
              />
            </div>
            <p class="text-xs text-slate-500">仅允许添加技能词典中的已启用技能，支持模糊检索与键盘选择。</p>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div class="space-y-2 md:col-span-2">
              <label class="text-sm font-medium text-slate-700">核心职责（每行一条）</label>
              <textarea v-model="createForm.responsibilities" rows="3" class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" placeholder="例如：\n负责招聘系统核心功能设计\n推动跨团队协作交付"></textarea>
            </div>
            <div class="space-y-2">
              <label class="text-sm font-medium text-slate-700">经验关键词（每行一条）</label>
              <textarea v-model="createForm.experienceKeywords" rows="3" class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" placeholder="例如：\nmicroservice\nhigh concurrency"></textarea>
            </div>
            <div class="space-y-2">
              <label class="text-sm font-medium text-slate-700">教育关键词（每行一条）</label>
              <textarea v-model="createForm.educationKeywords" rows="3" class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" placeholder="例如：\ncomputer science\nsoftware engineering"></textarea>
            </div>
            <div class="space-y-2 md:col-span-2">
              <label class="text-sm font-medium text-slate-700">岗位亮点（每行一条）</label>
              <textarea v-model="createForm.highlights" rows="3" class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" placeholder="例如：\n核心业务场景\n成长空间明确"></textarea>
            </div>
          </div>
        </div>
        <div class="px-6 py-4 border-t border-slate-200 bg-slate-50 flex items-center justify-end gap-3 shrink-0">
          <button @click="closeCreatePanel" class="px-4 py-2 rounded-lg border border-slate-300 text-slate-700 hover:bg-slate-100 text-sm">取消</button>
          <button
            @click="submitCreateJob"
            :disabled="createLoading"
            class="px-4 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium disabled:opacity-60"
          >
            {{ createLoading ? '发布中...' : '确认发布' }}
          </button>
        </div>
      </div>
    </div>
    
    <!-- Page Header -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold tracking-tight text-slate-900">职位管理</h1>
        <p class="mt-1 text-sm text-slate-500">管理所有发布的职位并追踪候选人进度</p>
      </div>
      <div class="flex items-center gap-3">
        <div class="hidden sm:flex items-center gap-2 px-3 py-1.5 bg-white border border-slate-200 rounded-lg text-sm text-slate-600 shadow-sm">
          <span>共 {{ totalElements }} 个岗位</span>
        </div>
        <button 
          @click="openCreatePanel" 
          class="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-lg font-medium shadow-sm transition-colors text-sm"
        >
          <Plus class="w-4 h-4" />
          发布岗位
        </button>
      </div>
    </div>

    <div v-if="errorMsg" class="bg-rose-50 text-rose-700 p-4 rounded-xl border border-rose-200 flex items-center shrink-0">
      <AlertCircle class="w-5 h-5 mr-2" />
      {{ errorMsg }}
    </div>

    <!-- Job List -->
    <div v-else class="bg-white border border-slate-200 rounded-xl shadow-sm overflow-hidden flex-1 flex flex-col">
      
      <!-- Sub-header/Filters -->
      <div class="px-6 py-4 border-b border-slate-200 bg-slate-50 flex justify-between items-center">
         <h2 class="text-sm font-semibold text-slate-900">活跃岗位</h2>
         <div class="flex items-center gap-2">
           <button @click="fetchJobs" class="p-1.5 text-slate-400 hover:text-slate-600 rounded-md hover:bg-slate-100 transition-colors">
              <RefreshCw class="w-4 h-4" :class="{'animate-spin': loading}" />
           </button>
         </div>
      </div>

      <!-- List Data -->
      <div class="flex-1 overflow-y-auto p-0 bg-slate-50">
        <div v-if="loading" class="flex flex-col items-center justify-center p-12 text-slate-500">
           <Loader2 class="w-8 h-8 animate-spin mb-4 text-indigo-600" />
           <p class="text-sm">正在加载数据...</p>
        </div>
        <div v-else-if="jobs.length === 0" class="flex flex-col items-center justify-center p-12 text-slate-500">
           <Briefcase class="w-12 h-12 mb-4 text-slate-300" />
           <p class="text-sm">尚未发布任何岗位</p>
        </div>
        <ul v-else class="divide-y divide-slate-100">
          <li 
            v-for="job in jobs" 
            :key="job.id"
            @click="goToJob(job.id)"
            class="hover:bg-slate-50 bg-white transition-colors cursor-pointer group"
          >
            <div class="px-6 py-5 flex items-center justify-between">
               <div class="flex items-center gap-4">
                  <div class="w-10 h-10 rounded-lg bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-600 shrink-0">
                     <Briefcase class="w-5 h-5" />
                  </div>
                  <div>
                    <h3 class="text-base font-semibold text-slate-900 group-hover:text-indigo-600 transition-colors">{{ job.title }}</h3>
                    <div class="mt-1 flex items-center gap-3 text-sm text-slate-500">
                      <span class="flex items-center gap-1">{{ job.department || '未分配部门' }}</span>
                      <span class="w-1 h-1 rounded-full bg-slate-300"></span>
                      <span class="flex items-center gap-1">{{ job.location || '未知位置' }}</span>
                    </div>
                  </div>
               </div>
               
               <div class="flex items-center gap-6">
                 <div class="text-right hidden sm:block">
                   <div class="text-sm font-medium text-slate-900">{{ job.candidateCount || 0 }} <span class="text-slate-500 text-xs font-normal">位候选人</span></div>
                 </div>
                 
                 <span 
                  class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border"
                  :class="job.status === 'ACTIVE' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-slate-100 text-slate-600 border-slate-200'"
                 >
                   {{ job.status === 'ACTIVE' ? '招聘中' : '已关闭' }}
                 </span>
                 
                 <ChevronRight class="w-5 h-5 text-slate-400 group-hover:text-indigo-500 transition-colors" />
               </div>
            </div>
          </li>
        </ul>
      </div>

    </div>
  </div>
</template>
