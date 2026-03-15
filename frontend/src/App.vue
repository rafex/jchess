<template lang="pug">
.app-shell
  header.app-header
    .brand
      .brand__mark JChess
      .brand__copy by rafex
    nav.app-nav
      RouterLink.app-nav__link(to='/') Inicio
  main.app-main
    RouterView
</template>

<script setup>
</script>

<style lang="scss" scoped>
.app-shell {
  min-height: 100vh;
}

.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25rem 1.75rem;
}

.brand {
  display: grid;
  gap: 0.1rem;

  &__mark {
    font-size: 1.5rem;
    font-weight: 800;
    letter-spacing: 0.08em;
    text-transform: uppercase;
  }

  &__copy {
    font-size: 0.82rem;
    color: rgba(244, 236, 220, 0.7);
  }
}

.app-nav {
  display: flex;
  gap: 1rem;

  &__link {
    color: inherit;
    text-decoration: none;
    font-weight: 600;

    &.router-link-active {
      color: var(--accent);
    }
  }
}

.app-main {
  padding: 0 1.5rem 2rem;
}
</style>
