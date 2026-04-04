<template>
  <div class="h-screen w-screen bg-gray-50 flex justify-center items-center relative overflow-hidden">
    <!-- Particle Network Background -->
    <canvas ref="canvasRef" class="absolute inset-0 w-full h-full pointer-events-none"></canvas>
    
    <div class="bg-white/80 backdrop-blur-lg p-10 rounded-2xl shadow-xl border border-gray-200 w-[400px] z-10 transform transition-all hover:-translate-y-1">
      <div class="mb-8 text-center">
        <h2 class="text-3xl font-extrabold text-gray-800 tracking-tight">智能招聘系统</h2>
        <p class="text-gray-500 mt-2 text-sm">欢迎回来，请登录。</p>
      </div>
      
      <form @submit.prevent="handleLogin" class="space-y-6">
        <div>
          <label class="block text-gray-700 text-sm font-semibold mb-2" for="username">
            用户名
          </label>
          <input 
            v-model="username" 
            class="w-full px-4 py-3 bg-gray-100/50 border border-gray-300 rounded-lg text-gray-800 placeholder-gray-400 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 transition-all font-medium"
            id="username" 
            type="text" 
            placeholder="admin"
            required
          >
        </div>
        
        <div>
          <label class="block text-gray-700 text-sm font-semibold mb-2" for="password">
            密码
          </label>
          <input 
            v-model="password" 
            class="w-full px-4 py-3 bg-gray-100/50 border border-gray-300 rounded-lg text-gray-800 placeholder-gray-400 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 transition-all font-medium"
            id="password" 
            type="password" 
            placeholder="******"
            required
          >
        </div>

        <div v-if="authStore.error" class="text-red-600 text-sm font-medium text-center bg-red-50 py-3 rounded-lg border border-red-100">
          {{ authStore.error }}
        </div>

        <button 
          type="submit" 
          :disabled="authStore.loading"
          class="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-3 px-4 rounded-lg shadow-md hover:shadow-lg focus:outline-none focus:ring-4 focus:ring-blue-500/30 transition-all disabled:opacity-70 flex justify-center items-center"
        >
          <svg v-if="authStore.loading" class="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          {{ authStore.loading ? '认证中...' : '登录' }}
        </button>
      </form>
      
      <div class="mt-8 pt-6 border-t border-gray-100 text-center text-gray-400 text-xs text-balance">
        默认模拟管理员: 用户名 = admin | 密码 = admin
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { useAuthStore } from '../stores/auth';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const router = useRouter();

const username = ref('admin');
const password = ref('admin'); 
const canvasRef = ref<HTMLCanvasElement | null>(null);

const handleLogin = async () => {
  const success = await authStore.login({
    username: username.value,
    password: password.value
  });
  
  if (success) {
    router.push({ name: 'dashboard' });
  }
};

// Canvas High-Tech Particle Animation (Light Theme)
let animationFrameId: number;

onMounted(() => {
  const canvas = canvasRef.value;
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  if (!ctx) return;

  let width = canvas.width = window.innerWidth;
  let height = canvas.height = window.innerHeight;

  const particles: any[] = [];
  const particleCount = Math.floor((width * height) / 15000); 

  for (let i = 0; i < particleCount; i++) {
    particles.push({
      x: Math.random() * width,
      y: Math.random() * height,
      vx: (Math.random() - 0.5) * 0.5,
      vy: (Math.random() - 0.5) * 0.5,
      radius: Math.random() * 2 + 1
    });
  }

  const draw = () => {
    ctx.clearRect(0, 0, width, height);
    
    ctx.fillStyle = 'rgba(59, 130, 246, 0.4)';
    ctx.strokeStyle = 'rgba(59, 130, 246, 0.15)';
    ctx.lineWidth = 1;

    for (let i = 0; i < particles.length; i++) {
      const p = particles[i];
      p.x += p.vx;
      p.y += p.vy;

      if (p.x < 0 || p.x > width) p.vx *= -1;
      if (p.y < 0 || p.y > height) p.vy *= -1;

      ctx.beginPath();
      ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
      ctx.fill();

      for (let j = i + 1; j < particles.length; j++) {
        const p2 = particles[j];
        const dx = p.x - p2.x;
        const dy = p.y - p2.y;
        const dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < 150) {
          ctx.beginPath();
          ctx.strokeStyle = `rgba(59, 130, 246, ${0.2 * (1 - dist / 150)})`;
          ctx.moveTo(p.x, p.y);
          ctx.lineTo(p2.x, p2.y);
          ctx.stroke();
        }
      }
    }
    animationFrameId = requestAnimationFrame(draw);
  };

  draw();

  const handleResize = () => {
    width = canvas.width = window.innerWidth;
    height = canvas.height = window.innerHeight;
  };
  window.addEventListener('resize', handleResize);

  onUnmounted(() => {
    cancelAnimationFrame(animationFrameId);
    window.removeEventListener('resize', handleResize);
  });
});
</script>
