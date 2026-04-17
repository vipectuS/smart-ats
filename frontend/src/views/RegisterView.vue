<template>
  <div class="h-screen w-screen bg-gray-50 flex justify-center items-center relative overflow-hidden">
    <canvas ref="canvasRef" class="absolute inset-0 w-full h-full pointer-events-none"></canvas>

    <div class="bg-white/80 backdrop-blur-lg p-10 rounded-2xl shadow-xl border border-gray-200 w-[400px] z-10 transform transition-all hover:-translate-y-1">
      <div class="mb-8 text-center">
        <h2 class="text-3xl font-extrabold text-gray-800 tracking-tight">创建账号</h2>
        <p class="text-gray-500 mt-2 text-sm">加入智能招聘系统，开始你的招聘或求职流程。</p>
      </div>

      <div class="flex p-1 bg-gray-100/60 rounded-lg mb-6 border border-gray-200">
        <button
          @click="role = 'CANDIDATE'"
          :class="role === 'CANDIDATE' ? 'bg-white shadow-sm text-blue-600 font-semibold' : 'text-gray-500 hover:text-gray-700'"
          class="flex-1 py-2.5 rounded-md text-sm transition-all"
        >
          我是求职者
        </button>
        <button
          @click="role = 'HR'"
          :class="role === 'HR' ? 'bg-white shadow-sm text-blue-600 font-semibold' : 'text-gray-500 hover:text-gray-700'"
          class="flex-1 py-2.5 rounded-md text-sm transition-all"
        >
          我是招聘方
        </button>
      </div>

      <form @submit.prevent="handleRegister" class="space-y-6">
        <div>
          <label class="block text-gray-700 text-sm font-semibold mb-2" for="username">
            用户名
          </label>
          <input
            id="username"
            v-model="form.username"
            type="text"
            required
            placeholder="请输入用户名"
            class="w-full px-4 py-3 bg-gray-100/50 border border-gray-300 rounded-lg text-gray-800 placeholder-gray-400 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 transition-all font-medium"
          >
        </div>

        <div>
          <label class="block text-gray-700 text-sm font-semibold mb-2" for="email">
            邮箱
          </label>
          <input
            id="email"
            v-model="form.email"
            type="email"
            required
            placeholder="your@email.com"
            class="w-full px-4 py-3 bg-gray-100/50 border border-gray-300 rounded-lg text-gray-800 placeholder-gray-400 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 transition-all font-medium"
          >
        </div>

        <div>
          <label class="block text-gray-700 text-sm font-semibold mb-2" for="password">
            密码
          </label>
          <input
            id="password"
            v-model="form.password"
            type="password"
            required
            placeholder="******"
            class="w-full px-4 py-3 bg-gray-100/50 border border-gray-300 rounded-lg text-gray-800 placeholder-gray-400 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 transition-all font-medium"
          >
        </div>

        <div v-if="submitError" class="text-red-600 text-sm font-medium text-center bg-red-50 py-3 rounded-lg border border-red-100">
          {{ submitError }}
        </div>

        <button
          type="submit"
          :disabled="loading"
          class="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-3 px-4 rounded-lg shadow-md hover:shadow-lg focus:outline-none focus:ring-4 focus:ring-blue-500/30 transition-all disabled:opacity-70 flex justify-center items-center"
        >
          <svg v-if="loading" class="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          {{ loading ? '注册中...' : '注册账号' }}
        </button>
      </form>

      <div class="mt-8 space-y-4 pt-6 border-t border-gray-100 text-center">
        <p class="text-sm text-gray-500">
          已有账号？
          <router-link to="/login" class="font-semibold text-blue-600 transition hover:text-blue-700 hover:underline">
            立即登录
          </router-link>
        </p>
        <p class="text-xs text-gray-400 text-balance">
          注册后会根据你的角色进入对应工作流，后续可继续完善资料与简历信息
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const role = ref<'CANDIDATE' | 'HR'>('CANDIDATE')
const loading = ref(false)
const submitError = ref('')
const form = ref({
  username: '',
  email: '',
  password: ''
})
const canvasRef = ref<HTMLCanvasElement | null>(null)

const handleRegister = async () => {
  try {
    loading.value = true
    submitError.value = ''
    await authStore.register({
      ...form.value,
      role: role.value,
    })
    router.push({ name: 'login', query: { registered: '1' } })
  } catch (error: any) {
    submitError.value = authStore.error || error.message || '注册失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

let animationFrameId: number

onMounted(() => {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  let width = canvas.width = window.innerWidth
  let height = canvas.height = window.innerHeight

  const particles: Array<{ x: number; y: number; vx: number; vy: number; radius: number }> = []
  const particleCount = Math.floor((width * height) / 15000)

  for (let i = 0; i < particleCount; i += 1) {
    particles.push({
      x: Math.random() * width,
      y: Math.random() * height,
      vx: (Math.random() - 0.5) * 0.5,
      vy: (Math.random() - 0.5) * 0.5,
      radius: Math.random() * 2 + 1,
    })
  }

  const draw = () => {
    ctx.clearRect(0, 0, width, height)
    ctx.fillStyle = 'rgba(59, 130, 246, 0.4)'
    ctx.strokeStyle = 'rgba(59, 130, 246, 0.15)'
    ctx.lineWidth = 1

    for (let i = 0; i < particles.length; i += 1) {
      const particle = particles[i]
      particle.x += particle.vx
      particle.y += particle.vy

      if (particle.x < 0 || particle.x > width) particle.vx *= -1
      if (particle.y < 0 || particle.y > height) particle.vy *= -1

      ctx.beginPath()
      ctx.arc(particle.x, particle.y, particle.radius, 0, Math.PI * 2)
      ctx.fill()

      for (let j = i + 1; j < particles.length; j += 1) {
        const other = particles[j]
        const dx = particle.x - other.x
        const dy = particle.y - other.y
        const distance = Math.sqrt(dx * dx + dy * dy)

        if (distance < 150) {
          ctx.beginPath()
          ctx.strokeStyle = `rgba(59, 130, 246, ${0.2 * (1 - distance / 150)})`
          ctx.moveTo(particle.x, particle.y)
          ctx.lineTo(other.x, other.y)
          ctx.stroke()
        }
      }
    }

    animationFrameId = requestAnimationFrame(draw)
  }

  const handleResize = () => {
    width = canvas.width = window.innerWidth
    height = canvas.height = window.innerHeight
  }

  draw()
  window.addEventListener('resize', handleResize)

  onUnmounted(() => {
    cancelAnimationFrame(animationFrameId)
    window.removeEventListener('resize', handleResize)
  })
})
</script>
