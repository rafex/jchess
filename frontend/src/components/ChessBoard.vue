<template lang="pug">
.board-frame.glass-card
  .board-toolbar
    .pill {{ orientationLabel }}
    .pill(v-if='selectedSquare') Seleccionada {{ selectedSquare }}
  .board-grid
    button.board-square(
      v-for='square in orientedSquares'
      :key='square.key'
      type='button'
      :class='squareClasses(square)'
      @click='selectSquare(square)'
    )
      span.board-square__label {{ square.square }}
      span.board-square__piece(:class='pieceClasses(square)') {{ square.pieceGlyph }}
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
  if (selectedSquare.value && targetSquares.value.has(square.square)) {
    const move = activeMoves.value.find((candidate) => candidate.slice(2, 4) === square.square)
    emit('move-intent', {
      from: selectedSquare.value,
      to: square.square,
      promotion: move?.length > 4 ? move.slice(4) : null,
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
    transform: translateY(0.1rem);
    line-height: 1;
    transition: transform 0.12s ease, filter 0.12s ease;

    &--white {
      color: #fff8ec;
      text-shadow:
        0 1px 0 rgba(95, 70, 33, 0.28),
        0 0 12px rgba(255, 249, 236, 0.18);
    }

    &--black {
      color: #1c1711;
      text-shadow:
        0 1px 0 rgba(255, 244, 223, 0.15),
        0 0 10px rgba(8, 6, 4, 0.12);
    }

    &--empty {
      opacity: 0;
    }
  }
}
</style>
