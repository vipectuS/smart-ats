<template>
  <div class="min-h-screen w-full bg-slate-50 flex items-center justify-center p-4 sm:p-8 relative overflow-hidden">
    
    <div class="relative z-10 w-full max-w-5xl bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden flex flex-col md:flex-row h-auto min-h-[580px] transition-all duration-300">
      
      <!-- Left Side Brand Area -->
      <div class="hidden md:flex md:w-1/2 bg-blue-700 relative flex-col justify-between p-12 text-white overflow-hidden">
        <div class="relative z-10">
          <h1 class="text-4xl font-extrabold tracking-tight mb-4 flex items-center gap-3">
            <span class="bg-white text-blue-700 p-2 rounded-lg">
              <Zap class="w-8 h-8" />
            </span>
            Smart ATS
          </h1>
          <p class="text-blue-100 text-lg leading-relaxed max-w-md mt-6">
            基于多模态大模型的企业级智能招聘辅助引擎<br/>
            高效精准锁定行业顶尖人才
          </p>
        </div>
        
        <div class="relative z-10 mt-auto border-t border-white/20 pt-6">
          <div class="flex items-center gap-4">
            <Shield class="w-10 h-10 text-blue-200 opacity-50" />
            <div>
              <h4 class="font-semibold text-blue-50">企业级安全防护</h4>
              <p class="text-sm text-blue-200 opacity-80">SSL级通讯加密与数据隔离</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Right Side Form Area -->
      <div class="w-full md:w-1/2 p-8 md:p-12 lg:p-16 flex flex-col justify-center relative bg-white">
        <!-- Logo for mobile only -->
        <div class="md:hidden flex items-center gap-2 mb-8">
          <span class="bg-blue-700 text-white p-1.5 rounded-lg">
            <Zap class="w-6 h-6" />
          </span>
          <span class="text-2xl font-bold text-slate-800">Smart ATS</span>
        </div>

        <div class="mb-8">
          <h2 class="text-3xl font-extrabold text-slate-900 tracking-tight">
            {{ isLogin ? '登录账户' : '创建账户' }}
          </h2>
          <p class="text-slate-500 mt-2 text-sm flex gap-2">
            {{ isLogin ? '没有账号?' : '已有账号?' }}
            <button 
              @click="toggleMode"
              class="text-blue-600 hover:text-blue-700 font-semibold transition-colors focus:outline-none"
            >
              {{ isLogin ? '立即注册' : '返回登录' }}
            </button>
          </p>
        </div>

        <!-- Form Components -->
        <div class="transition-all duration-300 ease-in-out">
          <LoginForm v-if="isLogin" />
          <RegisterForm v-else @success="handleRegisterSuccess" />
        </div>
        
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { Zap, Shield } from 'lucide-vue-next';
import LoginForm from '@/components/auth/LoginForm.vue';
import RegisterForm from '@/components/auth/RegisterForm.vue';

const isLogin = ref(true);

const toggleMode = () => {
  isLogin.value = !isLogin.value;
};

const handleRegisterSuccess = () => {
  isLogin.value = true;
};
</script>
