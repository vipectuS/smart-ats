<template>
  <div class="relative w-full">
    <div class="flex flex-col sm:flex-row gap-3">
      <div class="relative flex-1">
        <input
          ref="inputRef"
          v-model="query"
          @focus="openDropdown"
          @blur="closeDropdownTimeout"
          @keydown.down.prevent="moveDown"
          @keydown.up.prevent="moveUp"
          @keydown.enter.prevent="selectActive"
          @keydown.esc.prevent="closeDropdown"
          type="text"
          :placeholder="placeholder || '输入技能名进行检索 (支持首字母模糊检索)，按回车确认'"
          class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100"
        />
        <!-- 搜索匹配结果下拉狂 -->
        <ul
          v-if="isOpen && filteredItems.length > 0"
          class="absolute z-50 mt-1 max-h-60 w-full overflow-auto rounded-xl border border-slate-200 bg-white py-1 shadow-xl shadow-blue-900/10 ring-1 ring-black ring-opacity-5"
        >
          <li
            v-for="(item, index) in filteredItems"
            :key="item.name"
            @mousedown.prevent="selectItem(item)"
            @mouseenter="activeIndex = index"
            :class="[
              activeIndex === index ? 'bg-blue-50 text-blue-700 font-semibold' : 'text-slate-700',
              'relative cursor-pointer select-none py-2.5 px-4 text-sm transition-colors flex justify-between items-start'
            ]"
          >
            <span class="block min-w-0">
              <span class="block truncate">
                <template v-for="(segment, segIndex) in getHighlightedSegments(item.name)" :key="`${item.name}-${segIndex}`">
                  <span :class="segment.matched ? 'bg-amber-200/80 rounded px-0.5 text-slate-900' : ''">{{ segment.text }}</span>
                </template>
              </span>
              <span v-if="getAliasHint(item)" class="mt-1 block truncate text-[11px] text-slate-500">
                别名命中: {{ getAliasHint(item) }}
              </span>
            </span>
            <div class="ml-3 flex items-center gap-1.5">
              <span
                v-if="getMatchSource(item)"
                class="rounded px-1.5 py-0.5 text-[10px] leading-none"
                :class="getMatchSource(item) === 'alias' ? 'bg-emerald-100 text-emerald-700' : 'bg-sky-100 text-sky-700'"
              >
                {{ getMatchSource(item) === 'alias' ? '别名命中' : '名称命中' }}
              </span>
              <span v-if="activeIndex === index" class="text-[10px] bg-blue-100 text-blue-600 px-2 py-0.5 rounded flex items-center gap-1 whitespace-nowrap">
                <i class="fas fa-level-down-alt rotate-90"></i>
                回车确认
              </span>
            </div>
          </li>
        </ul>
        <!-- 未找到结果的占位 -->
        <div 
          v-else-if="isOpen && query"
          class="absolute z-50 mt-1 w-full rounded-xl border border-rose-100 bg-rose-50 py-3 px-4 shadow-lg text-sm text-rose-500 text-center flex items-center justify-center gap-2"
        >
          <i class="fas fa-exclamation-circle text-rose-500"></i>
          此技能不在系统标准词库中，我们提倡使用规范技能
        </div>
      </div>
    </div>
    
    <!-- 已选择的技能展示 -->
    <div class="flex flex-wrap gap-2 mt-4 min-h-[32px]">
      <button 
        v-for="skill in modelValue" 
        :key="skill" 
        type="button" 
        @click="removeSkill(skill)" 
        class="inline-flex items-center gap-2 rounded-full border border-blue-100 bg-blue-50 px-3 py-1.5 text-xs font-semibold text-blue-700 hover:bg-blue-200 hover:text-blue-800 transition"
      >
        <span>{{ skill }}</span>
        <i class="fas fa-times text-[10px]"></i>
      </button>
      <span v-if="modelValue.length === 0" class="text-xs text-slate-400 py-2 italic font-mono">尚未从字典中选取技能</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { PublicSkillCatalogItem, SkillOptionInput } from '@/types/skills'

const props = defineProps<{
  modelValue: string[]
  availableSkills: SkillOptionInput[]
  placeholder?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string[]): void
}>()

const query = ref('')
const isOpen = ref(false)
const activeIndex = ref(0)
const inputRef = ref<HTMLInputElement | null>(null)

interface NormalizedSkillOption {
  name: string
  aliases: string[]
}

interface HighlightSegment {
  text: string
  matched: boolean
}

const normalizedOptions = computed<NormalizedSkillOption[]>(() => {
  const seen = new Set<string>()
  const options: NormalizedSkillOption[] = []

  for (const item of props.availableSkills) {
    const option: PublicSkillCatalogItem =
      typeof item === 'string'
        ? { name: item, aliases: [] }
        : { name: item.name, aliases: item.aliases || [] }

    const name = option.name.trim()
    if (!name || seen.has(name)) continue
    seen.add(name)

    options.push({
      name,
      aliases: option.aliases
        .map((alias) => alias.trim())
        .filter((alias) => alias.length > 0),
    })
  }

  return options
})

// 极其智能的模糊搜索 (极简且高效)
const filteredItems = computed(() => {
  // 过滤已经选择的技能
  const unselected = normalizedOptions.value.filter((option) => !props.modelValue.includes(option.name))
  if (!query.value) {
    return unselected.slice(0, 30) // 默认展示前30条
  }
  
  const qStr = query.value.toLowerCase().trim()
  // 构建乱序匹配正则，如 "vjs" -> /v.*j.*s/i，允许用户漏打或首字母缩写
  const chars = qStr.split('').map(c => c.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'))
  const sparseRegex = new RegExp(chars.join('.*'), 'i')
  
  const results = unselected.filter((option) => {
    const candidates = [option.name, ...option.aliases]
    return candidates.some((candidate) => sparseRegex.test(candidate))
  })
  
  // 按匹配度排序（精确前缀 > 连续子串匹配 > 缩写离散匹配）
  results.sort((a, b) => {
    const aLower = a.name.toLowerCase()
    const bLower = b.name.toLowerCase()
    const aAlias = a.aliases.join(' ').toLowerCase()
    const bAlias = b.aliases.join(' ').toLowerCase()
    
    // 1. 完全一致
    if (aLower === qStr && bLower !== qStr) return -1
    if (aLower !== qStr && bLower === qStr) return 1
    
    // 2. 前缀一致优先
    const aPrefix = aLower.startsWith(qStr)
    const bPrefix = bLower.startsWith(qStr)
    if (aPrefix && !bPrefix) return -1
    if (!aPrefix && bPrefix) return 1
    
    // 3. 连续子串优先
    const aHasSub = aLower.includes(qStr) || aAlias.includes(qStr)
    const bHasSub = bLower.includes(qStr) || bAlias.includes(qStr)
    if (aHasSub && !bHasSub) return -1
    if (!aHasSub && bHasSub) return 1
    
    // 4. 字符串越短越匹配（靠前）
    return a.name.length - b.name.length
  })
  
  return results.slice(0, 30)
})

watch(query, () => {
  activeIndex.value = 0
  if (query.value && !isOpen.value) {
    isOpen.value = true
  }
})

function openDropdown() {
  isOpen.value = true
}

function closeDropdown() {
  isOpen.value = false
  activeIndex.value = 0
}

function closeDropdownTimeout() {
  setTimeout(() => {
    closeDropdown()
  }, 150)
}

function moveDown() {
  if (!isOpen.value) {
    isOpen.value = true
    return
  }
  if (activeIndex.value < filteredItems.value.length - 1) {
    activeIndex.value++
    // 此处可添加滚动逻辑，但对于最大高度的列表，Vue 响应式结合 css overflow 已足够
  }
}

function moveUp() {
  if (activeIndex.value > 0) {
    activeIndex.value--
  } else {
    activeIndex.value = filteredItems.value.length - 1
  }
}

function selectActive() {
  if (isOpen.value && filteredItems.value.length > 0) {
    selectItem(filteredItems.value[activeIndex.value])
  } else if (isOpen.value && filteredItems.value.length === 0) {
    // 强制不能输入非词典中的技能，所以清空无效输入
    query.value = ''
  }
}

function getMatchSource(item: NormalizedSkillOption): 'name' | 'alias' | null {
  const q = query.value.trim().toLowerCase()
  if (!q) return null

  const nameLower = item.name.toLowerCase()
  if (nameLower.includes(q)) {
    return 'name'
  }

  const aliasMatched = item.aliases.some((alias) => alias.toLowerCase().includes(q))
  return aliasMatched ? 'alias' : null
}

function getAliasHint(item: NormalizedSkillOption): string {
  const q = query.value.trim().toLowerCase()
  if (!q) return ''

  const matchedAlias = item.aliases.find((alias) => alias.toLowerCase().includes(q))
  return matchedAlias || ''
}

function getHighlightedSegments(text: string): HighlightSegment[] {
  const q = query.value.trim().toLowerCase()
  if (!q) {
    return [{ text, matched: false }]
  }

  const lower = text.toLowerCase()
  const start = lower.indexOf(q)
  if (start === -1) {
    return [{ text, matched: false }]
  }

  const end = start + q.length
  return [
    { text: text.slice(0, start), matched: false },
    { text: text.slice(start, end), matched: true },
    { text: text.slice(end), matched: false },
  ].filter((segment) => segment.text.length > 0)
}

function selectItem(item: NormalizedSkillOption) {
  if (!item?.name) return
  const current = [...props.modelValue]
  if (!current.includes(item.name)) {
    current.push(item.name)
    emit('update:modelValue', current)
  }
  query.value = ''
  closeDropdown()
  inputRef.value?.focus()
}

function removeSkill(skill: string) {
  const current = props.modelValue.filter(s => s !== skill)
  emit('update:modelValue', current)
}
</script>