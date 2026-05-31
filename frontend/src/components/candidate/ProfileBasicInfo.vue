<template>
  <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-sm">
    <div class="mb-8 flex items-center justify-between gap-4">
      <div>
        <h2 class="flex items-center gap-2 text-xl font-bold text-slate-800">
          <User class="h-6 w-6 text-blue-600" />
          基本资料
        </h2>
        <p class="mt-1 text-sm text-slate-500">更新您的基础履历与对外展示链接</p>
      </div>
      <span class="rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700 border border-blue-100">Candidate Profile</span>
    </div>

    <form class="space-y-6" @submit.prevent="saveProfile">
      <div class="grid grid-cols-1 gap-6 md:grid-cols-2">
        <div>
          <label class="mb-2 block text-sm font-semibold text-slate-700">用户名</label>
          <input
            :value="profile.username"
            type="text"
            disabled
            class="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-500 outline-none cursor-not-allowed"
          >
        </div>
        <div>
          <label class="mb-2 block text-sm font-semibold text-slate-700">邮箱</label>
          <input
            :value="profile.email"
            type="email"
            disabled
            class="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-500 outline-none cursor-not-allowed"
          >
        </div>
      </div>

      <div>
        <label class="mb-2 block text-sm font-semibold text-slate-700 hidden-required">GitHub 链接</label>
        <div class="relative">
          <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <Github class="h-5 w-5 text-slate-400" />
          </div>
          <input
            v-model.trim="form.githubUrl"
            type="url"
            placeholder="https://github.com/your-name"
            class="w-full rounded-xl border border-slate-200 bg-white pl-10 pr-4 py-3 text-slate-800 placeholder-slate-400 transition-all focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
          >
        </div>
      </div>

      <div>
        <label class="mb-2 block text-sm font-semibold text-slate-700">作品集链接</label>
        <div class="relative">
          <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <Link class="h-5 w-5 text-slate-400" />
          </div>
          <input
            v-model.trim="form.portfolioUrl"
            type="url"
            placeholder="https://your-portfolio.example.com"
            class="w-full rounded-xl border border-slate-200 bg-white pl-10 pr-4 py-3 text-slate-800 placeholder-slate-400 transition-all focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
          >
        </div>
      </div>

      <div class="rounded-xl border border-slate-100 bg-slate-50 p-4 text-sm text-slate-500 flex gap-3">
        <Clock class="w-5 h-5 flex-shrink-0 text-slate-400" />
        <div>
          <p class="font-medium text-slate-700">上次更新时间</p>
          <p class="mt-0.5">{{ profile.updatedAt ? new Date(profile.updatedAt).toLocaleString() : '尚未保存资料' }}</p>
        </div>
      </div>

      <div class="flex justify-end pt-4">
        <button
          type="submit"
          :disabled="updating"
          class="inline-flex w-full md:w-auto items-center justify-center gap-2 rounded-xl bg-blue-600 px-6 py-3 font-semibold text-white shadow-sm transition hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-70"
        >
          <Loader2 v-if="updating" class="h-5 w-5 animate-spin" />
          <Save v-else class="h-5 w-5" />
          {{ updating ? '正在保存...' : '保存资料' }}
        </button>
      </div>
    </form>
  </section>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue';
import { User, Github, Link, Clock, Save, Loader2 } from 'lucide-vue-next';
import api from '@/utils/api';

const props = defineProps<{
  profile: any;
}>();

const emit = defineEmits<{
  (e: 'update', data: any): void;
}>();

const updating = ref(false);
const form = reactive({
  githubUrl: '',
  portfolioUrl: ''
});

watch(() => props.profile, (newProfile) => {
  if (newProfile) {
    form.githubUrl = newProfile.githubUrl || '';
    form.portfolioUrl = newProfile.portfolioUrl || '';
  }
}, { immediate: true });

const saveProfile = async () => {
  try {
    updating.value = true;
    const response: any = await api.put('/candidate/profile', form);
    emit('update', response.data);
  } catch (error) {
    console.error(error);
    alert('保存失败，请重试');
  } finally {
    updating.value = false;
  }
};
</script>
