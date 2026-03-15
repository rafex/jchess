import { computed, reactive } from 'vue'

const state = reactive({
  installPrompt: null,
  canInstall: false,
  isInstalled: window.matchMedia('(display-mode: standalone)').matches,
  isOffline: !navigator.onLine,
})

export function registerPwa() {
  window.addEventListener('beforeinstallprompt', (event) => {
    event.preventDefault()
    state.installPrompt = event
    state.canInstall = true
  })

  window.addEventListener('appinstalled', () => {
    state.installPrompt = null
    state.canInstall = false
    state.isInstalled = true
  })

  window.addEventListener('online', () => {
    state.isOffline = false
  })

  window.addEventListener('offline', () => {
    state.isOffline = true
  })

  if ('serviceWorker' in navigator) {
    window.addEventListener('load', async () => {
      await navigator.serviceWorker.register('/sw.js')
    })
  }
}

export function usePwaState() {
  return {
    state,
    canInstall: computed(() => state.canInstall && !state.isInstalled),
  }
}

export async function promptPwaInstall() {
  if (!state.installPrompt) {
    return false
  }

  state.installPrompt.prompt()
  const result = await state.installPrompt.userChoice
  if (result.outcome !== 'accepted') {
    return false
  }

  state.installPrompt = null
  state.canInstall = false
  return true
}
