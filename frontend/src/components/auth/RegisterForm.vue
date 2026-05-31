<template>
  <form @submit.prevent="handleSubmit" class="space-y-5">
    <!-- 统一报错 / Unified Error block -->
    <div v-if="localError || authStore.error" class="bg-rose-50 text-rose-700 p-4 rounded-xl border border-rose-200 text-sm flex items-start gap-3">
      <AlertCircle class="w-5 h-5 flex-shrink-0 mt-0.5" />
      <span class="leading-relaxed">{{ localError || authStore.error }}</span>
    </div>

    <!-- 成功提示 / Success Message -->
    <div v-if="successMsg" class="bg-emerald-50 text-emerald-700 p-4 rounded-xl border border-emerald-200 text-sm flex items-start gap-3">
      <CheckCircle2 class="w-5 h-5 flex-shrink-0 mt-0.5" />
      <span class="leading-relaxed">{{ successMsg }}</span>
    </div>

    <!-- Role Selection -->
    <div>
      <label class="block text-sm font-medium text-slate-700 mb-2">我是 / I am a</label>
      <div class="grid grid-cols-2 gap-3">
        <label
          class="flex flex-col items-center justify-center p-3 border-2 rounded-xl cursor-pointer transition-all duration-200"
          :class="registerForm.role === 'CANDIDATE' ? 'border-blue-600 bg-blue-50 text-blue-700' : 'border-slate-200 hover:border-blue-200 bg-white'"
        >
          <input type="radio" v-model="registerForm.role" value="CANDIDATE" class="hidden" />
          <User class="w-6 h-6 mb-1.5" />
          <span class="text-sm font-medium">求职者 Candidate</span>
        </label>
        
        <label
          class="flex flex-col items-center justify-center p-3 border-2 rounded-xl cursor-pointer transition-all duration-200"
          :class="registerForm.role === 'HR' ? 'border-blue-600 bg-blue-50 text-blue-700' : 'border-slate-200 hover:border-blue-200 bg-white'"
        >
          <input type="radio" v-model="registerForm.role" value="HR" class="hidden" />
          <Building2 class="w-6 h-6 mb-1.5" />
          <span class="text-sm font-medium">招聘方 HR</span>
        </label>
      </div>
    </div>

    <div class="space-y-4">
      <div>
        <label class="block text-sm font-medium text-slate-700 mb-1.5">用户名 / Username</label>
        <div class="relative">
          <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <User class="h-5 w-5 text-slate-400" />
          </div>
          <input
            v-model="registerForm.username"
            type="text"
            required
            class="block w-full pl-10 pr-3 py-2.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm bg-slate-50 focus:bg-white transition-colors"
            placeholder="设置用户名"
            :disabled="isRegistering"
          />
        </div>
      </div>

      <div>
        <label class="block text-sm font-medium text-slate-700 mb-1.5">邮箱 / Email</label>
        <div class="relative">
          <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <Mail class="h-5 w-5 text-slate-400" />
          </div>
          <input
            v-model="registerForm.email"
            type="email"
            required
            class="block w-full pl-10 pr-3 py-2.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm bg-slate-50 focus:bg-white transition-colors"
            placeholder="your@email.com"
            :disabled="isRegistering"
          />
        </div>
      </div>

      <div>
        <label class="block text-sm font-medium text-slate-700 mb-1.5">密码 / Password</label>
        <div class="relative">
          <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <Lock class="h-5 w-5 text-slate-400" />
          </div>
          <input
            v-model="registerForm.password"
            :type="showPassword ? 'text' : 'password'"
            required
            minlength="6"
            class="block w-full pl-10 pr-10 py-2.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm bg-slate-50 focus:bg-white transition-colors"
            placeholder="至少 6 位字符"
            :disabled="isRegistering"
          />
          <button
            type="button"
            @click="showPassword = !showPassword"
            class="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600"
          >
            <Eye v-if="!showPassword" class="h-5 w-5" />
            <EyeOff v-else class="h-5 w-5" />
          </button>
        </div>
      </div>

      <div v-if="registerForm.role === 'HR'" class="p-4 bg-slate-50 rounded-xl border border-slate-200 space-y-4">
        <div class="flex items-center gap-2 mb-2 text-slate-700">
          <ShieldCheck class="w-5 h-5 text-blue-600" />
          <h4 class="text-sm font-semibold">企业认证 / Enterprise Verification</h4>
        </div>
        
        <div>
          <label class="block text-xs font-medium text-slate-500 mb-1">选择企业</label>
          <select 
            v-model="registerForm.organizationId"
            required
            class="block w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm bg-white"
            :disabled="isRegistering || isLoadingOrgs"
          >
            <option value="" disabled>请选择所属企业</option>
            <option v-for="org in organizations" :key="org.id" :value="org.id">
              {{ org.name }}
            </option>
          </select>
          <div v-if="isLoadingOrgs" class="mt-1 text-xs text-slate-500 flex items-center">
            <Loader2 class="w-3 h-3 animate-spin mr-1" /> 加载企业列表中...
          </div>
        </div>
        
        <div>
          <label class="block text-xs font-medium text-slate-500 mb-1">注册邀请码</label>
          <input
            v-model="registerForm.organizationToken"
            type="text"
            required
            class="block w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm bg-white uppercase"
            placeholder="请输入企业邀请码"
            :disabled="isRegistering"
          />
        </div>
      </div>
    </div>

    <button
      type="submit"
      :disabled="isRegistering || !isFormValid"
      class="w-full flex justify-center py-2.5 px-4 border border-transparent rounded-xl shadow-sm text-sm font-medium text-white bg-slate-800 hover:bg-slate-900 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-slate-900 transition-colors disabled:opacity-70 disabled:cursor-not-allowed mt-6"
    >
      <Loader2 v-if="isRegistering" class="w-5 h-5 animate-spin mr-2" />
      {{ isRegistering ? '正在注册...' : '注 册 账 号' }}
    </button>
  </form>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { useAuthStore, type UserRole, type OrganizationRef } from '@/stores/auth';
import { AlertCircle, CheckCircle2, User, Building2, Mail, Lock, Eye, EyeOff, ShieldCheck, Loader2 } from 'lucide-vue-next';
import api from '@/utils/api';
import { resolveApiError } from '@/utils/apiError';

const emit = defineEmits<{
  (e: 'success'): void
}>();

const authStore = useAuthStore();
const showPassword = ref(false);
const isRegistering = ref(false);
const localError = ref('');
const successMsg = ref('');

const organizations = ref<OrganizationRef[]>([]);
const isLoadingOrgs = ref(false);

const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  role: 'CANDIDATE' as UserRole,
  organizationId: '',
  organizationToken: ''
});

const isFormValid = computed(() => {
  const baseValid = registerForm.username.trim().length > 0 &&
                    registerForm.email.includes('@') &&
                    registerForm.password.length >= 6;
  if (!baseValid) return false;
  
  if (registerForm.role === 'HR') {
    return !!registerForm.organizationId && registerForm.organizationToken.trim().length > 0;
  }
  return true;
});

const loadOrganizations = async () => {
  try {
    isLoadingOrgs.value = true;
    localError.value = '';
    const res: any = await api.get('/v1/organizations');
    // Assuming backend returns Page or List, adjust based on your API structure.
    organizations.value = Array.isArray(res.data) ? res.data : (res.data.content || []);
  } catch (err: any) {
    localError.value = resolveApiError(err, '无法获取组织列表 / Failed to load organizations').summary;
  } finally {
    isLoadingOrgs.value = false;
  }
};

onMounted(() => {
  loadOrganizations();
});

const handleSubmit = async () => {
  localError.value = '';
  successMsg.value = '';
  
  try {
    isRegistering.value = true;
    await authStore.register({
      username: registerForm.username,
      email: registerForm.email,
      password: registerForm.password,
      role: registerForm.role,
      organizationId: registerForm.role === 'HR' ? registerForm.organizationId : null,
      organizationToken: registerForm.role === 'HR' ? registerForm.organizationToken : null,
    });
    
    successMsg.value = '注册成功，请登录！';
    setTimeout(() => {
      emit('success');
    }, 1500);
  } catch (err: any) {
    localError.value = resolveApiError(err, '注册失败').summary;
  } finally {
    isRegistering.value = false;
  }
};
</script>