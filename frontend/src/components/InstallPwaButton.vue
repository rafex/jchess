<template lang="pug">
button.button.button--tonal.install-button(
  v-if='canInstall'
  type='button'
  @click='install'
)
  span Instalar app
</template>

<script setup>
import { usePwaState, promptPwaInstall } from '../lib/pwa'

const { canInstall } = usePwaState()

async function install() {
  await promptPwaInstall()
}
</script>

<style lang="scss" scoped>
.install-button {
  white-space: nowrap;
}
</style>
