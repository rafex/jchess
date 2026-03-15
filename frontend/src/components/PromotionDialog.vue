<template lang="pug">
.promotion-overlay(v-if='open')
  .promotion-dialog.glass-card
    .promotion-dialog__header
      h3 Elige promoción
      p {{ subtitle }}
    .promotion-dialog__choices
      button.promotion-dialog__choice(
        v-for='option in options'
        :key='option.code'
        type='button'
        @click='$emit("select", option.code)'
      )
        img.promotion-dialog__piece(:src='option.asset' :alt='option.label')
        span {{ option.label }}
    button.button.button--secondary(type='button' @click='$emit("cancel")') Cancelar
</template>

<script setup>
defineProps({
  open: {
    type: Boolean,
    default: false,
  },
  subtitle: {
    type: String,
    default: '',
  },
  options: {
    type: Array,
    default: () => [],
  },
})

defineEmits(['select', 'cancel'])
</script>

<style lang="scss" scoped>
.promotion-overlay {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 1rem;
  background: rgba(7, 11, 15, 0.48);
  backdrop-filter: blur(6px);
  z-index: 4;
}

.promotion-dialog {
  width: min(28rem, 100%);
  padding: 1.25rem;
  display: grid;
  gap: 1rem;
}

.promotion-dialog__header {
  h3,
  p {
    margin: 0;
  }

  p {
    margin-top: 0.35rem;
    color: var(--muted);
  }
}

.promotion-dialog__choices {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
}

.promotion-dialog__choice {
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.05);
  color: var(--text);
  border-radius: var(--radius-md);
  padding: 0.9rem;
  display: flex;
  align-items: center;
  gap: 0.8rem;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, background 0.18s ease;

  &:hover {
    transform: translateY(-1px);
    border-color: rgba(255, 138, 102, 0.35);
    background: rgba(255, 138, 102, 0.09);
  }
}

.promotion-dialog__piece {
  width: 2.4rem;
  height: 2.4rem;
  object-fit: contain;
}
</style>
