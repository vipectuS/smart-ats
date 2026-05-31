<script setup lang="ts">
import { ref, computed } from 'vue';
import { useAuthStore } from '../stores/auth';
import { useRoute, useRouter } from 'vue-router';
import {
  LogOut,
  Menu,
  X,
  LayoutDashboard,
  Briefcase,
  Users,
  Settings,
  ChevronLeft,
  ChevronRight,
  UserCircle,
  FileText
} from 'lucide-vue-next';

const authStore = useAuthStore();
const router = useRouter();
const route = useRoute();

const isCandidate = computed(() => authStore.user?.role === 'CANDIDATE');
const isAdmin = computed(() => authStore.user?.role === 'ADMIN');
const displayName = computed(() => authStore.user?.username || '未登录用户');
const roleLabel = computed(() => {
  switch (authStore.user?.role) {
    case 'CANDIDATE': return '候选人';
    case 'ADMIN': return '系统管理员';
    default: return 'HR 招聘专员';
  }
});

const avatarUrl = computed(() => `https://ui-avatars.com/api/?name=${encodeURIComponent(displayName.value)}&background=4f46e5&color=fff&rounded=true&bold=true`);

const isSidebarCollapsed = ref(false);
const isMobileMenuOpen = ref(false);

const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value;
};

const handleLogout = () => {
  authStore.logout();
  router.push('/login');
};

const navigation = computed(() => {
  if (isCandidate.value) {
    return [
      { name: '工作台', to: '/candidate/dashboard', icon: LayoutDashboard },
      { name: '我的档案', to: '/candidate/profile', icon: UserCircle },
      { name: '投递记录', to: '/candidate/applications', icon: Briefcase },
    ];
  }
  if (isAdmin.value) {
    return [
      { name: '工作台', to: '/', icon: LayoutDashboard },
      { name: '职位管理', to: '/jobs', icon: Briefcase },
      { name: '人才库', to: '/resumes', icon: Users },
      { name: '管理终端', to: '/admin/console', icon: Settings },
    ];
  }
  return [
    { name: '工作台', to: '/', icon: LayoutDashboard },
    { name: '职位管理', to: '/jobs', icon: Briefcase },
    { name: '全部简历', to: '/resumes', icon: FileText },
  ];
});

</script>

<template>
  <div class="h-screen w-full flex bg-slate-50 overflow-hidden text-slate-900">
    
    <!-- Mobile overlay -->
    <div 
      v-if="isMobileMenuOpen" 
      class="fixed inset-0 z-40 bg-slate-900/50 block md:hidden"
      @click="isMobileMenuOpen = false"
    ></div>

    <!-- Sidebar -->
    <aside 
      :class="[
        isSidebarCollapsed ? 'w-20' : 'w-64',
        isMobileMenuOpen ? 'translate-x-0' : '-translate-x-full',
        'fixed inset-y-0 left-0 z-50 flex flex-col bg-white border-r border-slate-200 transition-all duration-300 md:relative md:translate-x-0 overflow-y-auto'
      ]"
    >
      <!-- Logo Area -->
      <div class="h-16 flex items-center justify-between px-4 border-b border-slate-200 overflow-hidden whitespace-nowrap bg-white">
        <div class="flex items-center gap-3 w-full">
          <div class="w-8 h-8 rounded shrink-0 bg-indigo-600 flex items-center justify-center shadow-sm">
            <span class="text-white font-bold text-sm">S</span>
          </div>
          <span 
            class="font-semibold text-lg tracking-tight transition-opacity duration-300 truncate"
            :class="isSidebarCollapsed ? 'opacity-0 w-0' : 'opacity-100 flex-1'"
          >Smart ATS</span>
        </div>
        
        <!-- Mobile close -->
        <button class="md:hidden text-slate-500 hover:text-slate-800 shrink-0" @click="isMobileMenuOpen = false">
          <X class="w-5 h-5" />
        </button>
      </div>

      <!-- Nav Links -->
      <nav class="flex-1 overflow-y-auto p-3 space-y-1 bg-white">
        <router-link
          v-for="item in navigation"
          :key="item.to"
          :to="item.to"
          class="flex items-center gap-3 px-3 py-2.5 rounded-lg transition-colors group relative"
          :class="[
            route.path === item.to || (route.path.startsWith(item.to + '/') && item.to !== '/') 
              ? 'bg-indigo-50 text-indigo-700 font-medium' 
              : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
          ]"
          :title="isSidebarCollapsed ? item.name : ''"
        >
          <component :is="item.icon" class="w-5 h-5 shrink-0" 
            :class="[
              route.path === item.to || (route.path.startsWith(item.to + '/') && item.to !== '/') 
                ? 'text-indigo-600' : 'text-slate-400 group-hover:text-slate-600'
            ]" 
          />
          <span 
            class="whitespace-nowrap transition-opacity duration-300 text-sm overflow-hidden"
            :class="isSidebarCollapsed ? 'opacity-0 w-0 hidden' : 'opacity-100 block flex-1'"
          >{{ item.name }}</span>
        </router-link>
      </nav>

      <!-- Collapse Toggle (Desktop) -->
      <div class="hidden md:flex p-3 border-t border-slate-200 bg-white justify-end shrink-0">
        <button 
          @click="toggleSidebar"
          class="p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-700 rounded-lg transition-colors w-full flex"
          :class="isSidebarCollapsed ? 'justify-center' : 'justify-end'"
        >
          <ChevronRight v-if="isSidebarCollapsed" class="w-5 h-5" />
          <ChevronLeft v-else class="w-5 h-5" />
        </button>
      </div>
    </aside>

    <!-- Main Workspace -->
    <div class="flex-1 flex flex-col min-w-0 h-screen overflow-hidden">
      <!-- Top header -->
      <header class="h-16 flex items-center justify-between px-4 sm:px-6 bg-white border-b border-slate-200 z-10 shrink-0">
        <div class="flex items-center gap-4">
          <button 
            @click="isMobileMenuOpen = true" 
            class="md:hidden text-slate-500 hover:bg-slate-100 p-2 rounded-lg"
          >
            <Menu class="w-5 h-5" />
          </button>
          
          <div class="hidden sm:block text-sm font-medium text-slate-900">
             {{ roleLabel }}
          </div>
        </div>

        <div class="flex items-center gap-4 relative">
          <div class="flex items-center gap-3 relative group">
            <div class="text-right hidden sm:block">
              <div class="text-sm font-semibold text-slate-900">{{ displayName }}</div>
              <div class="text-xs text-slate-500">{{ roleLabel }}</div>
            </div>
            <img :src="avatarUrl" alt="Avatar" class="w-8 h-8 rounded-full ring-2 ring-slate-100 cursor-pointer">
            
            <div class="absolute right-0 top-full mt-2 w-48 bg-white border border-slate-200 rounded-xl shadow-sm opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 z-50">
              <div class="p-4 border-b border-slate-100 md:hidden">
                <div class="text-sm font-medium text-slate-900">{{ displayName }}</div>
                <div class="text-xs text-slate-500">{{ roleLabel }}</div>
              </div>
              <div class="p-2">
                <button 
                  @click="handleLogout"
                  class="w-full flex items-center gap-2 px-3 py-2 text-sm text-rose-600 hover:bg-rose-50 rounded-lg transition-colors font-medium text-left"
                >
                  <LogOut class="w-4 h-4" />
                  退出登录
                </button>
              </div>
            </div>
          </div>
        </div>
      </header>

      <!-- Main Content scrollable area -->
      <main class="flex-1 overflow-auto bg-slate-50/50 p-4 sm:p-6 lg:p-8">
        <div class="mx-auto max-w-7xl h-full">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(4px);
}
</style>
