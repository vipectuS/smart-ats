import { createRouter, createWebHistory } from 'vue-router'
import ATSLayout from '../layouts/ATSLayout.vue'
import { useAuthStore } from '../stores/auth'

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
        },
        {
          path: 'jobs',
          name: 'jobs',
          component: () => import('../views/JobsView.vue'),
        },
        {
          path: 'jobs/:id',
          name: 'jobDetail',
          component: () => import('../views/JobDetailView.vue'),
        },
        {
          path: 'resumes',
          name: 'resumes',
          component: () => import('../views/ResumeListView.vue'),
        },
        {
          path: 'candidate/dashboard',
          name: 'candidateDashboard',
          component: () => import('../views/CandidateDashboardView.vue'),
        }
      ]
    }
  ],
})

router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()
  
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next('/login')
  } else if (to.name === 'login' && authStore.isAuthenticated) {
    next('/')
  } else {
    next()
  }
})

export default router
