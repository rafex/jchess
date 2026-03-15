<template lang="pug">
.loader(:class='loaderClasses')
  .loader__stage
    .loop.cubes
      .item.cubes(v-for='cube in cubes' :key='cube')
  p.loader__label(v-if='label') {{ label }}
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: {
    type: String,
    default: '',
  },
  compact: {
    type: Boolean,
    default: false,
  },
})

const cubes = [1, 2, 3, 4, 5, 6]

const loaderClasses = computed(() => ({
  'loader--compact': props.compact,
}))
</script>

<style lang="scss" scoped>
.loader {
  display: grid;
  justify-items: center;
  gap: 1.25rem;
  padding: 2rem 1rem;

  &--compact {
    gap: 0.75rem;
    padding: 0.25rem 0;

    .loader__stage {
      width: 10rem;
      height: 10rem;
    }

    .loader__label {
      font-size: 0.92rem;
    }

    .cubes {
      font-size: 0.72rem;
    }
  }

  &__stage {
    position: relative;
    width: 14rem;
    height: 14rem;
    perspective: 900px;
  }

  &__label {
    margin: 0;
    color: var(--muted);
    font-weight: 600;
    letter-spacing: 0.04em;
  }
}

.cubes {
  position: absolute;
  top: 50%;
  left: 50%;
  transform-style: preserve-3d;
}

.loop {
  transform: rotateX(-35deg) rotateY(-45deg) translateZ(1.5625em);
}

@keyframes loaderScale {
  to {
    transform: scale3d(0.2, 0.2, 0.2);
  }
}

.item {
  margin: -1.5625em;
  width: 3.125em;
  height: 3.125em;
  transform-origin: 50% 50% -1.5625em;
  box-shadow: 0 0 0.125em currentColor;
  background: currentColor;
  animation: loaderScale 0.6s cubic-bezier(0.45, 0.03, 0.51, 0.95) infinite alternate;
}

.item::before,
.item::after {
  position: absolute;
  width: inherit;
  height: inherit;
  transform-origin: 0 100%;
  box-shadow: inherit;
  background: currentColor;
  content: '';
}

.item::before {
  bottom: 100%;
  transform: rotateX(90deg);
}

.item::after {
  left: 100%;
  transform: rotateY(90deg);
}

.item:nth-child(1) {
  margin-top: 6.25em;
  color: #fe1e52;
  animation-delay: -1.2s;
}

.item:nth-child(1)::before {
  color: #ff6488;
}

.item:nth-child(1)::after {
  color: #ff416d;
}

.item:nth-child(2) {
  margin-top: 3.125em;
  color: #fe4252;
  animation-delay: -1s;
}

.item:nth-child(2)::before {
  color: #ff8892;
}

.item:nth-child(2)::after {
  color: #ff6572;
}

.item:nth-child(3) {
  margin-top: 0;
  color: #fe6553;
  animation-delay: -0.8s;
}

.item:nth-child(3)::before {
  color: #ffa499;
}

.item:nth-child(3)::after {
  color: #ff8476;
}

.item:nth-child(4) {
  margin-top: -3.125em;
  color: #fe8953;
  animation-delay: -0.6s;
}

.item:nth-child(4)::before {
  color: #ffb999;
}

.item:nth-child(4)::after {
  color: #ffa176;
}

.item:nth-child(5) {
  margin-top: -6.25em;
  color: #feac54;
  animation-delay: -0.4s;
}

.item:nth-child(5)::before {
  color: #ffce9a;
}

.item:nth-child(5)::after {
  color: #ffbd77;
}

.item:nth-child(6) {
  margin-top: -9.375em;
  color: #fed054;
  animation-delay: -0.2s;
}

.item:nth-child(6)::before {
  color: #ffe49a;
}

.item:nth-child(6)::after {
  color: #ffda77;
}
</style>
