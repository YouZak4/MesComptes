import { createRouter, createWebHistory } from 'vue-router'
import PageAccueil from '../views/pageAccueil.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'accueil',
      component: PageAccueil,
    },
  ],
})

export default router
