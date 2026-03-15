<template lang="pug">
button.button.button--secondary.refresh-button(
  v-if='canRefreshCache'
  type='button'
  :disabled='pwa.state.refreshingCache'
  @click='refresh'
)
  span(v-if='pwa.state.refreshingCache') Actualizando...
  span(v-else) Actualizar app
</template>

<script setup>
import { refreshPwaCache, usePwaState } from '../lib/pwa'

const pwa = usePwaState()
const { canRefreshCache } = pwa

async function refresh() {
  await refreshPwaCache()
}
</script>

<style lang="scss" scoped>
.refresh-button {
  white-space: nowrap;
}
</style>
