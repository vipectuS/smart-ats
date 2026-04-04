<template>
  <div class="min-h-screen flex items-center justify-center bg-slate-50 relative overflow-hidden">
    <!-- Background decorations -->
    <div class="absolute -top-40 -right-40 w-96 h-96 bg-blue-400 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob"></div>
    <div class="absolute -bottom-40 -left-40 w-96 h-96 bg-indigo-400 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob animation-delay-2000"></div>

    <div class="bg-white/80 backdrop-blur-xl p-8 rounded-3xl shadow-xl w-full max-w-md border border-white/50 z-10">
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-blue-600 to-indigo-600 mb-2">创建账号</h1>
        <p class="text-slate-500">加入智能招聘生态系统</p>
      </div>

      <!-- Role Selection -->
      <div class="flex p-1 bg-slate-100/50 rounded-xl mb-6 border border-slate-200/50">
        <button 
          @click="role = 'CANDIDATE'" 
          :class="role === 'CANDIDATE' ? 'bg-white shadow-sm text-blue-600 font-bold' : 'text-slate-500 hover:text-slate-700'"
          class="flex-1 py-2.5 rounded-lg text-sm transition-all duration-200"
        >
          <i class="fas fa-user-tie mr-1"></i> 我是求职者
        </button>
        <button 
          @click="role = 'HR'" 
          :class="role === 'HR' ? 'bg-white shadow-sm text-indigo-600 font-bold' : 'text-slate-500 hover:text-slate-700'"
          class="flex-1 py-2.5 rounded-lg text-sm transition-all duration-200"
        >
          <i class="fas fa-building mr-1"></i> 我是招聘方
        </button>
      </div>

      <form @submit.prevent="handleRegister" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1">用户名</label>
          <input v-model="form.username" type="text" required
                 class="w-full px-4 py-3 rounded-xl bg-slate-50/50 border border-slate-200 focus:border-blue-500 focus:ring-2 focus:ring-blue-200 outline-none transition" 
                 placeholder="请输入用户名" />
        </div>
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1">邮箱</label>
          <input v-model="form.email" type="email" required
                 class="w-full px-4 py-3 rounded-xl bg-slate-50/50 border border-slate-200 focus:border-blue-500 focus:ring-2 focus:ring-blue-200 outline-none transition" 
                 placeholder="your@email.com" />
        </div>
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1">密码</label>
          <input v-model="form.password" type="password" required
                 class="w-full px-4 py-3 rounded-xl bg-slate-50/50 border border-slate-200 focus:border-blue-500 focus:ring-2 focus:ring-blue-200 outline-none transition" 
                 placeholder="••••••••" />
        </div>

        <button type="submit" :disabled="loading"
                class="w-full py-3.5 bg-gradient-to-r from-blue-600 to-indigo-600 text-white rounded-xl font-bold shadow-lg hover:shadow-indigo-500/30 hover:-translate-y-0.5 transition-all duration-200 mt-6 disabled:opacity-70">
          <i :class="loading ? 'fas fa-spinner fa-spin' : 'fas fa-user-plus'"></i> 
          {{ loading ? '注册中...' : '注册账号' }}
        </button>
      </form>

      <div class="mt-6 text-center text-sm text-slate-500">
        已有账号？ 
        <router-link to="/login" class="text-blue-600 font-bold hover:underline">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
// import { useAuthStore } from '../stores/auth'

const router = useRouter()
// const authStore = useAuthStore()

const role = ref<'CANDIDATE' | 'HR'>('CANDIDATE')
const loading = ref(false)
const form = ref({
  username: '',
  email: '',
  password: ''
})

const handleRegister = async () => {
  try {
    loading.value = true
    // Simulate API registration call payload: { ...form.value, role: role.value }
    // await authStore.register(form.value.username, form.value.password, role.value)
    alert(`暂态模拟注册成功: ${role.value}。\n前端已就绪，等待后端 Milestone 6 接口开放。`)
    router.push('/login')
  } catch (error: any) {
    alert('注册失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}
</script>
