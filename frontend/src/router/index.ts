import { createRouter, createWebHistory } from 'vue-router'
import ATSLayout from '../layouts/ATSLayout.vue'
import { useAuthStore } from '../stores/auth'

const getHomeRouteName = (role?: string | null) => {
  if (role === 'CANDIDATE') return 'candidateDashboard'
  if (role === 'ADMIN') return 'adminConsole'
  return 'dashboard'
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/RegisterView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/',
      component: ATSLayout,
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'dashboard',
          component: () => import('../views/DashboardView.vue'),
          meta: { roles: ['HR', 'ADMIN'] },
        },
        {
          path: 'jobs',
          name: 'jobs',
          component: () => import('../views/JobsView.vue'),
          meta: { roles: ['HR', 'ADMIN'] },
        },
        {
          path: 'jobs/:id',
          name: 'jobDetail',
          component: () => import('../views/JobDetailView.vue'),
          meta: { roles: ['HR', 'ADMIN'] },
        },
        {
          path: 'resumes',
          name: 'resumes',
          component: () => import('../views/ResumeListView.vue'),
          meta: { roles: ['HR', 'ADMIN'] },
        },
        {
          path: 'resumes/:id',
          name: 'resumeDetail',
          component: () => import('../views/ResumeDetailView.vue'),
          meta: { roles: ['HR', 'ADMIN'] },
        },
        {
          path: 'admin/console',
          name: 'adminConsole',
          component: () => import('../views/AdminConsoleView.vue'),
          meta: { roles: ['ADMIN'] },
        },
        {
          path: 'candidate/dashboard',
          name: 'candidateDashboard',
          component: () => import('../views/CandidateDashboardView.vue'),
          meta: { roles: ['CANDIDATE'] },
        },
        {
          path: 'candidate/jobs/:id',
          name: 'candidateJobDetail',
          component: () => import('../views/CandidateJobDetailView.vue'),
          meta: { roles: ['CANDIDATE'] },
        },
        {
          path: 'candidate/profile',
          name: 'candidateProfile',
          component: () => import('../views/CandidateProfileView.vue'),
          meta: { roles: ['CANDIDATE'] },
        },
        {
          path: 'candidate/applications',
          name: 'candidateApplications',
          component: () => import('../views/CandidateApplicationsView.vue'),
          meta: { roles: ['CANDIDATE'] },
        }
      ]
    }
  ],
})

router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore()

  if (authStore.token && !authStore.user) {
    await authStore.fetchCurrentUser()
  }
  
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next('/login')
    return
  }

  if ((to.name === 'login' || to.name === 'register') && authStore.isAuthenticated) {
    next({ name: getHomeRouteName(authStore.user?.role) })
    return
  }

  if (to.path === '/' && authStore.isAuthenticated && authStore.user?.role === 'CANDIDATE') {
    next({ name: 'candidateDashboard' })
    return
  }

  const allowedRoles = to.meta.roles as string[] | undefined
  if (allowedRoles && authStore.user && !allowedRoles.includes(authStore.user.role)) {
    next({ name: getHomeRouteName(authStore.user.role) })
    return
  }

  next()
})

export default router
