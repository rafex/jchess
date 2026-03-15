import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './styles/main.scss'
import 'animation.css/main.css'
import { registerPwa } from './lib/pwa'

registerPwa()
createApp(App).use(router).mount('#app')
