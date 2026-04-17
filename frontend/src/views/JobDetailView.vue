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
          <p class="text-sm text-gray-500 ml-8">{{ getJobMeta(job) }}</p>
        </div>
        <div class="flex items-center gap-3">
          <button @click="openEditPanel" class="px-5 py-2.5 border border-slate-200 bg-white text-slate-700 rounded-xl shadow-sm font-medium hover:bg-slate-50 transition flex items-center gap-2">
            <i class="fas fa-pen"></i>
            编辑岗位
          </button>
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
            <input type="range" v-model.number="weights.skillWeight" min="0" max="100" class="w-full h-1.5 bg-blue-100 rounded-lg appearance-none cursor-pointer accent-blue-600">
          </div>
          <div class="space-y-2">
            <div class="flex justify-between text-xs text-slate-600 font-medium">
              <span>项目经验 (Experience)</span>
              <span class="text-indigo-600">{{ weights.experienceWeight }}%</span>
            </div>
            <input type="range" v-model.number="weights.experienceWeight" min="0" max="100" class="w-full h-1.5 bg-indigo-100 rounded-lg appearance-none cursor-pointer accent-indigo-600">
          </div>
          <div class="space-y-2">
            <div class="flex justify-between text-xs text-slate-600 font-medium">
              <span>教育背景 (Education)</span>
              <span class="text-purple-600">{{ weights.educationWeight }}%</span>
            </div>
            <input type="range" v-model.number="weights.educationWeight" min="0" max="100" class="w-full h-1.5 bg-purple-100 rounded-lg appearance-none cursor-pointer accent-purple-600">
          </div>
          <div class="space-y-2">
            <div class="flex justify-between text-xs text-slate-600 font-medium">
              <span>非结构化语义深度 (pgvector)</span>
              <span class="text-green-600">{{ weights.semanticWeight }}%</span>
            </div>
            <input type="range" v-model.number="weights.semanticWeight" min="0" max="100" class="w-full h-1.5 bg-green-100 rounded-lg appearance-none cursor-pointer accent-green-600">
          </div>
        </div>
      </div>
    </div>

    <!-- Recommendations List -->
    <div class="flex-1 overflow-y-auto min-h-0 space-y-6 pb-6">
      <div v-if="pageFeedback" :class="pageFeedback.type === 'error' ? 'bg-rose-50 border-rose-200 text-rose-700' : 'bg-emerald-50 border-emerald-200 text-emerald-700'" class="rounded-2xl border px-5 py-4 text-sm shadow-sm">
        {{ pageFeedback.message }}
      </div>

      <div v-if="errorMsg" class="bg-rose-50 border border-rose-200 rounded-2xl p-6 text-center text-rose-700">
        <p class="font-semibold">岗位详情或推荐结果加载失败</p>
        <p class="text-sm mt-2">{{ errorMsg }}</p>
        <button @click="reloadPageData" class="mt-4 px-4 py-2 rounded-lg bg-rose-600 text-white hover:bg-rose-700 transition">重试</button>
      </div>

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

      <section class="rounded-[28px] border border-white/70 bg-[linear-gradient(180deg,rgba(255,255,255,0.95),rgba(241,245,249,0.94))] p-6 shadow-sm">
        <div class="flex flex-col gap-3 border-b border-slate-200 pb-4 md:flex-row md:items-end md:justify-between">
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.24em] text-emerald-500">Application Review</p>
            <h2 class="mt-2 text-xl font-bold text-slate-900">申请审核台</h2>
            <p class="mt-2 text-sm text-slate-500">这里展示当前岗位的有效投递，便于 HR 从 AI 推荐过渡到实际处理。已撤回记录不会出现在审核台里。</p>
          </div>
          <div class="inline-flex items-center gap-2 rounded-full border border-emerald-100 bg-emerald-50 px-3 py-1.5 text-xs font-semibold text-emerald-700">
            <i class="fas fa-inbox"></i>
            当前有效投递 {{ applications.length }} 份
          </div>
        </div>

        <div v-if="applicationLoading" class="py-10 text-center text-slate-500">
          <i class="fas fa-circle-notch fa-spin text-3xl text-emerald-500"></i>
          <p class="mt-3">正在加载岗位投递记录...</p>
        </div>

        <div v-else-if="applicationError" class="mt-5 rounded-2xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">
          <p class="font-semibold">投递记录加载失败</p>
          <p class="mt-1">{{ applicationError }}</p>
          <button @click="fetchApplications" class="mt-3 rounded-xl bg-rose-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-rose-700">重试</button>
        </div>

        <div v-else-if="applications.length === 0" class="mt-5 rounded-2xl border border-dashed border-slate-200 bg-white/70 px-6 py-10 text-center text-slate-500">
          <div class="text-4xl">📬</div>
          <p class="mt-3 text-base font-semibold text-slate-700">当前还没有候选人投递这个岗位</p>
          <p class="mt-2 text-sm">你可以先发起 AI 评估并让候选人从推荐岗位详情进入投递，之后这里会聚合有效申请。</p>
        </div>

        <div v-else class="mt-5 space-y-4">
          <article v-for="application in applications" :key="application.applicationId" class="rounded-3xl border border-slate-200 bg-white/90 p-5 shadow-sm">
            <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
              <div class="min-w-0 flex-1">
                <div class="flex flex-wrap items-center gap-3">
                  <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-emerald-100 to-cyan-100 text-lg font-bold text-emerald-700">
                    {{ getApplicationDisplayName(application).charAt(0) }}
                  </div>
                  <div class="min-w-0">
                    <h3 class="truncate text-lg font-bold text-slate-900">{{ getApplicationDisplayName(application) }}</h3>
                    <p class="truncate text-sm text-slate-500">{{ application.candidate.username }} · {{ application.candidate.email }}</p>
                  </div>
                </div>

                <div class="mt-4 flex flex-wrap gap-2">
                  <span :class="getApplicationStatusClass(application.status)" class="rounded-full border px-3 py-1 text-xs font-semibold">
                    {{ getApplicationStatusLabel(application.status) }}
                  </span>
                  <span v-if="application.latestResume" class="rounded-full border border-slate-200 bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700">
                    简历 {{ getResumeStatusLabel(application.latestResume.status) }}
                  </span>
                  <span v-if="getApplicationRecommendation(application)" class="rounded-full border border-emerald-100 bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-700">
                    AI 匹配 {{ getApplicationRecommendation(application)?.matchScore }}/100
                  </span>
                </div>

                <div class="mt-4 grid gap-3 text-sm text-slate-600 md:grid-cols-2">
                  <div class="rounded-2xl bg-slate-50 px-4 py-3">
                    <p class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">投递时间</p>
                    <p class="mt-1 font-medium text-slate-800">{{ formatDateTime(application.appliedAt) }}</p>
                  </div>
                  <div class="rounded-2xl bg-slate-50 px-4 py-3">
                    <p class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">最近更新</p>
                    <p class="mt-1 font-medium text-slate-800">{{ formatDateTime(application.updatedAt) }}</p>
                  </div>
                  <div v-if="application.latestResume" class="rounded-2xl bg-slate-50 px-4 py-3 md:col-span-2">
                    <p class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">最近简历</p>
                    <p class="mt-1 font-medium text-slate-800">{{ application.latestResume.candidateName || getApplicationDisplayName(application) }}</p>
                    <p class="mt-1 text-xs text-slate-500">联系方式：{{ application.latestResume.contactInfo || application.candidate.email }} · 更新于 {{ formatDateTime(application.latestResume.updatedAt) }}</p>
                  </div>
                </div>

                <div v-if="getApplicationRecommendation(application)?.xaiReasoning" class="mt-4 rounded-2xl border border-emerald-100 bg-emerald-50/70 px-4 py-3 text-sm text-emerald-900">
                  <p class="font-semibold">AI 当前给出的匹配理由</p>
                  <p class="mt-1 line-clamp-3">{{ getApplicationRecommendation(application)?.xaiReasoning }}</p>
                </div>

                <div v-if="application.reviewNote" class="mt-4 rounded-2xl border border-amber-100 bg-amber-50/80 px-4 py-3 text-sm text-amber-900">
                  <p class="font-semibold">当前审核备注</p>
                  <p class="mt-1 whitespace-pre-line">{{ application.reviewNote }}</p>
                </div>
              </div>

              <div class="flex shrink-0 flex-col gap-3 lg:w-72">
                <button
                  v-if="application.latestResume?.resumeId"
                  @click="openResumeDetail(application.latestResume.resumeId)"
                  class="rounded-2xl bg-slate-900 px-4 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
                >
                  查看简历详情
                </button>
                <div v-else class="rounded-2xl border border-dashed border-slate-200 px-4 py-3 text-sm text-slate-400">
                  候选人尚未上传可查看的简历
                </div>
                <div class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
                  <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">审核动作</p>
                  <label class="mt-3 block space-y-2">
                    <span class="text-sm font-medium text-slate-700">处理状态</span>
                    <select v-model="reviewDrafts[String(application.applicationId)].status" class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-700 outline-none focus:border-emerald-400 focus:ring-4 focus:ring-emerald-100">
                      <option value="APPLIED">待处理</option>
                      <option value="INTERVIEW">约面</option>
                      <option value="REJECTED">淘汰</option>
                    </select>
                  </label>
                  <label class="mt-3 block space-y-2">
                    <span class="text-sm font-medium text-slate-700">审核备注</span>
                    <textarea v-model="reviewDrafts[String(application.applicationId)].reviewNote" rows="4" placeholder="记录筛选结论、面试安排或淘汰原因" class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-700 outline-none focus:border-emerald-400 focus:ring-4 focus:ring-emerald-100"></textarea>
                  </label>
                  <p v-if="reviewDrafts[String(application.applicationId)].error" class="mt-3 text-xs text-rose-600">
                    {{ reviewDrafts[String(application.applicationId)].error }}
                  </p>
                  <button
                    @click="submitApplicationReview(application)"
                    :disabled="reviewDrafts[String(application.applicationId)].saving"
                    class="mt-3 w-full rounded-xl bg-emerald-600 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-70"
                  >
                    {{ reviewDrafts[String(application.applicationId)].saving ? '保存中...' : '保存审核动作' }}
                  </button>
                </div>
                <p class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-xs leading-5 text-slate-500">
                  当前审核台已经覆盖推荐查看、投递处理和审核备注三类核心动作，可直接用于 HR 主路径演示。
                </p>
              </div>
            </div>
          </article>
        </div>
      </section>
    </div>

    <div v-if="editPanelOpen" class="fixed inset-0 z-40 flex justify-end bg-slate-950/30 backdrop-blur-sm">
      <div class="h-full w-full max-w-2xl overflow-y-auto border-l border-white/50 bg-[linear-gradient(180deg,rgba(255,255,255,0.98),rgba(244,247,251,0.98))] p-6 shadow-2xl">
        <div class="mb-6 flex items-start justify-between gap-4">
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.25em] text-blue-500">HR Workspace</p>
            <h2 class="mt-2 text-2xl font-bold text-slate-900">编辑岗位</h2>
            <p class="mt-2 text-sm text-slate-500">更新岗位要求后会重新生成岗位向量，建议保存后重新执行一次 AI 评估。</p>
          </div>
          <button @click="closeEditPanel" class="h-10 w-10 rounded-full border border-slate-200 bg-white text-slate-500 transition hover:border-slate-300 hover:text-slate-700">
            <i class="fas fa-times"></i>
          </button>
        </div>

        <div v-if="editError" class="mb-5 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {{ editError }}
        </div>

        <form class="space-y-6" @submit.prevent="submitJobUpdate">
          <section class="rounded-2xl border border-white/60 bg-white/80 p-5 shadow-sm">
            <h3 class="mb-4 text-sm font-semibold text-slate-800">岗位主信息</h3>
            <div class="grid grid-cols-1 gap-4">
              <label class="space-y-2">
                <span class="text-sm font-medium text-slate-700">岗位名称</span>
                <input v-model="editForm.title" type="text" maxlength="255" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100">
              </label>
              <label class="space-y-2">
                <span class="text-sm font-medium text-slate-700">岗位描述</span>
                <textarea v-model="editForm.description" rows="6" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100"></textarea>
              </label>
            </div>
          </section>

          <section class="rounded-2xl border border-white/60 bg-white/80 p-5 shadow-sm">
            <h3 class="mb-4 text-sm font-semibold text-slate-800">结构化要求</h3>
            <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
              <label class="space-y-2">
                <span class="text-sm font-medium text-slate-700">部门</span>
                <input v-model="editForm.department" type="text" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100">
              </label>
              <label class="space-y-2">
                <span class="text-sm font-medium text-slate-700">工作地点</span>
                <input v-model="editForm.location" type="text" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100">
              </label>
              <label class="space-y-2">
                <span class="text-sm font-medium text-slate-700">招聘人数</span>
                <input v-model="editForm.headcount" type="number" min="1" max="999" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100">
              </label>
              <label class="space-y-2">
                <span class="text-sm font-medium text-slate-700">职级</span>
                <input v-model="editForm.seniority" type="text" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100">
              </label>
              <label class="space-y-2">
                <span class="text-sm font-medium text-slate-700">用工形式</span>
                <select v-model="editForm.employmentType" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100">
                  <option value="">请选择</option>
                  <option value="全职">全职</option>
                  <option value="实习">实习</option>
                  <option value="校招">校招</option>
                  <option value="外包/合同">外包/合同</option>
                </select>
              </label>
              <label class="space-y-2">
                <span class="text-sm font-medium text-slate-700">学历要求</span>
                <input v-model="editForm.education" type="text" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100">
              </label>
              <label class="space-y-2 md:col-span-2">
                <span class="text-sm font-medium text-slate-700">薪资区间</span>
                <input v-model="editForm.salaryRange" type="text" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100">
              </label>
            </div>
          </section>

          <section class="space-y-4 rounded-2xl border border-white/60 bg-white/80 p-5 shadow-sm">
            <div>
              <h3 class="text-sm font-semibold text-slate-800">关键技能与亮点</h3>
              <p class="mt-1 text-xs text-slate-500">技能会参与向量更新和推荐评估，保存后建议重新发起 AI 评估。</p>
            </div>

            <div class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
              <div class="flex flex-col gap-3 sm:flex-row">
                <input
                  v-model="skillInput"
                  @keydown.enter.prevent="addSkillsFromInput(skillInput)"
                  type="text"
                  placeholder="输入技能后回车，支持逗号批量录入"
                  class="flex-1 rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100"
                >
                <button type="button" @click="addSkillsFromInput(skillInput)" class="rounded-xl bg-slate-900 px-4 py-3 text-sm font-medium text-white transition hover:bg-slate-800">
                  添加技能
                </button>
              </div>

              <div class="mt-4 flex min-h-[32px] flex-wrap gap-2">
                <button v-for="skill in editForm.skills" :key="skill" type="button" @click="removeSkill(skill)" class="inline-flex items-center gap-2 rounded-full border border-blue-100 bg-blue-50 px-3 py-1.5 text-xs font-medium text-blue-700 transition hover:bg-blue-100">
                  <span>{{ skill }}</span>
                  <i class="fas fa-times text-[10px]"></i>
                </button>
                <span v-if="editForm.skills.length === 0" class="py-2 text-xs text-slate-400">尚未添加技能标签</span>
              </div>
            </div>

            <label class="block space-y-2">
              <span class="text-sm font-medium text-slate-700">补充亮点</span>
              <textarea v-model="editForm.highlights" rows="4" class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100"></textarea>
            </label>
          </section>

          <div class="flex items-center justify-end gap-3 pt-2">
            <button type="button" @click="closeEditPanel" class="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-slate-700 transition hover:bg-slate-50">取消</button>
            <button type="submit" :disabled="savingJob" class="flex items-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 px-5 py-2.5 font-medium text-white shadow-md shadow-blue-500/20 transition hover:shadow-lg disabled:cursor-not-allowed disabled:opacity-70">
              <i :class="savingJob ? 'fas fa-spinner fa-spin' : 'fas fa-save'"></i>
              {{ savingJob ? '保存中...' : '保存岗位更新' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
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
const applications = ref<any[]>([])
const loading = ref(true)
const applicationLoading = ref(false)
const evaluating = ref(false)
const errorMsg = ref('')
const applicationError = ref('')
const pageFeedback = ref<{ type: 'success' | 'error'; message: string } | null>(null)
const editPanelOpen = ref(false)
const editError = ref('')
const savingJob = ref(false)
const skillInput = ref('')
const reviewDrafts = reactive<Record<string, { status: string; reviewNote: string; saving: boolean; error: string }>>({})

const editForm = reactive({
  title: '',
  description: '',
  department: '',
  location: '',
  headcount: '1',
  seniority: '',
  employmentType: '',
  education: '',
  salaryRange: '',
  skills: [] as string[],
  highlights: '',
})

const weights = ref({
  skillWeight: 40,
  experienceWeight: 30,
  educationWeight: 10,
  semanticWeight: 20
})

const recommendationByResumeId = computed(() => {
  const entries = recommendations.value
    .filter((rec) => typeof rec?.resumeId === 'string' && rec.resumeId)
    .map((rec) => [rec.resumeId, rec] as const)
  return new Map(entries)
})

const getJobMeta = (job: any) => {
  const requirements = job?.requirements || {}
  const department = requirements.department || (job?.createdBy?.username ? `发布者: ${job.createdBy.username}` : '未标注部门')
  const location = requirements.location || '地点未设置'
  const headcount = requirements.headcount ? `${requirements.headcount} 人` : '人数未设置'
  return `${department} • ${location} • ${headcount}`
}

const formatDateTime = (value?: string | null) => {
  if (!value) {
    return '未知时间'
  }

  return new Date(value).toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const getResumeStatusLabel = (status?: string | null) => {
  switch (status) {
    case 'PARSED':
      return '已解析'
    case 'PARSING':
      return '解析中'
    case 'PENDING_PARSE':
      return '待解析'
    case 'PARSE_FAILED':
      return '解析失败'
    default:
      return status || '未知状态'
  }
}

const getApplicationStatusLabel = (status?: string | null) => {
  switch (status) {
    case 'APPLIED':
      return '待处理投递'
    case 'INTERVIEW':
      return '已约面'
    case 'REJECTED':
      return '未通过'
    default:
      return status || '未知状态'
  }
}

const getApplicationStatusClass = (status?: string | null) => {
  switch (status) {
    case 'APPLIED':
      return 'border-blue-100 bg-blue-50 text-blue-700'
    case 'INTERVIEW':
      return 'border-emerald-100 bg-emerald-50 text-emerald-700'
    case 'REJECTED':
      return 'border-rose-100 bg-rose-50 text-rose-700'
    default:
      return 'border-slate-200 bg-slate-100 text-slate-700'
  }
}

const getApplicationDisplayName = (application: any) => {
  return application?.candidate?.displayName || application?.candidate?.username || '未命名候选人'
}

const getApplicationRecommendation = (application: any) => {
  const resumeId = application?.latestResume?.resumeId
  if (!resumeId) {
    return null
  }

  return recommendationByResumeId.value.get(resumeId) || null
}

const openResumeDetail = (resumeId: string) => {
  router.push({ name: 'resumeDetail', params: { id: resumeId } })
}

const syncReviewDrafts = (items: any[]) => {
  const nextIds = new Set(items.map((item) => String(item.applicationId)))
  for (const existingId of Object.keys(reviewDrafts)) {
    if (!nextIds.has(existingId)) {
      delete reviewDrafts[existingId]
    }
  }

  for (const item of items) {
    const applicationId = String(item.applicationId)
    reviewDrafts[applicationId] = {
      status: item.status || 'APPLIED',
      reviewNote: item.reviewNote || '',
      saving: reviewDrafts[applicationId]?.saving || false,
      error: '',
    }
  }
}

const submitApplicationReview = async (application: any) => {
  const applicationId = String(application.applicationId)
  const draft = reviewDrafts[applicationId]
  if (!draft) {
    return
  }

  try {
    draft.saving = true
    draft.error = ''
    pageFeedback.value = null
    const res: any = await api.put(`/jobs/${jobId}/applications/${applicationId}`, {
      status: draft.status,
      reviewNote: draft.reviewNote.trim() || null,
    })

    applications.value = applications.value.map((item) => (
      String(item.applicationId) === applicationId ? res.data : item
    ))
    syncReviewDrafts(applications.value)
    pageFeedback.value = { type: 'success', message: '候选人审核状态已更新。' }
  } catch (error: any) {
    console.error('Failed to review application', error)
    draft.error = error.response?.data?.message || '更新审核状态失败，请稍后重试。'
  } finally {
    draft.saving = false
  }
}

const hydrateEditForm = (jobData: any) => {
  const requirements = jobData?.requirements || {}
  editForm.title = jobData?.title || ''
  editForm.description = jobData?.description || ''
  editForm.department = requirements.department || ''
  editForm.location = requirements.location || ''
  editForm.headcount = requirements.headcount ? String(requirements.headcount) : '1'
  editForm.seniority = requirements.seniority || ''
  editForm.employmentType = requirements.employmentType || ''
  editForm.education = requirements.education || ''
  editForm.salaryRange = requirements.salaryRange || ''
  editForm.skills = Array.isArray(requirements.skills) ? requirements.skills.filter((skill: unknown) => typeof skill === 'string' && skill.trim()) : []
  editForm.highlights = Array.isArray(requirements.highlights) ? requirements.highlights.join('\n') : ''
  skillInput.value = ''
}

const openEditPanel = () => {
  if (!job.value) {
    return
  }
  hydrateEditForm(job.value)
  editError.value = ''
  editPanelOpen.value = true
}

const closeEditPanel = () => {
  editPanelOpen.value = false
  editError.value = ''
}

const normalizeSkillToken = (value: string) => value.trim().replace(/\s+/g, ' ')

const addSkillsFromInput = (rawValue: string) => {
  const tokens = rawValue
    .split(/[，,\n]/)
    .map(normalizeSkillToken)
    .filter(Boolean)

  if (tokens.length === 0) {
    return
  }

  const existing = new Set(editForm.skills.map((skill) => skill.toLowerCase()))
  for (const token of tokens) {
    if (!existing.has(token.toLowerCase())) {
      editForm.skills.push(token)
      existing.add(token.toLowerCase())
    }
  }

  skillInput.value = ''
}

const removeSkill = (skillToRemove: string) => {
  editForm.skills = editForm.skills.filter((skill) => skill !== skillToRemove)
}

const buildRequirementsPayload = () => {
  const requirements: Record<string, unknown> = {}

  if (editForm.department.trim()) requirements.department = editForm.department.trim()
  if (editForm.location.trim()) requirements.location = editForm.location.trim()

  const parsedHeadcount = Number(editForm.headcount)
  if (Number.isFinite(parsedHeadcount) && parsedHeadcount > 0) requirements.headcount = parsedHeadcount

  if (editForm.seniority.trim()) requirements.seniority = editForm.seniority.trim()
  if (editForm.employmentType.trim()) requirements.employmentType = editForm.employmentType.trim()
  if (editForm.education.trim()) requirements.education = editForm.education.trim()
  if (editForm.salaryRange.trim()) requirements.salaryRange = editForm.salaryRange.trim()
  if (editForm.skills.length > 0) requirements.skills = [...editForm.skills]

  const highlights = editForm.highlights
    .split(/\n+/)
    .map((item) => item.trim())
    .filter(Boolean)
  if (highlights.length > 0) requirements.highlights = highlights

  return requirements
}

const fetchJob = async () => {
  try {
    const res: any = await api.get(`/jobs/${jobId}`)
    job.value = res.data
  } catch (error) {
    console.error('Failed to fetch job', error)
    errorMsg.value = '无法读取岗位详情，请检查岗位是否存在。'
    job.value = null
  }
}

const submitJobUpdate = async () => {
  const title = editForm.title.trim()
  const description = editForm.description.trim()
  const requirements = buildRequirementsPayload()

  if (!title) {
    editError.value = '岗位名称不能为空。'
    return
  }
  if (!description) {
    editError.value = '岗位描述不能为空。'
    return
  }
  if (Object.keys(requirements).length === 0) {
    editError.value = '请至少保留一项结构化要求，例如地点、技能或招聘人数。'
    return
  }

  try {
    savingJob.value = true
    editError.value = ''
    pageFeedback.value = null
    const res: any = await api.put(`/jobs/${jobId}`, {
      title,
      description,
      requirements,
    })
    job.value = res.data
    closeEditPanel()
    pageFeedback.value = { type: 'success', message: '岗位信息已更新，建议重新执行一次 AI 评估以刷新推荐结果。' }
  } catch (error: any) {
    console.error('Failed to update job', error)
    editError.value = error.response?.data?.message || '岗位更新失败，请稍后重试。'
  } finally {
    savingJob.value = false
  }
}

const fetchRecommendations = async () => {
  try {
    loading.value = true
    errorMsg.value = ''
    const res: any = await api.get(`/jobs/${jobId}/recommendations`)
    recommendations.value = res.data || []
  } catch (error) {
    console.error('Failed to fetch recommendations', error)
    errorMsg.value = '无法读取推荐结果，请稍后重试。'
    recommendations.value = []
  } finally {
    loading.value = false
  }
}

const fetchApplications = async () => {
  try {
    applicationLoading.value = true
    applicationError.value = ''
    const res: any = await api.get(`/jobs/${jobId}/applications`)
    applications.value = res.data || []
    syncReviewDrafts(applications.value)
  } catch (error) {
    console.error('Failed to fetch job applications', error)
    applicationError.value = '无法读取当前岗位的投递记录，请稍后重试。'
    applications.value = []
  } finally {
    applicationLoading.value = false
  }
}

const evaluateJob = async () => {
  try {
    evaluating.value = true
    await api.post(`/jobs/${jobId}/evaluate`, weights.value)
    await fetchRecommendations()
  } catch (error) {
    console.error('Failed to evaluate job', error)
    errorMsg.value = '评估请求失败，请稍后重试。'
  } finally {
    evaluating.value = false
  }
}

const reloadPageData = async () => {
  errorMsg.value = ''
  applicationError.value = ''
  await Promise.all([fetchJob(), fetchRecommendations(), fetchApplications()])
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
  reloadPageData()
})
</script>
