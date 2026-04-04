<script setup lang="ts">
import { useAuthStore } from '../stores/auth';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const router = useRouter();

const handleLogout = () => {
  authStore.logout();
  router.push('/login');
};
</script>

<template>
  <div class="flex h-screen bg-slate-50 font-sans text-slate-800">
    <!-- Sidebar -->
    <aside class="w-72 bg-white border-r border-slate-200 flex flex-col justify-between shadow-sm z-10">
      <div>
        <div class="h-20 flex items-center px-8 border-b border-slate-100">
           <div class="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center mr-3 shadow-md shadow-blue-500/30">
               <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path></svg>
           </div>
           <span class="text-xl font-black bg-clip-text text-transparent bg-gradient-to-r from-blue-700 to-indigo-600 tracking-tight">Smart ATS</span>
        </div>
        
        <nav class="p-4 space-y-1.5 mt-4">
          <div class="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-3 px-4">主菜单</div>
          
          <router-link
            to="/"
            class="flex items-center px-4 py-3 rounded-xl text-slate-600 hover:bg-slate-50 hover:text-blue-600 transition-all font-medium group"
            exact-active-class="bg-blue-50 text-blue-700 font-bold"
          >
            <svg class="w-5 h-5 mr-3 group-hover:scale-110 transition-transform" :class="{'text-blue-600': $route.path === '/', 'text-slate-400 group-hover:text-blue-500': $route.path !== '/'}" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"></path></svg>
            数据看板
          </router-link>

          <router-link
            to="/jobs"
            class="flex items-center px-4 py-3 rounded-xl text-slate-600 hover:bg-slate-50 hover:text-blue-600 transition-all font-medium group"
             exact-active-class="bg-blue-50 text-blue-700 font-bold"
          >
             <svg class="w-5 h-5 mr-3 group-hover:scale-110 transition-transform" :class="{'text-blue-600': $route.path === '/jobs', 'text-slate-400 group-hover:text-blue-500': $route.path !== '/jobs'}" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 13.255A23.931 23.931 0 0112 15c-3.183 0-6.22-.62-9-1.745M16 6V4a2 2 0 00-2-2h-4a2 2 0 00-2 2v2m4 6h.01M5 20h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"></path></svg>
            职位管理
          </router-link>
          
          <router-link
            to="/resumes"
             class="flex items-center px-4 py-3 rounded-xl text-slate-600 hover:bg-slate-50 hover:text-blue-600 transition-all font-medium group"
             exact-active-class="bg-blue-50 text-blue-700 font-bold"
          >
            <svg class="w-5 h-5 mr-3 group-hover:scale-110 transition-transform" :class="{'text-blue-600': $route.path === '/resumes', 'text-slate-400 group-hover:text-blue-500': $route.path !== '/resumes'}" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>
            简历库
            <span class="ml-auto bg-blue-100 text-blue-600 py-0.5 px-2 rounded-full text-xs font-bold">新</span>
          </router-link>
        </nav>
      </div>
      
      <!-- Footer items in sidebar -->
      <div class="p-6 border-t border-slate-100">
         <div class="flex items-center mb-6">
            <div class="relative">
                <img src="https://ui-avatars.com/api/?name=Admin+User&background=0D8ABC&color=fff&rounded=true&bold=true" alt="avatar" class="w-10 h-10 rounded-full border-2 border-white shadow-sm">
                <div class="absolute bottom-0 right-0 w-3 h-3 bg-green-500 border-2 border-white rounded-full"></div>
            </div>
            <div class="ml-3">
                <p class="text-sm font-bold text-slate-800 leading-tight">Admin User</p>
                <p class="text-xs text-slate-500 font-medium">HR 招聘专员</p>
            </div>
         </div>
        <button 
          @click="handleLogout"
          class="w-full flex items-center justify-center px-4 py-2.5 text-sm font-semibold text-rose-600 bg-rose-50 hover:bg-rose-100 rounded-xl transition-colors"
        >
          <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path></svg>
          退出系统
        </button>
      </div>
    </aside>

    <!-- Main Content -->
    <div class="flex-1 flex flex-col overflow-hidden relative">
      <!-- Decorator background shapes -->
       <div class="absolute top-0 right-0 -mr-20 -mt-20 w-96 h-96 rounded-full bg-blue-50/50 blur-3xl pointer-events-none z-0"></div>

      <!-- Top Header -->
      <header class="h-20 bg-white/70 backdrop-blur-md border-b border-slate-200/50 flex items-center justify-between px-8 z-10 sticky top-0">
        <h2 class="text-lg text-slate-700 font-medium">
             <span class="text-slate-400 font-normal mr-2">{{ new Date().toLocaleDateString('zh-CN', {month:'long', day:'numeric', weekday:'long'}) }}</span>
             👏 欢迎回来工作，今天也是高效的一天。
        </h2>
        <div class="flex items-center space-x-4">
             <button class="p-2 text-slate-400 hover:text-blue-600 bg-white rounded-full shadow-sm hover:shadow border border-slate-100 transition-all relative">
                 <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"></path></svg>
                 <span class="absolute top-1.5 right-2 w-2 h-2 bg-red-500 rounded-full border border-white"></span>
             </button>
          <button class="bg-blue-600 text-white px-5 py-2.5 rounded-xl shadow-md shadow-blue-500/20 hover:bg-blue-700 hover:shadow-lg hover:-translate-y-0.5 transition-all font-semibold flex items-center">
            <svg class="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path></svg>
            发布新职位
          </button>
        </div>
      </header>

      <!-- Scrollable Main View -->
      <main class="flex-1 overflow-y-auto z-10">
        <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
                <component :is="Component" />
            </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<style>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
