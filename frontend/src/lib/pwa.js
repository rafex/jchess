import { computed, reactive } from 'vue'

const state = reactive({
  installPrompt: null,
  canInstall: false,
  isInstalled: window.matchMedia('(display-mode: standalone)').matches,
  isOffline: !navigator.onLine,
  refreshingCache: false,
})

export function registerPwa() {
  if (!('serviceWorker' in navigator)) {
    return
  }

  if (!import.meta.env.PROD) {
    navigator.serviceWorker.getRegistrations().then((registrations) => {
      registrations.forEach((registration) => registration.unregister())
    })
    return
  }

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

  window.addEventListener('load', async () => {
    await navigator.serviceWorker.register('/sw.js')
  })
}

export function usePwaState() {
  return {
    state,
    canInstall: computed(() => state.canInstall && !state.isInstalled),
    canRefreshCache: computed(() => 'serviceWorker' in navigator && 'caches' in window),
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

export async function refreshPwaCache() {
  if (!('serviceWorker' in navigator) || !('caches' in window)) {
    window.location.reload()
    return
  }

  state.refreshingCache = true

  try {
    const registrations = await navigator.serviceWorker.getRegistrations()
    await Promise.all(registrations.map(async (registration) => {
      await registration.update().catch(() => {})
      if (!import.meta.env.PROD) {
        await registration.unregister().catch(() => {})
      }
    }))

    const cacheKeys = await caches.keys()
    await Promise.all(cacheKeys.map((key) => caches.delete(key)))
  } finally {
    window.location.reload()
  }
}
