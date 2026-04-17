<template>
  <div class="h-full w-full p-8 bg-gray-50 flex flex-col space-y-6 overflow-hidden">
    <!-- Header Controls -->
     <div class="flex flex-col md:flex-row justify-between items-start md:items-center bg-white p-6 rounded-2xl shadow-sm border border-gray-100 gap-4">
      <div>
        <h1 class="text-2xl font-extrabold text-gray-800">候选人简历库</h1>
        <p class="text-sm text-gray-500 mt-1">管理和追踪所有应聘者的简历状态</p>
      </div>

       <div class="flex items-center space-x-3 w-full md:w-auto">
        <div class="relative flex-1 md:w-64">
           <svg class="w-5 h-5 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
          <input v-model.trim="searchQuery" type="text" placeholder="搜索姓名/联系方式/文件名..." class="pl-10 pr-4 py-2 w-full bg-gray-50 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all">
        </div>

        <button @click="showUploadModal = true" class="px-5 py-2 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-lg shadow-md transition-all flex items-center shrink-0">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
          </svg>
          导入简历
        </button>
      </div>
    </div>

    <div v-if="errorMsg || feedbackMsg" :class="errorMsg ? 'border-rose-200 bg-rose-50 text-rose-700' : feedbackType === 'success' ? 'border-emerald-200 bg-emerald-50 text-emerald-700' : 'border-amber-200 bg-amber-50 text-amber-700'" class="rounded-2xl border px-5 py-4 text-sm shadow-sm flex items-center justify-between gap-4">
      <div>
        <p class="font-semibold">{{ errorMsg ? '简历列表操作提示' : feedbackType === 'success' ? '操作成功' : '操作提示' }}</p>
        <p class="mt-1">{{ errorMsg || feedbackMsg }}</p>
      </div>
      <button v-if="errorMsg" @click="fetchResumes()" class="shrink-0 rounded-lg bg-rose-600 px-4 py-2 text-white transition hover:bg-rose-700">重试</button>
    </div>

    <!-- Main List Container -->
    <div class="flex-1 bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden flex flex-col">
       
        <!-- Filters -->
        <div class="flex border-b border-gray-100 px-6 py-3 space-x-6 text-sm font-medium">
             <button @click="activeFilter = 'ALL'" :class="activeFilter === 'ALL' ? 'text-blue-600 border-b-2 border-blue-600' : 'text-gray-500 hover:text-gray-800'" class="pb-3 -mb-3 px-1 transition-colors">全部简历 ({{ totalElements }})</button>
             <button @click="activeFilter = 'PENDING_PARSE'" :class="activeFilter === 'PENDING_PARSE' ? 'text-blue-600 border-b-2 border-blue-600' : 'text-gray-500 hover:text-gray-800'" class="pb-3 -mb-3 px-1 transition-colors">待解析 ({{ statusCounts.PENDING_PARSE }})</button>
             <button @click="activeFilter = 'PARSED'" :class="activeFilter === 'PARSED' ? 'text-blue-600 border-b-2 border-blue-600' : 'text-gray-500 hover:text-gray-800'" class="pb-3 -mb-3 px-1 transition-colors">解析成功 ({{ statusCounts.PARSED }})</button>
             <button @click="activeFilter = 'PARSE_FAILED'" :class="activeFilter === 'PARSE_FAILED' ? 'text-rose-600 border-b-2 border-rose-600' : 'text-red-500 hover:text-red-600'" class="pb-3 -mb-3 px-1 transition-colors">解析异常 ({{ statusCounts.PARSE_FAILED }})</button>
        </div>

        <!-- Table -->
        <div class="flex-1 overflow-auto">
            <table class="w-full text-left border-collapse">
            <thead class="bg-gray-50/50 sticky top-0 backdrop-blur-sm z-10">
                <tr>
                <th class="p-4 text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap border-b border-gray-100">文件名 / 候选人</th>
                <th class="p-4 text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap border-b border-gray-100">联系方式 / 资料</th>
                <th class="p-4 text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap border-b border-gray-100">系统状态</th>
                <th class="p-4 text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap border-b border-gray-100">入库时间</th>
                <th class="p-4 text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap border-b border-gray-100 text-right">操作</th>
                </tr>
            </thead>
            <tbody class="divide-y divide-gray-50">
                <tr v-if="loading" class="animate-pulse">
                     <td colspan="5" class="p-8 text-center text-gray-400">正在加载数据...</td>
                </tr>
                 <tr v-else-if="filteredResumes.length === 0">
                     <td colspan="5" class="p-12 text-center text-gray-500">
                          <div class="w-16 h-16 bg-gray-50 rounded-full flex items-center justify-center mx-auto mb-3">
                              <svg class="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>
                          </div>
                     <p>当前筛选条件下暂无简历记录</p>
                     </td>
                </tr>
                 <tr v-for="resume in filteredResumes" :key="resume.id" class="hover:bg-blue-50/30 transition-colors group cursor-pointer">
                <td class="p-4">
                    <div class="flex items-center">
                    <div class="h-10 w-10 shrink-0 bg-gradient-to-br from-indigo-100 to-blue-100 rounded-xl flex items-center justify-center text-blue-600 mr-3">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>
                    </div>
                    <div class="overflow-hidden">
                        <div class="text-sm font-semibold text-gray-800 truncate max-w-[200px]">{{ resume.rawContentReference }}</div>
                      <div class="text-xs text-gray-500 mt-0.5" v-if="getResumeDisplayName(resume)">{{ getResumeDisplayName(resume) }}</div>
                      <div class="text-xs text-gray-400 mt-0.5" v-else>未识别候选人姓名</div>
                    </div>
                    </div>
                </td>
                <td class="p-4">
                    <div class="text-sm text-gray-700">{{ resume.contactInfo || '未提供联系方式' }}</div>
                    <div class="text-xs text-gray-400 mt-1">{{ resume.candidateName || '候选人资料待完善' }}</div>
                </td>
                <td class="p-4">
                    <span 
                       class="px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-yellow-100 text-yellow-800" 
                       v-if="resume.status === 'PENDING_PARSE'">
                      等待解析...
                    </span>
                    <span 
                       class="px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-blue-100 text-blue-800" 
                       v-else-if="resume.status === 'PARSING'">
                      <svg class="animate-spin -ml-1 mr-1 h-3 w-3 text-blue-800 inline-block" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
                      AI解析中
                    </span>
                     <span 
                       class="px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-emerald-100 text-emerald-800" 
                       v-else-if="resume.status === 'PARSED'">
                      解析完成
                    </span>
                    <div v-else-if="resume.status === 'PARSE_FAILED'" class="flex flex-col items-start gap-1">
                      <span 
                         class="px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-rose-100 text-rose-800" 
                         :title="resume.parseFailureReason || '解析异常'">
                        解析异常
                      </span>
                      <span v-if="resume.parseFailureReason" class="text-[10px] text-rose-600 truncate max-w-[120px]" :title="resume.parseFailureReason">
                        {{ resume.parseFailureReason }}
                      </span>
                    </div>
                </td>
                <td class="p-4 text-sm text-gray-500">
                    {{ new Date(resume.createdAt).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute:'2-digit' }) }}
                </td>
                <td class="p-4 text-right">
                    <button @click="openResumeDetail(resume.id)" class="text-blue-600 hover:text-blue-900 font-medium text-sm px-2 py-1 rounded hover:bg-blue-50 transition-colors">
                      查看详情
                    </button>
                    <button 
                      v-if="resume.status === 'PENDING_PARSE'" 
                      @click="triggerParse(resume.id)"
                      class="ml-2 text-indigo-600 hover:text-indigo-900 font-medium text-sm px-2 py-1 rounded hover:bg-indigo-50 transition-colors"
                    >
                      投递解析
                    </button>
                </td>
                </tr>
            </tbody>
            </table>
        </div>
        
        <!-- Pagination Placeholder -->
        <div class="border-t border-gray-100 p-4 flex items-center justify-between text-sm text-gray-500 bg-gray-50/50">
             <span>第 <strong>{{ currentPage + 1 }}</strong> / {{ totalPagesDisplay }} 页，共 <strong>{{ totalElements }}</strong> 条记录</span>
             <div class="flex space-x-1">
               <button @click="goToPreviousPage" :disabled="currentPage === 0 || loading" class="px-3 py-1 border border-gray-200 rounded bg-white hover:bg-gray-50 disabled:text-gray-400 disabled:cursor-not-allowed">上一页</button>
               <button class="px-3 py-1 border border-gray-200 rounded bg-blue-50 text-blue-600 font-medium">{{ currentPage + 1 }}</button>
               <button @click="goToNextPage" :disabled="isLastPage || loading" class="px-3 py-1 border border-gray-200 rounded bg-white hover:bg-gray-50 disabled:text-gray-400 disabled:cursor-not-allowed">下一页</button>
             </div>
        </div>
    </div>


    <!-- Upload Modal Overlay (Simplified) -->
    <div v-if="showUploadModal" class="fixed inset-0 bg-gray-900/50 backdrop-blur-sm z-50 flex justify-center items-center p-4">
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-md overflow-hidden transform transition-all">
        <div class="p-6 border-b border-gray-100 flex justify-between items-center">
          <h2 class="text-lg font-bold text-gray-800">上传新简历</h2>
          <button @click="showUploadModal = false" class="text-gray-400 hover:text-gray-600 p-1 rounded hover:bg-gray-100">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
          </button>
        </div>
        <div class="p-6 space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">简历文件名/凭证</label>
            <input v-model="newResumeRef" type="text" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500">
          </div>
        </div>
        <div class="p-4 bg-gray-50 flex justify-end space-x-3">
          <button @click="showUploadModal = false" class="px-4 py-2 text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 font-medium">取消</button>
          <button @click="submitResume" :disabled="submitting" class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-medium disabled:opacity-50 inline-flex items-center">
             <svg v-if="submitting" class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
             上传并保存
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import api from '../utils/api';

const router = useRouter();

const resumes = ref<any[]>([]);
const loading = ref(true);
const errorMsg = ref('');
const feedbackMsg = ref('');
const feedbackType = ref<'success' | 'warning'>('success');
const totalElements = ref(0);
const totalPages = ref(0);
const currentPage = ref(0);
const pageSize = ref(10);
const searchQuery = ref('');
const activeFilter = ref<'ALL' | 'PENDING_PARSE' | 'PARSED' | 'PARSE_FAILED'>('ALL');

// Modal state
const showUploadModal = ref(false);
const submitting = ref(false);
const newResumeRef = ref('backend_dev_resume.pdf');

const statusCounts = computed(() => resumes.value.reduce((acc: Record<string, number>, resume: any) => {
  acc[resume.status] = (acc[resume.status] || 0) + 1;
  return acc;
}, { PENDING_PARSE: 0, PARSED: 0, PARSE_FAILED: 0, PARSING: 0 }));

const filteredResumes = computed(() => {
  const keyword = searchQuery.value.trim().toLowerCase();
  return resumes.value.filter((resume: any) => {
    const matchesFilter = activeFilter.value === 'ALL' ? true : resume.status === activeFilter.value;
    if (!matchesFilter) return false;
    if (!keyword) return true;

    const haystack = [
      resume.rawContentReference,
      resume.candidateName,
      resume.contactInfo,
      getResumeDisplayName(resume),
    ].filter(Boolean).join(' ').toLowerCase();

    return haystack.includes(keyword);
  });
});

const isLastPage = computed(() => totalPages.value === 0 || currentPage.value >= totalPages.value - 1);
const totalPagesDisplay = computed(() => Math.max(totalPages.value, 1));

let pollingInterval: ReturnType<typeof setInterval> | null = null;

const getResumeDisplayName = (resume: any) => {
  return resume.candidateName
    || resume.parsedData?.basicInfo?.fullName
    || resume.parsedData?.candidateProfile?.name
    || '';
};

const openResumeDetail = (resumeId: string) => {
  router.push({ name: 'resumeDetail', params: { id: resumeId } });
};

const startPolling = () => {
  if (pollingInterval) return;
  pollingInterval = setInterval(() => {
    fetchResumes(true);
  }, 3000);
};

const stopPolling = () => {
  if (pollingInterval) {
    clearInterval(pollingInterval);
    pollingInterval = null;
  }
};

const checkAndHandlePolling = () => {
  const needsPolling = resumes.value.some(
    (r: any) => r.status === 'PENDING_PARSE' || r.status === 'PARSING'
  );
  if (needsPolling) {
    startPolling();
  } else {
    stopPolling();
  }
};

const fetchResumes = async (silent = false) => {
  if (!silent) loading.value = true;
  if (!silent) errorMsg.value = '';
  try {
    const response: any = await api.get('/resumes', {
      params: {
        page: currentPage.value,
        size: pageSize.value,
      },
    });
    resumes.value = response.data?.content || [];
    totalElements.value = response.data?.totalElements || 0;
    totalPages.value = response.data?.totalPages || 0;
    checkAndHandlePolling();
  } catch (e) {
    console.error("Failed to fetch resumes", e);
    if (!silent) {
      errorMsg.value = '无法读取简历列表，请检查登录状态或稍后重试。';
      resumes.value = [];
      totalElements.value = 0;
      totalPages.value = 0;
      stopPolling();
    }
  } finally {
    if (!silent) loading.value = false;
  }
};

const submitResume = async () => {
  if (!newResumeRef.value.trim()) {
    feedbackType.value = 'warning';
    feedbackMsg.value = '请先填写简历文件名或存储引用。';
    return;
  }

  submitting.value = true;
  try {
    errorMsg.value = '';
    feedbackMsg.value = '';
    const payload = {
      rawContentReference: newResumeRef.value
    };

    await api.post('/resumes', payload);
    showUploadModal.value = false;
    feedbackType.value = 'success';
    feedbackMsg.value = '简历已成功入库，可继续触发解析任务。';
    await fetchResumes();
  } catch (e) {
    console.error('Failed to submit resume', e);
    feedbackType.value = 'warning';
    feedbackMsg.value = '简历上传失败，请检查输入内容或稍后重试。';
  } finally {
    submitting.value = false;
  }
};

const triggerParse = async (resumeId: string) => {
   try {
    errorMsg.value = '';
    feedbackMsg.value = '';
    await api.post(`/resumes/${resumeId}/parse`);
    feedbackType.value = 'success';
    feedbackMsg.value = '解析任务已提交，列表会自动刷新状态。';
    await fetchResumes();
   } catch(e) {
     console.error(e);
     feedbackType.value = 'warning';
     feedbackMsg.value = '解析任务提交失败，请稍后重试。';
   }
}

const goToPreviousPage = async () => {
  if (currentPage.value === 0) return;
  currentPage.value -= 1;
  await fetchResumes();
}

const goToNextPage = async () => {
  if (isLastPage.value) return;
  currentPage.value += 1;
  await fetchResumes();
}

onMounted(() => {
  fetchResumes();
});

onUnmounted(() => {
  stopPolling();
});
</script>
