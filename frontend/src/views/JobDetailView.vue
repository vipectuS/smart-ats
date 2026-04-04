<template>
  <div class="h-full flex flex-col pt-4 px-6 space-y-6 overflow-hidden">
    <!-- Header -->
    <div class="flex-shrink-0 flex flex-col bg-white/50 backdrop-blur-xl p-6 rounded-2xl border border-white/20 shadow-sm gap-4" v-if="job">
      <div class="flex justify-between items-center">
        <div>
          <div class="flex items-center gap-3 mb-1">
            <button @click="router.back()" class="text-gray-400 hover:text-blue-600 transition">
              <i class="fas fa-arrow-left"></i>
            </button>
            <h1 class="text-2xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-blue-600 to-indigo-600">{{ job.title }}</h1>
          </div>
          <p class="text-sm text-gray-500 ml-8">{{ job.department }} • {{ job.location }} • {{ job.headcount }} 人</p>
        </div>
        <div>
          <button @click="evaluateJob" :disabled="evaluating"
            class="px-5 py-2.5 bg-gradient-to-r from-indigo-500 to-blue-600 text-white rounded-xl shadow-md font-medium hover:shadow-lg transition flex items-center gap-2">
            <i :class="evaluating ? 'fas fa-spinner fa-spin' : 'fas fa-magic'"></i>
            {{ evaluating ? '按自定义权重重新评估' : 'AI 动态画像评估' }}
          </button>
        </div>
      </div>
      
      <!-- Interactive HR Weight Adjustments (Graduation Project Feature) -->
      <div class="mt-2 pt-4 border-t border-slate-200">
        <h3 class="text-sm font-bold text-slate-700 flex items-center gap-2 mb-4">
          <i class="fas fa-sliders-h text-blue-500"></i> 交互式画像权重雷达调整
        </h3>
        <div class="grid grid-cols-1 md:grid-cols-4 gap-6">
          <div class="space-y-2">
            <div class="flex justify-between text-xs text-slate-600 font-medium">
              <span>硬技能匹配 (Skill)</span>
              <span class="text-blue-600">{{ weights.skillWeight }}%</span>
            </div>
            <input type="range" v-model="weights.skillWeight" min="0" max="100" class="w-full h-1.5 bg-blue-100 rounded-lg appearance-none cursor-pointer accent-blue-600">
          </div>
          <div class="space-y-2">
            <div class="flex justify-between text-xs text-slate-600 font-medium">
              <span>项目经验 (Experience)</span>
              <span class="text-indigo-600">{{ weights.experienceWeight }}%</span>
            </div>
            <input type="range" v-model="weights.experienceWeight" min="0" max="100" class="w-full h-1.5 bg-indigo-100 rounded-lg appearance-none cursor-pointer accent-indigo-600">
          </div>
          <div class="space-y-2">
            <div class="flex justify-between text-xs text-slate-600 font-medium">
              <span>教育背景 (Education)</span>
              <span class="text-purple-600">{{ weights.educationWeight }}%</span>
            </div>
            <input type="range" v-model="weights.educationWeight" min="0" max="100" class="w-full h-1.5 bg-purple-100 rounded-lg appearance-none cursor-pointer accent-purple-600">
          </div>
          <div class="space-y-2">
            <div class="flex justify-between text-xs text-slate-600 font-medium">
              <span>非结构化语义深度 (pgvector)</span>
              <span class="text-green-600">{{ weights.semanticWeight }}%</span>
            </div>
            <input type="range" v-model="weights.semanticWeight" min="0" max="100" class="w-full h-1.5 bg-green-100 rounded-lg appearance-none cursor-pointer accent-green-600">
          </div>
        </div>
      </div>
    </div>

    <!-- Recommendations List -->
    <div class="flex-1 overflow-y-auto min-h-0 space-y-6 pb-6">
      <div v-if="loading" class="text-center py-10 text-gray-500">
        <i class="fas fa-circle-notch fa-spin text-3xl mb-3 text-blue-500"></i>
        <p>正在加载推荐结果...</p>
      </div>

      <div v-else-if="recommendations.length === 0" class="text-center py-10 text-gray-500 bg-white/50 rounded-2xl">
        <div class="mb-3 text-4xl">📭</div>
        <p>暂无评估的候选人。请点击上方按钮进行 AI 评估。</p>
      </div>

      <div v-else v-for="rec in recommendations" :key="rec.resumeId" class="bg-white/80 backdrop-blur-md rounded-2xl shadow-sm border border-slate-100 p-6 flex flex-col lg:flex-row gap-6">
        <!-- Candidate Info & Reasoning -->
        <div class="flex-1 space-y-4">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-4">
              <div class="w-12 h-12 rounded-full bg-gradient-to-br from-indigo-100 to-purple-100 text-indigo-600 flex items-center justify-center font-bold text-lg border border-indigo-200">
                {{ rec.candidateName?.charAt(0) || '?' }}
              </div>
              <div>
                <h3 class="text-lg font-bold text-slate-800">{{ rec.candidateName || '未知候选人' }}</h3>
                <p class="text-sm text-slate-500">匹配得分: <span class="font-bold text-blue-600 text-lg">{{ rec.matchScore }}</span>/100</p>
              </div>
            </div>
            <div class="px-3 py-1 bg-blue-50 text-blue-600 rounded-lg text-sm font-medium border border-blue-100 flex items-center gap-1.5">
               <i class="fas fa-robot"></i> AI 首选
            </div>
          </div>
          
          <div class="bg-slate-50/50 p-4 rounded-xl border border-slate-100 text-sm text-slate-700 leading-relaxed">
            <h4 class="font-semibold text-slate-800 mb-2 flex items-center gap-2"><i class="fas fa-brain text-purple-500"></i> AI 评估理由</h4>
            {{ rec.xaiReasoning }}
          </div>
        </div>

        <!-- Radar Chart -->
        <div class="w-full lg:w-72 h-64 shrink-0 bg-white rounded-xl shadow-inner border border-slate-50 flex items-center justify-center relative">
           <v-chart class="w-full h-full" :option="getRadarOption(rec)" autoresize />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../utils/api'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { RadarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([CanvasRenderer, RadarChart, TitleComponent, TooltipComponent])

const route = useRoute()
const router = useRouter()
const jobId = route.params.id

const job = ref<any>(null)
const recommendations = ref<any[]>([])
const loading = ref(true)
const evaluating = ref(false)

const weights = ref({
  skillWeight: 40,
  experienceWeight: 30,
  educationWeight: 10,
  semanticWeight: 20
})

const fetchJob = async () => {
  try {
    const res: any = await api.get(`/jobs/${jobId}`)
    job.value = res.data || res
  } catch (error) {
    console.error('Failed to fetch job', error)
  }
}

const fetchRecommendations = async () => {
  try {
    loading.value = true
    const res: any = await api.get(`/jobs/${jobId}/recommendations`)
    recommendations.value = res.data || res || []
  } catch (error) {
    console.error('Failed to fetch recommendations', error)
  } finally {
    loading.value = false
  }
}

const evaluateJob = async () => {
  try {
    evaluating.value = true
    await api.post(`/jobs/${jobId}/evaluate`, weights.value)
    await fetchRecommendations()
  } catch (error) {
    console.error('Failed to evaluate job', error)
    alert('评估请求失败')
  } finally {
    evaluating.value = false
  }
}

const getRadarOption = (rec: any) => {
  const scores = rec.radarScores || {}
  
  return {
    tooltip: {
      trigger: 'item'
    },
    radar: {
      indicator: [
        { name: '技能匹配', max: 100 },
        { name: '教育背景', max: 100 },
        { name: '工作经验', max: 100 },
        { name: '项目经验', max: 100 },
        { name: '稳定性', max: 100 }
      ],
      radius: '60%',
      splitArea: {
        areaStyle: {
          color: ['rgba(250,250,250,0.3)', 'rgba(200,200,200,0.1)']
        }
      }
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: [
              scores.skillsScore || 0,
              scores.educationScore || 0,
              scores.experienceScore || 0,
              scores.projectScore || 0,
              scores.stabilityScore || 0
            ],
            name: '候选人画像',
            areaStyle: {
              color: 'rgba(99, 102, 241, 0.2)'
            },
            lineStyle: {
              color: 'rgba(99, 102, 241, 1)'
            },
            itemStyle: {
              color: 'rgba(99, 102, 241, 1)'
            }
          }
        ]
      }
    ]
  }
}

onMounted(() => {
  fetchJob()
  fetchRecommendations()
})
</script>
