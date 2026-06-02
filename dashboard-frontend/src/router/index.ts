import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: () => import('../views/DashboardView.vue')
    },
    {
      path: '/review/:id',
      name: 'review-detail',
      component: () => import('../views/ReviewDetailView.vue')
    }
  ]
})

export default router
