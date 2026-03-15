<template lang="pug">
.app-shell
  header.app-header.glass-card
    .brand
      .brand__eyebrow Motor + tablero vivo
      .brand__mark JChess
      .brand__copy by rafex
    nav.app-nav
      RouterLink.app-nav__link(to='/') Inicio
      InstallPwaButton
      .pill.pill--status(v-if='pwa.state.isOffline') Sin conexión
  main.app-main
    RouterView
</template>

<script setup>
import InstallPwaButton from './components/InstallPwaButton.vue'
import { usePwaState } from './lib/pwa'

const pwa = usePwaState()
</script>

<style lang="scss" scoped>
.app-shell {
  min-height: 100vh;
}

.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 1rem 1.25rem 0;
  padding: 1rem 1.25rem;
  position: sticky;
  top: 0.75rem;
  z-index: 10;
}

.brand {
  display: grid;
  gap: 0.16rem;

  &__eyebrow {
    color: var(--secondary);
    font-size: 0.74rem;
    font-weight: 700;
    letter-spacing: 0.12em;
    text-transform: uppercase;
  }

  &__mark {
    font-size: 1.55rem;
    font-weight: 700;
    letter-spacing: -0.03em;
  }

  &__copy {
    font-size: 0.84rem;
    color: var(--muted);
  }
}

.app-nav {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  flex-wrap: wrap;

  &__link {
    color: inherit;
    text-decoration: none;
    font-weight: 700;
    padding: 0.75rem 1rem;
    border-radius: 999px;
    transition: background 0.18s ease, color 0.18s ease;

    &:hover {
      background: rgba(255, 255, 255, 0.06);
    }

    &.router-link-active {
      background: rgba(255, 255, 255, 0.08);
      color: var(--primary);
    }
  }
}

.app-main {
  padding: 1rem 1.5rem 2rem;
}

@media (max-width: 760px) {
  .app-header {
    margin: 0.75rem 0.75rem 0;
    padding: 1rem;
    position: static;
  }

  .app-main {
    padding: 1rem 0.75rem 2rem;
  }
}
</style>
