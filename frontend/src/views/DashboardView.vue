<template>
  <div class="h-full flex flex-col p-6 space-y-6 overflow-y-auto w-full relative">
    <!-- Loading Overlay -->
    <div v-if="loading" class="absolute inset-0 z-50 flex items-center justify-center bg-white/50 backdrop-blur-sm rounded-xl">
      <div class="flex flex-col items-center">
        <i class="fas fa-spinner fa-spin text-4xl text-blue-600 mb-4"></i>
        <p class="text-slate-600 font-medium">加载数据中...</p>
      </div>
    </div>

    <div class="flex justify-between items-center bg-white/50 backdrop-blur-xl p-6 rounded-2xl border border-white/20 shadow-sm shrink-0">
      <div>
        <h1 class="text-2xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-blue-600 to-indigo-600">招聘概览与数据大屏</h1>
        <p class="text-sm text-gray-500 mt-1">实时监控系统招聘吞吐量、AI 评估指标与漏斗转化</p>
      </div>
      <div class="flex items-center gap-3">
        <select v-model="timeRange" @change="fetchStats" class="px-4 py-2 bg-white text-slate-700 rounded-lg shadow-sm border border-slate-200 font-medium outline-none focus:ring-2 focus:ring-blue-500 cursor-pointer">
          <option :value="7">近 7 天</option>
          <option :value="30">近 30 天</option>
          <option :value="90">近 90 天</option>
        </select>
        <button class="px-4 py-2 bg-white text-slate-700 rounded-lg shadow-sm border border-slate-200 font-medium hover:bg-slate-50 transition flex items-center gap-2">
          <i class="fas fa-download"></i> 导出报表
        </button>
      </div>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 shrink-0">
      <div v-for="stat in stats" :key="stat.name" class="bg-white/70 backdrop-blur-md rounded-2xl p-6 border border-white/40 shadow-sm relative overflow-hidden group hover:shadow-md transition">
        <div :class="stat.color" class="absolute -right-4 -top-4 w-24 h-24 rounded-full opacity-10 group-hover:scale-150 transition-transform duration-500"></div>
        <div class="flex justify-between items-start mb-4">
          <h3 class="text-sm font-medium text-slate-500">{{ stat.name }}</h3>
          <div :class="stat.iconBg" class="w-10 h-10 rounded-xl flex items-center justify-center text-lg">
            <i :class="stat.icon"></i>
          </div>
        </div>
        <div class="text-3xl font-bold text-slate-800">{{ stat.value }}</div>
        <div class="mt-2 text-sm" :class="stat.trend > 0 ? 'text-green-500' : (stat.trend < 0 ? 'text-red-500' : 'text-gray-500')">
          <i v-if="stat.trend !== 0" :class="stat.trend > 0 ? 'fas fa-arrow-up' : 'fas fa-arrow-down'"></i>
          <span class="ml-1">{{ Math.abs(stat.trend) }}% 较上周</span>
        </div>
      </div>
    </div>

    <!-- Charts Row 1: Funnel & Pie -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 shrink-0 h-96">
      <!-- Funnel Chart -->
      <div class="lg:col-span-2 bg-white/70 backdrop-blur-md border border-white/40 shadow-sm rounded-2xl p-6 h-full flex flex-col">
        <h3 class="text-lg font-bold text-slate-800 mb-2">招聘转化漏斗</h3>
        <p class="text-xs text-slate-400 mb-4">从收到简历到发放 Offer 的全链路追踪</p>
        <div class="flex-1 w-full relative min-h-0">
          <v-chart class="absolute inset-0 w-full h-full" :option="funnelOption" autoresize />
        </div>
      </div>

      <!-- Pie Chart -->
      <div class="bg-white/70 backdrop-blur-md border border-white/40 shadow-sm rounded-2xl p-6 h-full flex flex-col">
        <h3 class="text-lg font-bold text-slate-800 mb-2">候选人技术栈画像</h3>
        <p class="text-xs text-slate-400 mb-4">简历库热门技能分布</p>
        <div class="flex-1 w-full relative min-h-0">
          <v-chart class="absolute inset-0 w-full h-full" :option="pieOption" autoresize />
        </div>
      </div>
    </div>

    <!-- Charts Row 2: Line/Area Chart -->
    <div class="bg-white/70 backdrop-blur-md border border-white/40 shadow-sm rounded-2xl p-6 h-80 flex flex-col shrink-0">
      <div class="flex justify-between items-center mb-2">
        <h3 class="text-lg font-bold text-slate-800">系统吞吐趋势</h3>
        <select class="bg-slate-50 border border-slate-200 text-sm rounded-lg px-2 py-1 outline-none text-slate-600">
          <option>近 7 天</option>
          <option>近 30 天</option>
        </select>
      </div>
      <p class="text-xs text-slate-400 mb-4">每日简历解析增量与 AI 自动化处理量</p>
      <div class="flex-1 w-full relative min-h-0">
        <v-chart class="absolute inset-0 w-full h-full" :option="lineOption" autoresize />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, FunnelChart, LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent, ToolboxComponent } from 'echarts/components'
import api from '@/utils/api'

use([
  CanvasRenderer,
  PieChart,
  FunnelChart,
  LineChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  ToolboxComponent
])

const loading = ref(true)
const timeRange = ref(7)

// Stats Initial Data
const stats = ref([
  { name: '累计简历池', value: '0', trend: 0, icon: 'fas fa-file-alt text-blue-600', iconBg: 'bg-blue-100', color: 'bg-blue-500' },
  { name: 'AI 自动解析', value: '0', trend: 0, icon: 'fas fa-brain text-purple-600', iconBg: 'bg-purple-100', color: 'bg-purple-500' },
  { name: '进入面试池', value: '0', trend: 0, icon: 'fas fa-comments text-indigo-600', iconBg: 'bg-indigo-100', color: 'bg-indigo-500' },
  { name: '发出的 Offer', value: '0', trend: 0, icon: 'fas fa-trophy text-green-600', iconBg: 'bg-green-100', color: 'bg-green-500' }
])

const funnelOption = ref({
  tooltip: { trigger: 'item', formatter: '{a} <br/>{b} : {c}' },
  series: [
    {
      name: '招聘漏斗', type: 'funnel', left: '10%', top: 20, bottom: 20, width: '80%', minSize: '0%', maxSize: '100%',
      sort: 'descending', gap: 2,
      label: { show: true, position: 'inside' },
      data: [] as any[]
    }
  ],
  color: ['#3b82f6', '#6366f1', '#8b5cf6', '#a855f7', '#10b981']
})

const pieOption = ref({
  tooltip: { trigger: 'item' },
  legend: { bottom: '0%', left: 'center', itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11 } },
  series: [
    {
      name: '技术栈', type: 'pie', radius: ['40%', '70%'], avoidLabelOverlap: false,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: false, position: 'center' },
      emphasis: { label: { show: true, fontSize: '14', fontWeight: 'bold' } },
      data: [] as any[]
    }
  ],
  color: ['#60a5fa', '#34d399', '#f472b6', '#fcd34d', '#fb923c']
})

const lineOption = ref({
  tooltip: { trigger: 'axis', axisPointer: { type: 'cross', label: { backgroundColor: '#6a7985' } } },
  legend: { data: ['收取量', '解析量'] },
  grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
  xAxis: [ { type: 'category', boundaryGap: false, data: [] as string[], axisLine: { lineStyle: { color: '#cbd5e1' } }, axisLabel: { color: '#64748b' } } ],
  yAxis: [ { type: 'value', axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: '#64748b' }, splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } } } ],
  series: [
    { name: '收取量', type: 'line', smooth: true, lineStyle: { width: 3, color: '#93c5fd' }, showSymbol: false, areaStyle: { opacity: 0.3, color: '#93c5fd' }, data: [] as number[] },
    { name: '解析量', type: 'line', smooth: true, lineStyle: { width: 3, color: '#818cf8' }, showSymbol: false, areaStyle: { opacity: 0.3, color: '#818cf8' }, data: [] as number[] }
  ]
})

const fetchStats = async () => {
  loading.value = true
  try {
    const response = await api.get('/hr/dashboard/stats', { params: { days: timeRange.value } })
    const data = response.data
    
    // Update Stats
    if (data.keyMetrics) {
      stats.value[0].value = data.keyMetrics.totalResumes?.toLocaleString() || '0'
      stats.value[1].value = data.keyMetrics.parsedResumes?.toLocaleString() || '0'
      stats.value[2].value = data.keyMetrics.interviewCount?.toLocaleString() || '0'
      stats.value[3].value = data.keyMetrics.offersSent?.toLocaleString() || '0'
    }
    
    // Update Funnel
    if (data.funnel) {
      funnelOption.value.series[0].data = data.funnel
    }
    
    // Update Pie
    if (data.skillsDistribution) {
      pieOption.value.series[0].data = data.skillsDistribution
    }
    
    // Update Line
    if (data.trends) {
      lineOption.value.xAxis[0].data = data.trends.dates || []
      lineOption.value.series[0].data = data.trends.received || []
      lineOption.value.series[1].data = data.trends.parsed || []
    }
  } catch (error) {
    console.error('Failed to fetch dashboard stats', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchStats()
})
</script>
