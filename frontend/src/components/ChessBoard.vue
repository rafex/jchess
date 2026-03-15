<template lang="pug">
.board-frame.glass-card
  .board-toolbar
    .pill {{ orientationLabel }}
    .pill(v-if='selectedSquare') Seleccionada {{ selectedSquare }}
    .pill(v-if='busy && busyLabel') {{ busyLabel }}
  .board-overlay(v-if='busy')
    span.board-overlay__label {{ busyLabel || 'Procesando...' }}
  .board-grid
    button.board-square(
      v-for='square in orientedSquares'
      :key='square.key'
      type='button'
      :class='squareClasses(square)'
      :disabled='!interactive || busy'
      @click='selectSquare(square)'
    )
      span.board-square__label {{ square.square }}
      img.board-square__piece(
        v-if='square.pieceAsset'
        :class='pieceClasses(square)'
        :src='square.pieceAsset'
        :alt='square.pieceAlt'
        loading='lazy'
        decoding='async'
        draggable='false'
      )
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { legalMovesForSquare, orientSquares, parseFenBoard, squareColor } from '../lib/chess'

const props = defineProps({
  fen: {
    type: String,
    required: true,
  },
  legalMovesUci: {
    type: Array,
    default: () => [],
  },
  perspective: {
    type: String,
    default: 'WHITE',
  },
  activeSide: {
    type: String,
    default: 'WHITE',
  },
  interactive: {
    type: Boolean,
    default: true,
  },
  busy: {
    type: Boolean,
    default: false,
  },
  busyLabel: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['move-intent'])

const selectedSquare = ref('')

const squares = computed(() => parseFenBoard(props.fen))
const orientedSquares = computed(() => orientSquares(squares.value, props.perspective))
const activeMoves = computed(() => legalMovesForSquare(props.legalMovesUci, selectedSquare.value))
const targetSquares = computed(() => new Set(activeMoves.value.map((move) => move.slice(2, 4))))
const orientationLabel = computed(() => `Vista ${props.perspective === 'WHITE' ? 'blancas' : 'negras'}`)

watch(() => props.fen, () => {
  selectedSquare.value = ''
})

function selectSquare(square) {
  if (!props.interactive || props.busy) {
    return
  }

  if (selectedSquare.value && targetSquares.value.has(square.square)) {
    const candidates = activeMoves.value.filter((candidate) => candidate.slice(2, 4) === square.square)
    if (!candidates.length) {
      selectedSquare.value = ''
      return
    }

    if (candidates.length > 1) {
      emit('move-intent', {
        from: selectedSquare.value,
        to: square.square,
        promotion: null,
        promotionOptions: candidates.map((move) => move.slice(4)).filter(Boolean),
      })
      selectedSquare.value = ''
      return
    }

    const move = candidates[0]
    emit('move-intent', {
      from: selectedSquare.value,
      to: square.square,
      promotion: move?.length > 4 ? move.slice(4) : null,
      promotionOptions: [],
    })
    selectedSquare.value = ''
    return
  }

  if (square.side !== props.activeSide) {
    selectedSquare.value = ''
    return
  }

  const moves = legalMovesForSquare(props.legalMovesUci, square.square)
  selectedSquare.value = moves.length ? square.square : ''
}

function squareClasses(square) {
  return {
    'board-square--light': squareColor(square) === 'light',
    'board-square--dark': squareColor(square) === 'dark',
    'board-square--selected': selectedSquare.value === square.square,
    'board-square--legal': targetSquares.value.has(square.square),
  }
}

function pieceClasses(square) {
  return {
    'board-square__piece--white': square.side === 'WHITE',
    'board-square__piece--black': square.side === 'BLACK',
    'board-square__piece--empty': !square.side,
  }
}
</script>

<style lang="scss" scoped>
.board-frame {
  padding: 1rem;
  display: grid;
  gap: 1rem;
  position: relative;
}

.board-toolbar {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.board-grid {
  display: grid;
  grid-template-columns: repeat(8, minmax(0, 1fr));
  overflow: hidden;
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.board-overlay {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  border-radius: inherit;
  background: rgba(8, 12, 16, 0.34);
  backdrop-filter: blur(4px);
  z-index: 3;
  pointer-events: none;

  &__label {
    padding: 0.8rem 1rem;
    border-radius: 999px;
    background: rgba(17, 24, 31, 0.86);
    border: 1px solid var(--line);
    color: var(--text);
    font-weight: 700;
  }
}

.board-square {
  aspect-ratio: 1;
  border: 0;
  position: relative;
  display: grid;
  place-items: center;
  font-size: clamp(1.7rem, 4vw, 3rem);
  cursor: pointer;
  transition: transform 0.12s ease, box-shadow 0.12s ease, filter 0.12s ease;

  &:hover {
    filter: brightness(1.05);
  }

  &:disabled {
    cursor: default;
  }

  &--light {
    background: var(--board-light);
  }

  &--dark {
    background: var(--board-dark);
  }

  &--selected {
    box-shadow: inset 0 0 0 5px var(--board-highlight);
  }

  &--legal {
    box-shadow: inset 0 0 0 5px var(--board-legal);
  }

  &__label {
    position: absolute;
    left: 0.35rem;
    top: 0.3rem;
    font-size: 0.62rem;
    opacity: 0.7;
    font-weight: 700;
  }

  &__piece {
    width: min(74%, 4.2rem);
    height: min(74%, 4.2rem);
    object-fit: contain;
    transform: translateY(0.08rem);
    transition: transform 0.12s ease, filter 0.12s ease;
    user-select: none;
    pointer-events: none;

    &--white {
      filter: drop-shadow(0 2px 4px rgba(78, 54, 26, 0.16));
    }

    &--black {
      filter: drop-shadow(0 2px 4px rgba(255, 245, 228, 0.08));
    }

    &--empty {
      opacity: 0;
    }
  }
}
</style>
