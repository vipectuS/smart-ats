<script setup lang="ts">
import { reactive, ref } from 'vue'
import api from '@/utils/api'
import { resolveApiError } from '@/utils/apiError'
import type { AdminSkill, AdminSkillUpsertPayload } from '@/types/admin'
import { BookOpen, Save, PencilLine } from 'lucide-vue-next'

const props = defineProps<{ skills: AdminSkill[] }>()
const emit = defineEmits(['refresh'])

const saving = ref(false)
const errorMsg = ref('')
const saveMsg = ref('')
const editingSkillId = ref<string | null>(null)

const skillForm = reactive({ name: '', category: '', aliases: '', enabled: true })

const resetForm = () => {
  editingSkillId.value = null
  skillForm.name = ''
  skillForm.category = ''
  skillForm.aliases = ''
  skillForm.enabled = true
  saveMsg.value = ''
  errorMsg.value = ''
}

const editSkill = (skill: AdminSkill) => {
  editingSkillId.value = skill.id
  skillForm.name = skill.name
  skillForm.category = skill.category || ''
  skillForm.aliases = skill.aliases.join(', ')
  skillForm.enabled = skill.enabled
  saveMsg.value = ''
  errorMsg.value = ''
}

const submitSkill = async () => {
  saving.value = true
  errorMsg.value = ''
  saveMsg.value = ''

  const payload: AdminSkillUpsertPayload = {
    name: skillForm.name.trim(),
    category: skillForm.category.trim() || null,
    aliases: skillForm.aliases.split(',').map(item => item.trim()).filter(Boolean),
    enabled: skillForm.enabled,
  }

  try {
    if (editingSkillId.value) {
      await api.put(`/admin/skills/${editingSkillId.value}`, payload)
      saveMsg.value = '技能词条已更新。'
    } else {
      await api.post('/admin/skills', payload)
      saveMsg.value = '技能词条已创建。'
    }
    emit('refresh')
    resetForm()
  } catch (error: any) {
    errorMsg.value = resolveApiError(error, '保存技能词条失败。').summary
  } finally {
    saving.value = false
  }
}
// const formatTime = (value: string) => new Date(value).toLocaleString('zh-CN')
</script>
<template>
  <div class="grid gap-6 xl:grid-cols-2">
    <article class="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <div class="flex items-center justify-between gap-4 border-b border-slate-100 pb-4 mb-4">
        <div>
          <h2 class="text-lg font-semibold text-slate-900 flex items-center gap-2"><BookOpen class="w-5 h-5 text-indigo-500" />技能词典入口</h2>
          <p class="text-sm text-slate-500">统一维护解析引擎映射使用的归一化词典。</p>
        </div>
        <button class="bg-indigo-50 text-indigo-600 px-3 py-1.5 rounded-lg text-sm font-medium hover:bg-indigo-100 transition" @click="resetForm">新增</button>
      </div>

      <div v-if="errorMsg" class="mb-4 bg-rose-50 text-rose-700 px-4 py-3 rounded-lg text-sm">{{ errorMsg }}</div>
      <div v-else-if="saveMsg" class="mb-4 bg-emerald-50 text-emerald-700 px-4 py-3 rounded-lg text-sm">{{ saveMsg }}</div>

      <form @submit.prevent="submitSkill" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1">主词条名称</label>
          <input v-model="skillForm.name" required class="w-full bg-white border border-slate-200 rounded-lg px-3 py-2 text-sm focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500 outline-none" />
        </div>
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1">分类边界 <span class="text-slate-400 font-normal">(可选)</span></label>
          <input v-model="skillForm.category" class="w-full bg-white border border-slate-200 rounded-lg px-3 py-2 text-sm focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500 outline-none" />
        </div>
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1">重定向别名 <span class="text-slate-400 font-normal">(逗号分隔)</span></label>
          <input v-model="skillForm.aliases" class="w-full bg-white border border-slate-200 rounded-lg px-3 py-2 text-sm focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500 outline-none" />
        </div>
        <div class="flex items-center gap-2">
          <input v-model="skillForm.enabled" type="checkbox" class="rounded border-slate-300 text-indigo-600 focus:ring-indigo-500 w-4 h-4" />
          <span class="text-sm text-slate-700">启用该词条用于规范化</span>
        </div>
        <button :disabled="saving" class="mt-2 w-full flex items-center justify-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2.5 rounded-lg text-sm font-medium transition disabled:bg-slate-400">
          <Save class="w-4 h-4" />{{ saving ? '保存中...' : (editingSkillId ? '保存词条' : '创建新词条') }}
        </button>
      </form>
    </article>

    <article class="rounded-xl border border-slate-200 bg-white p-6 shadow-sm flex flex-col max-h-[650px]">
       <h2 class="text-lg font-semibold text-slate-900 border-b border-slate-100 pb-4 mb-4 flex items-center justify-between">
        词典全集 <span class="text-xs bg-slate-100 text-slate-600 px-2 py-1 rounded-md">{{ skills.length }} 个记录</span>
      </h2>
      <div class="flex-1 overflow-y-auto pr-2">
         <div v-if="skills.length === 0" class="text-center py-10 text-slate-400 text-sm">空空如也，系统将采用默认不规范提词。</div>
         <ul class="divide-y divide-slate-100">
            <li v-for="skill in skills" :key="skill.id" class="py-4 px-2 hover:bg-slate-50 transition group">
                <div class="flex justify-between items-start">
                   <div>
                       <div class="flex items-center gap-2">
                          <span class="font-medium text-slate-900">{{ skill.name }}</span>
                          <span v-if="skill.category" class="bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded text-[10px]">{{ skill.category }}</span>
                       </div>
                       <p class="text-xs text-slate-500 mt-1">别名: {{ skill.aliases.length > 0 ? skill.aliases.join(', ') : '无' }}</p>
                   </div>
                   <div class="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition">
                       <button @click="editSkill(skill)" class="text-slate-400 hover:text-indigo-600 p-1"><PencilLine class="w-4 h-4" /></button>
                   </div>
                </div>
            </li>
         </ul>
      </div>
    </article>
  </div>
</template>
