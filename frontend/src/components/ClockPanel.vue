<template lang="pug">
.glass-card.panel.clock-panel
  h2.panel__title Reloj
  .clock-panel__grid
    .clock-panel__side(:class='{ "clock-panel__side--active": activeSide === "WHITE" }')
      span.clock-panel__label Blancas
      strong.clock-panel__time {{ whiteLabel }}
    .clock-panel__side(:class='{ "clock-panel__side--active": activeSide === "BLACK" }')
      span.clock-panel__label Negras
      strong.clock-panel__time {{ blackLabel }}
  p.clock-panel__meta(v-if='timeControl') Control: {{ timeControl }}
</template>

<script setup>
defineProps({
  whiteLabel: {
    type: String,
    required: true,
  },
  blackLabel: {
    type: String,
    required: true,
  },
  activeSide: {
    type: String,
    required: true,
  },
  timeControl: {
    type: String,
    default: '',
  },
})
</script>

<style lang="scss" scoped>
.clock-panel__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
}

.clock-panel__side {
  display: grid;
  gap: 0.35rem;
  padding: 0.95rem;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.04);

  &--active {
    border-color: rgba(255, 138, 102, 0.3);
    background: rgba(255, 138, 102, 0.12);
  }
}

.clock-panel__label,
.clock-panel__meta {
  color: var(--muted);
}

.clock-panel__time {
  font-size: clamp(1.2rem, 2vw, 1.7rem);
}

.clock-panel__meta {
  margin: 0.9rem 0 0;
}
</style>
