import { computed, onBeforeUnmount, ref, watch } from 'vue'

export function parseTimeControl(value = '5+0') {
  const [minutesRaw, incrementRaw] = String(value).split('+')
  const minutes = Number(minutesRaw || 5)
  const increment = Number(incrementRaw || 0)
  return {
    minutes,
    incrementSeconds: increment,
    initialMs: minutes * 60 * 1000,
  }
}

export function formatClock(ms) {
  const totalSeconds = Math.max(0, Math.ceil(ms / 1000))
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${minutes}:${String(seconds).padStart(2, '0')}`
}

export function useChessClock(options) {
  const { activeSide, version, status, timeControl, whiteMs: externalWhiteMs, blackMs: externalBlackMs } = options
  const config = computed(() => parseTimeControl(timeControl.value))
  const authoritative = computed(() => Number.isFinite(externalWhiteMs?.value) && Number.isFinite(externalBlackMs?.value))
  const whiteMs = ref(authoritative.value ? externalWhiteMs.value : config.value.initialMs)
  const blackMs = ref(authoritative.value ? externalBlackMs.value : config.value.initialMs)
  const runningSide = ref(activeSide.value)
  const lastTickAt = ref(Date.now())
  let intervalId = null

  function reset() {
    whiteMs.value = config.value.initialMs
    blackMs.value = config.value.initialMs
    runningSide.value = activeSide.value
    lastTickAt.value = Date.now()
  }

  function applyElapsed() {
    const now = Date.now()
    const elapsed = now - lastTickAt.value
    lastTickAt.value = now
    if (status.value !== 'ACTIVE') {
      return
    }
    if (runningSide.value === 'WHITE') {
      whiteMs.value = Math.max(0, whiteMs.value - elapsed)
    } else {
      blackMs.value = Math.max(0, blackMs.value - elapsed)
    }
  }

  function startInterval() {
    stopInterval()
    intervalId = window.setInterval(() => {
      applyElapsed()
    }, 250)
  }

  function stopInterval() {
    if (intervalId) {
      window.clearInterval(intervalId)
      intervalId = null
    }
  }

  watch(config, () => {
    if (!authoritative.value) {
      reset()
    }
  })

  watch(() => version?.value, (next, previous) => {
    if (authoritative.value) {
      return
    }
    if (previous == null || next === previous) {
      return
    }
    applyElapsed()
    const previousSide = runningSide.value
    if (previousSide === 'WHITE') {
      whiteMs.value += config.value.incrementSeconds * 1000
    } else {
      blackMs.value += config.value.incrementSeconds * 1000
    }
    runningSide.value = activeSide.value
    lastTickAt.value = Date.now()
  })

  watch([externalWhiteMs || ref(null), externalBlackMs || ref(null), activeSide, status], ([nextWhite, nextBlack]) => {
    if (!authoritative.value) {
      return
    }
    whiteMs.value = nextWhite
    blackMs.value = nextBlack
    runningSide.value = activeSide.value
    lastTickAt.value = Date.now()
    if (status.value === 'ACTIVE') {
      startInterval()
    } else {
      stopInterval()
    }
  }, { immediate: true })

  watch([activeSide, status], () => {
    runningSide.value = activeSide.value
    lastTickAt.value = Date.now()
    if (status.value === 'ACTIVE') {
      startInterval()
    } else {
      stopInterval()
    }
  }, { immediate: true })

  onBeforeUnmount(() => {
    stopInterval()
  })

  return {
    whiteMs,
    blackMs,
    whiteLabel: computed(() => formatClock(whiteMs.value)),
    blackLabel: computed(() => formatClock(blackMs.value)),
    reset,
  }
}
