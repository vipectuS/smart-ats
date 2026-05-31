<script setup lang="ts">
import { reactive, ref } from 'vue'
import api from '@/utils/api'
import { resolveApiError } from '@/utils/apiError'
import type { AdminOrganization, AdminOrganizationTokenResponse } from '@/types/admin'
import { Building2, Save, RefreshCw, PencilLine, ShieldAlert } from 'lucide-vue-next'

const props = defineProps<{ organizations: AdminOrganization[] }>()
const emit = defineEmits(['refresh'])

const organizationSaving = ref(false)
const organizationErrorMsg = ref('')
const organizationSaveMsg = ref('')
const generatedOrganizationToken = ref('')
const editingOrganizationId = ref<string | null>(null)

const organizationForm = reactive({ name: '', enabled: true })

const resetOrganizationForm = () => {
  editingOrganizationId.value = null
  organizationForm.name = ''
  organizationForm.enabled = true
  organizationErrorMsg.value = ''
  organizationSaveMsg.value = ''
  generatedOrganizationToken.value = ''
}

const editOrganization = (organization: AdminOrganization) => {
  editingOrganizationId.value = organization.id
  organizationForm.name = organization.name
  organizationForm.enabled = organization.enabled
  organizationErrorMsg.value = ''
  organizationSaveMsg.value = ''
  generatedOrganizationToken.value = ''
}

const submitOrganization = async () => {
  organizationSaving.value = true
  organizationErrorMsg.value = ''
  organizationSaveMsg.value = ''
  generatedOrganizationToken.value = ''

  try {
    if (editingOrganizationId.value) {
      await api.put(`/admin/organizations/${editingOrganizationId.value}`, {
        name: organizationForm.name.trim(),
        enabled: organizationForm.enabled,
      })
      organizationSaveMsg.value = '组织信息已更新。'
      emit('refresh')
      resetOrganizationForm()
    } else {
      const response = await api.post('/admin/organizations', { name: organizationForm.name.trim() })
      const payload = response.data as AdminOrganizationTokenResponse
      organizationSaveMsg.value = `组织 ${payload.organization.name} 已创建。`
      generatedOrganizationToken.value = payload.generatedToken
      emit('refresh')
      editingOrganizationId.value = payload.organization.id
      organizationForm.name = payload.organization.name
      organizationForm.enabled = payload.organization.enabled
    }
  } catch (error: any) {
    organizationErrorMsg.value = resolveApiError(error, '保存组织失败。').summary
  } finally {
    organizationSaving.value = false
  }
}

const regenerateOrganizationToken = async (organization: AdminOrganization) => {
  organizationSaving.value = true
  organizationErrorMsg.value = ''
  organizationSaveMsg.value = ''

  try {
    const response = await api.post(`/admin/organizations/${organization.id}/regenerate-token`)
    const payload = response.data as AdminOrganizationTokenResponse
    organizationSaveMsg.value = `已为 ${payload.organization.name} 生成新令牌。`
    generatedOrganizationToken.value = payload.generatedToken
    emit('refresh')
  } catch (error: any) {
    organizationErrorMsg.value = resolveApiError(error, '重置组织令牌失败。').summary
  } finally {
    organizationSaving.value = false
  }
}
const formatTime = (value: string) => new Date(value).toLocaleString('zh-CN')
</script>
<template>
  <div class="grid gap-6 xl:grid-cols-2">
    <article class="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <div class="flex items-center justify-between gap-4 border-b border-slate-100 pb-4 mb-4">
        <div>
          <h2 class="text-lg font-semibold text-slate-900 flex items-center gap-2"><Building2 class="w-5 h-5 text-indigo-500" />新建/更新组织</h2>
          <p class="text-sm text-slate-500">创建组织并生成 HR 注册令牌。</p>
        </div>
        <button class="bg-indigo-50 text-indigo-600 px-3 py-1.5 rounded-lg text-sm font-medium hover:bg-indigo-100 transition" @click="resetOrganizationForm">重新创建</button>
      </div>
      
      <div v-if="organizationErrorMsg" class="mb-4 bg-rose-50 text-rose-700 px-4 py-3 rounded-lg text-sm">{{ organizationErrorMsg }}</div>
      <div v-else-if="organizationSaveMsg" class="mb-4 bg-emerald-50 text-emerald-700 px-4 py-3 rounded-lg text-sm">{{ organizationSaveMsg }}</div>
      
      <div v-if="generatedOrganizationToken" class="mb-4 bg-amber-50 border border-amber-200 px-4 py-4 rounded-lg text-amber-900 text-sm">
        <p class="font-semibold flex items-center gap-2"><ShieldAlert class="w-4 h-4" />新令牌已生成</p>
        <p class="mt-2 font-mono text-xs break-all bg-white p-2 rounded border border-amber-100">{{ generatedOrganizationToken }}</p>
        <p class="mt-2 text-xs text-amber-700">请立即复制令牌并分发给组织 HR，刷新将不再明文显示。</p>
      </div>

      <form @submit.prevent="submitOrganization" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1">组织名称</label>
          <input v-model="organizationForm.name" required class="w-full bg-white border border-slate-200 rounded-lg px-3 py-2 text-sm focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500 outline-none" />
        </div>
        <div class="flex items-center gap-2">
          <input v-model="organizationForm.enabled" type="checkbox" class="rounded border-slate-300 text-indigo-600 focus:ring-indigo-500 w-4 h-4" />
          <span class="text-sm text-slate-700">组织状态启用 (允许注册)</span>
        </div>
        <button :disabled="organizationSaving" class="mt-2 w-full flex items-center justify-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2.5 rounded-lg text-sm font-medium transition disabled:bg-slate-400">
          <Save class="w-4 h-4" />{{ organizationSaving ? '保存中...' : (editingOrganizationId ? '更新组织' : '创建并生成令牌') }}
        </button>
      </form>
    </article>

    <article class="rounded-xl border border-slate-200 bg-white p-6 shadow-sm flex flex-col max-h-[600px]">
      <h2 class="text-lg font-semibold text-slate-900 border-b border-slate-100 pb-4 mb-4 flex items-center justify-between">
        列表 <span class="text-xs bg-slate-100 text-slate-600 px-2 py-1 rounded-md">{{ organizations.length }} 个记录</span>
      </h2>
      <div class="flex-1 overflow-y-auto pr-2">
        <div v-if="organizations.length === 0" class="text-center py-10 text-slate-400 text-sm">暂无任何组织记录，请先在左侧创建。</div>
        <ul class="divide-y divide-slate-100">
          <li v-for="org in organizations" :key="org.id" class="py-4 px-2 hover:bg-slate-50 transition group">
             <div class="flex justify-between items-start mb-2">
                <div class="flex items-center gap-2">
                  <span class="font-medium text-slate-900">{{ org.name }}</span>
                  <span class="text-[10px] px-2 py-0.5 rounded" :class="org.enabled ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-200 text-slate-600'">{{ org.enabled ? '正常' : '停用' }}</span>
                </div>
                <div class="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition">
                   <button @click="editOrganization(org)" class="text-slate-400 hover:text-indigo-600 p-1"><PencilLine class="w-4 h-4" /></button>
                   <button @click="regenerateOrganizationToken(org)" class="text-slate-400 hover:text-rose-600 p-1"><RefreshCw class="w-4 h-4" /></button>
                </div>
             </div>
             <div class="text-xs text-slate-500 mb-1">Token: {{ org.tokenPreview }}...</div>
             <div class="flex items-center gap-3 text-xs text-slate-500">
                <span>HR: {{ org.hrCount }}</span>
                <span>岗位: {{ org.jobCount }}</span>
                <span>{{ formatTime(org.updatedAt) }}</span>
             </div>
          </li>
        </ul>
      </div>
    </article>
  </div>
</template>
