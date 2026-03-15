import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import GameView from '../views/GameView.vue'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior() {
    return { top: 0 }
  },
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: {
        title: 'JChess | Nueva partida',
      },
    },
    {
      path: '/game/:sessionId',
      name: 'game',
      component: GameView,
      props: true,
      meta: {
        title: 'JChess | Partida',
      },
    },
  ],
})

router.afterEach((to) => {
  document.title = to.meta.title || 'JChess'
})

export default router
