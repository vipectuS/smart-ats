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
           <input type="text" placeholder="搜索姓名/技能..." class="pl-10 pr-4 py-2 w-full bg-gray-50 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all">
        </div>

        <button @click="showUploadModal = true" class="px-5 py-2 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-lg shadow-md transition-all flex items-center shrink-0">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
          </svg>
          导入简历
        </button>
      </div>
    </div>

    <!-- Main List Container -->
    <div class="flex-1 bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden flex flex-col">
       
        <!-- Filters -->
        <div class="flex border-b border-gray-100 px-6 py-3 space-x-6 text-sm font-medium">
             <button class="text-blue-600 border-b-2 border-blue-600 pb-3 -mb-3 px-1">全部简历</button>
             <button class="text-gray-500 hover:text-gray-800 pb-3 -mb-3 px-1 transition-colors">待解析 (4)</button>
             <button class="text-gray-500 hover:text-gray-800 pb-3 -mb-3 px-1 transition-colors">解析成功</button>
             <button class="text-red-500 hover:text-red-600 pb-3 -mb-3 px-1 transition-colors">解析异常 (1)</button>
        </div>

        <!-- Table -->
        <div class="flex-1 overflow-auto">
            <table class="w-full text-left border-collapse">
            <thead class="bg-gray-50/50 sticky top-0 backdrop-blur-sm z-10">
                <tr>
                <th class="p-4 text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap border-b border-gray-100">文件名 / 候选人</th>
                <th class="p-4 text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap border-b border-gray-100">应聘职位</th>
                <th class="p-4 text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap border-b border-gray-100">系统状态</th>
                <th class="p-4 text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap border-b border-gray-100">入库时间</th>
                <th class="p-4 text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap border-b border-gray-100 text-right">操作</th>
                </tr>
            </thead>
            <tbody class="divide-y divide-gray-50">
                <tr v-if="loading" class="animate-pulse">
                     <td colspan="5" class="p-8 text-center text-gray-400">正在加载数据...</td>
                </tr>
                <tr v-else-if="resumes.length === 0">
                     <td colspan="5" class="p-12 text-center text-gray-500">
                          <div class="w-16 h-16 bg-gray-50 rounded-full flex items-center justify-center mx-auto mb-3">
                              <svg class="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>
                          </div>
                          <p>暂无简历记录，请上传导入</p>
                     </td>
                </tr>
                <tr v-for="resume in resumes" :key="resume.id" class="hover:bg-blue-50/30 transition-colors group cursor-pointer">
                <td class="p-4">
                    <div class="flex items-center">
                    <div class="h-10 w-10 shrink-0 bg-gradient-to-br from-indigo-100 to-blue-100 rounded-xl flex items-center justify-center text-blue-600 mr-3">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>
                    </div>
                    <div class="overflow-hidden">
                        <div class="text-sm font-semibold text-gray-800 truncate max-w-[200px]">{{ resume.rawContentReference }}</div>
                        <div class="text-xs text-gray-500 mt-0.5" v-if="resume.parsedData?.name">{{ resume.parsedData.name }}</div>
                        <div class="text-xs text-gray-400 mt-0.5" v-else>未能识别</div>
                    </div>
                    </div>
                </td>
                <td class="p-4">
                    <span class="text-sm font-medium text-gray-700 bg-gray-100 px-3 py-1 rounded-full">
                       Job #{{ resume.jobId }}
                    </span>
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
                    <button class="text-blue-600 hover:text-blue-900 font-medium text-sm px-2 py-1 rounded hover:bg-blue-50 transition-colors">
                        查看画像
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
             <span>共 <strong>{{ resumes.length }}</strong> 条记录</span>
             <div class="flex space-x-1">
                  <button class="px-3 py-1 border border-gray-200 rounded bg-white text-gray-400 cursor-not-allowed">上一页</button>
                  <button class="px-3 py-1 border border-gray-200 rounded bg-blue-50 text-blue-600 font-medium">1</button>
                  <button class="px-3 py-1 border border-gray-200 rounded bg-white hover:bg-gray-50">下一页</button>
             </div>
        </div>
    </div>


    <!-- Upload Modal Overlay (Simplified) -->
    <div v-if="showUploadModal" class="fixed inset-0 bg-gray-900/50 backdrop-blur-sm z-50 flex justify-center items-center p-4">
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-md overflow-hidden transform transition-all">
        <div class="p-6 border-b border-gray-100 flex justify-between items-center">
          <h2 class="text-lg font-bold text-gray-800">上传新简历 (模拟)</h2>
          <button @click="showUploadModal = false" class="text-gray-400 hover:text-gray-600 p-1 rounded hover:bg-gray-100">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
          </button>
        </div>
        <div class="p-6 space-y-4">
           <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">关联的 Job ID</label>
            <input v-model.number="newResumeJobId" type="number" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500">
          </div>
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
import { ref, onMounted, onUnmounted } from 'vue';
import { useAuthStore } from '../stores/auth';

const authStore = useAuthStore();
const resumes = ref<any[]>([]);
const loading = ref(true);

// Modal state
const showUploadModal = ref(false);
const submitting = ref(false);
const newResumeJobId = ref(1);
const newResumeRef = ref('backend_dev_resume.pdf');

const API_BASE_URL = 'http://localhost:18080';

let pollingInterval: ReturnType<typeof setInterval> | null = null;

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
  try {
    const res = await fetch(`${API_BASE_URL}/api/resumes?page=0&size=20`, {
      headers: {
        'Authorization': `Bearer ${authStore.token}`
      }
    });

    if (res.ok) {
      const data = await res.json();
      resumes.value = data.content || [];
      checkAndHandlePolling();
    }
  } catch (e) {
    console.error("Failed to fetch resumes", e);
  } finally {
    if (!silent) loading.value = false;
  }
};

const submitResume = async () => {
  submitting.value = true;
  try {
    const payload = {
      jobId: newResumeJobId.value,
      rawContentReference: newResumeRef.value
    };

    const res = await fetch(`${API_BASE_URL}/api/resumes`, {
      method: 'POST',
      headers: {
         'Authorization': `Bearer ${authStore.token}`,
         'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    });

    if (res.ok) {
      // Refresh list
      showUploadModal.value = false;
      await fetchResumes();
    } else {
       alert("上传失败");
    }
  } finally {
    submitting.value = false;
  }
};

const triggerParse = async (resumeId: number) => {
   try {
     const res = await fetch(`${API_BASE_URL}/api/resumes/${resumeId}/parse`, {
       method: 'POST',
       headers: {
         'Authorization': `Bearer ${authStore.token}`
       }
     });

     if (res.ok || res.status === 202) {
        // Refresh local status
        await fetchResumes();
     } else {
        alert("投递解析任务失败");
     }
   } catch(e) {
     console.error(e);
   }
}

onMounted(() => {
  fetchResumes();
});

onUnmounted(() => {
  stopPolling();
});
</script>
