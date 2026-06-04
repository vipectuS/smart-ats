<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { Upload, FileText, Search, Calendar, ChevronRight, X, AlertCircle, Loader2, Inbox } from 'lucide-vue-next';
import api from '@/utils/api';
import { resolveApiError } from '@/utils/apiError';

const router = useRouter();
const searchQuery = ref('');
const activeFilter = ref('ALL');
const showUploadModal = ref(false);

const resumes = ref<any[]>([]);
const totalElements = ref(0);
const loading = ref(true);
const errorMsg = ref('');

const normalizeKeyword = (value: string) => value.trim().toLowerCase();

const extractResumeDisplayName = (resume: any) => {
  if (resume.candidateName?.trim()) return resume.candidateName.trim();
  const basicInfoName = resume.parsedData?.basicInfo?.fullName;
  if (typeof basicInfoName === 'string' && basicInfoName.trim()) return basicInfoName.trim();
  if (resume.status === 'PARSING' || resume.status === 'PENDING_PARSE') return '解析中...';
  return '未知候选人';
};

const extractResumeFileName = (resume: any) => {
  const rawReference = String(resume.rawContentReference || '');
  if (!rawReference) return '未命名文件';
  const normalized = rawReference.split('/').filter(Boolean);
  return normalized[normalized.length - 1] || rawReference;
};

const filteredResumes = computed(() => {
  const keyword = normalizeKeyword(searchQuery.value);

  return resumes.value.filter((resume) => {
    const matchesStatus = activeFilter.value === 'ALL'
      ? true
      : activeFilter.value === 'PENDING_PARSE'
        ? resume.status === 'PENDING_PARSE' || resume.status === 'PARSING'
        : resume.status === activeFilter.value;

    if (!matchesStatus) {
      return false;
    }

    if (!keyword) {
      return true;
    }

    const haystack = [
      extractResumeDisplayName(resume),
      extractResumeFileName(resume),
      resume.contactInfo || '',
      resume.rawContentReference || '',
    ].join(' ').toLowerCase();

    return haystack.includes(keyword);
  });
});

const fetchResumes = async () => {
  loading.value = true;
  errorMsg.value = '';
  try {
    const res = await api.get('/resumes', { params: { size: 100 } });
    if (res.data && res.data.content) {
      resumes.value = res.data.content;
      totalElements.value = res.data.totalElements || res.data.content.length;
    } else {
      resumes.value = res.data || [];
      totalElements.value = resumes.value.length;
    }
  } catch (err: any) {
    errorMsg.value = resolveApiError(err, '获取简历列表失败').summary;
  } finally {
    loading.value = false;
  }
};

const getStatusBadge = (status: string) => {
  if (status === 'PARSED') return 'bg-emerald-50 text-emerald-700 border-emerald-200';
  if (status === 'PENDING_PARSE') return 'bg-amber-50 text-amber-700 border-amber-200';
  if (status === 'PARSE_FAILED') return 'bg-rose-50 text-rose-700 border-rose-200';
  return 'bg-slate-100 text-slate-700 border-slate-200';
};

const getStatusText = (status: string) => {
  if (status === 'PARSED') return '解析成功';
  if (status === 'PENDING_PARSE') return '待解析/队列中';
  if (status === 'PARSING') return '解析中';
  if (status === 'PARSE_FAILED') return '解析失败';
  return status || '未知状态';
};

const goToResume = (id: number) => {
  router.push(`/resumes/${id}`);
};

const openUploadModal = () => {
  showUploadModal.value = true;
};

watch(searchQuery, () => {
  if (loading.value) return;
});

onMounted(() => {
  fetchResumes();
});
</script>

<template>
  <div class="h-full flex flex-col space-y-6 text-slate-900">
    
    <!-- Header Controls -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold tracking-tight">全部简历</h1>
        <p class="mt-1 text-sm text-slate-500">管理候选人简历，追踪状态与AI评估报告。</p>
      </div>

      <div class="flex items-center gap-3 w-full sm:w-auto">
        <div class="relative flex-1 sm:w-64">
           <Search class="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
           <input 
              v-model.lazy="searchQuery" 
              type="text" 
              placeholder="搜索姓名、邮箱或文件名" 
              class="w-full pl-9 pr-4 py-2 bg-white border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500 transition-all shadow-sm"
           />
        </div>

        <button 
          @click="openUploadModal" 
          class="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium shadow-sm transition-colors shrink-0"
        >
          <Upload class="w-4 h-4" />
          <span>导入简历</span>
        </button>
      </div>
    </div>

    <div v-if="errorMsg" class="bg-rose-50 text-rose-700 p-4 rounded-xl border border-rose-200 flex items-center shrink-0">
      <AlertCircle class="w-5 h-5 mr-2" />
      {{ errorMsg }}
    </div>

    <!-- Main List Container -->
    <div v-else class="bg-white border border-slate-200 rounded-xl shadow-sm overflow-hidden flex-1 flex flex-col">
       
        <!-- Filters Header -->
        <div class="px-6 py-0 border-b border-slate-200 bg-slate-50 flex space-x-6 text-sm font-medium overflow-x-auto">
             <button 
               @click="activeFilter = 'ALL'" 
               :class="activeFilter === 'ALL' ? 'text-indigo-600 border-b-2 border-indigo-600' : 'text-slate-500 hover:text-slate-900 border-b-2 border-transparent'" 
               class="py-3 px-1 transition-colors whitespace-nowrap"
             >
               全部简历
             </button>
             <button 
               @click="activeFilter = 'PENDING_PARSE'" 
               :class="activeFilter === 'PENDING_PARSE' ? 'text-indigo-600 border-b-2 border-indigo-600' : 'text-slate-500 hover:text-slate-900 border-b-2 border-transparent'" 
               class="py-3 px-1 transition-colors whitespace-nowrap"
             >
               待解析
             </button>
             <button 
               @click="activeFilter = 'PARSED'" 
               :class="activeFilter === 'PARSED' ? 'text-indigo-600 border-b-2 border-indigo-600' : 'text-slate-500 hover:text-slate-900 border-b-2 border-transparent'" 
               class="py-3 px-1 transition-colors whitespace-nowrap"
             >
               解析成功
             </button>
        </div>

        <!-- List Body -->
        <div class="flex-1 overflow-y-auto bg-slate-50">
           <div v-if="loading" class="flex flex-col items-center justify-center p-16 text-slate-500">
             <Loader2 class="w-8 h-8 animate-spin mb-4 text-indigo-600" />
             <p class="text-sm">正在加载数据...</p>
           </div>
           
           <div v-else-if="filteredResumes.length === 0" class="flex flex-col items-center justify-center p-16 text-slate-500">
             <Inbox class="w-12 h-12 mb-4 text-slate-300" />
             <p class="text-sm">没找到匹配的简历</p>
           </div>

           <ul v-else class="divide-y divide-slate-100">
             <li 
               v-for="resume in filteredResumes" 
               :key="resume.id"
               @click="goToResume(resume.id)"
               class="bg-white hover:bg-slate-50 transition-colors cursor-pointer group"
             >
                <div class="px-6 py-4 flex items-center justify-between gap-4">
                  
                  <div class="flex items-center gap-4 min-w-0">
                     <div class="w-10 h-10 rounded-lg bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-600 shrink-0">
                         <FileText class="w-5 h-5" />
                     </div>
                     <div class="truncate">
                        <h3 class="text-sm font-semibold text-slate-900 group-hover:text-indigo-600 transition-colors truncate">
                        {{ extractResumeDisplayName(resume) }}
                        </h3>
                        <div class="mt-1 flex items-center text-xs text-slate-500 gap-3 truncate">
                        <span class="truncate">{{ extractResumeFileName(resume) }}</span>
                           <span class="w-1 h-1 rounded-full bg-slate-300 shrink-0"></span>
                           <div class="flex items-center gap-1 shrink-0">
                             <Calendar class="w-3.5 h-3.5" />
                             <span>{{ resume.createdAt ? new Date(resume.createdAt).toLocaleDateString() : '未知日期' }}</span>
                           </div>
                        </div>
                     </div>
                  </div>

                  <div class="flex items-center gap-4 shrink-0">
                     <span 
                       class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border"
                       :class="getStatusBadge(resume.status)"
                     >
                       {{ getStatusText(resume.status) }}
                     </span>
                     <ChevronRight class="w-5 h-5 text-slate-400 group-hover:text-indigo-500 transition-colors" />
                  </div>
                  
                </div>
             </li>
           </ul>
        </div>
    </div>

    <!-- Simple Upload Modal Dummy -->
    <div v-if="showUploadModal" class="fixed inset-0 z-50 bg-slate-900/50 flex flex-col items-center justify-center p-4">
        <div class="bg-white rounded-xl shadow-sm border border-slate-200 max-w-md w-full p-6">
           <div class="flex justify-between items-center mb-4">
              <h2 class="text-lg font-semibold text-slate-900">导入简历</h2>
              <button @click="showUploadModal = false" class="p-1 hover:bg-slate-100 rounded-md text-slate-500"><X class="w-5 h-5"/></button>
           </div>
           <div class="border border-dashed border-slate-200 rounded-lg p-8 flex flex-col items-center justify-center bg-slate-50 hover:bg-indigo-50 hover:border-indigo-300 transition-colors cursor-pointer">
              <Upload class="w-8 h-8 text-indigo-500 mb-3" />
              <p class="text-sm font-medium text-slate-900 text-center">点击或拖拽 PDF 文件到此处上传</p>
              <p class="text-xs text-slate-500 mt-1">仅支持 .pdf 格式，最大 15MB</p>
           </div>
        </div>
    </div>
  </div>
</template>
