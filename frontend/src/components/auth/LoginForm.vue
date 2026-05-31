<template>
  <form @submit.prevent="handleSubmit" class="space-y-6">
    <!-- 统一报错 / Unified Error block -->
    <div v-if="authStore.error" class="bg-rose-50 text-rose-700 p-4 rounded-xl border border-rose-200 text-sm flex items-start gap-3">
      <AlertCircle class="w-5 h-5 flex-shrink-0 mt-0.5" />
      <span class="leading-relaxed">{{ authStore.error }}</span>
    </div>

    <!-- Login Fields -->
    <div class="space-y-4">
      <div>
        <label class="block text-sm font-medium text-slate-700 mb-1.5">用户名 / Username</label>
        <div class="relative">
          <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <User class="h-5 w-5 text-slate-400" />
          </div>
          <input
            v-model="loginForm.username"
            type="text"
            required
            class="block w-full pl-10 pr-3 py-2.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors bg-slate-50 focus:bg-white text-sm"
            placeholder="请输入用户名"
            :disabled="authStore.loading"
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
            v-model="loginForm.password"
            :type="showPassword ? 'text' : 'password'"
            required
            class="block w-full pl-10 pr-10 py-2.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors bg-slate-50 focus:bg-white text-sm"
            placeholder="••••••••"
            :disabled="authStore.loading"
          />
          <button
            type="button"
            @click="showPassword = !showPassword"
            class="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600 transition-colors"
          >
            <Eye v-if="!showPassword" class="h-5 w-5" />
            <EyeOff v-else class="h-5 w-5" />
          </button>
        </div>
      </div>
    </div>

    <div class="flex items-center justify-between mt-4">
      <div class="flex items-center">
        <input
          id="remember-me"
          v-model="loginForm.rememberMe"
          type="checkbox"
          class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-slate-300 rounded cursor-pointer"
          :disabled="authStore.loading"
        />
        <label for="remember-me" class="ml-2 block text-sm text-slate-600 cursor-pointer">
          保持登录
        </label>
      </div>
      <div class="text-sm">
        <a href="#" class="font-medium text-blue-600 hover:text-blue-500 transition-colors">
          忘记密码?
        </a>
      </div>
    </div>

    <button
      type="submit"
      :disabled="authStore.loading || !isFormValid"
      class="w-full flex justify-center py-2.5 px-4 border border-transparent rounded-xl shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors disabled:opacity-70 disabled:cursor-not-allowed"
    >
      <Loader2 v-if="authStore.loading" class="w-5 h-5 animate-spin mr-2" />
      {{ authStore.loading ? '正在登录...' : '登录' }}
    </button>
  </form>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { AlertCircle, User, Lock, Eye, EyeOff, Loader2 } from 'lucide-vue-next';

const router = useRouter();
const authStore = useAuthStore();

const showPassword = ref(false);

const loginForm = reactive({
  username: '',
  password: '',
  rememberMe: false
});

const isFormValid = computed(() => {
  return loginForm.username.trim().length > 0 && loginForm.password.trim().length > 0;
});

const handleSubmit = async () => {
  try {
    const user = await authStore.login(loginForm);
    if (user) {
      router.push({ name: authStore.homeRouteName });
    }
  } catch (error) {
    // Auth store handles error internally
  }
};
</script>