<template lang="pug">
.board-frame.glass-card
  .board-toolbar
    .pill {{ orientationLabel }}
    .pill(v-if='selectedSquare') Seleccionada {{ selectedSquare }}
    .pill(v-if='busy && busyLabel') {{ busyLabel }}
  .board-stage
    .board-status.animate__animated.animate__fadeIn(v-if='busy && busyLabel')
      span.board-status__label {{ busyLabel || 'Procesando...' }}
    .board-grid(ref='boardGrid')
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
    transition(name='board-ghost')
      img.board-ghost.animate__animated.animate__fadeIn(
        v-if='movingPiece'
        :src='movingPiece.asset'
        :alt='movingPiece.alt'
        :style='movingPieceStyle'
        draggable='false'
      )
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
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
  animatedMoveUci: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['move-intent'])

const selectedSquare = ref('')
const boardGrid = ref(null)
const movingPiece = ref(null)
const movingPieceStyle = ref({})

const squares = computed(() => parseFenBoard(props.fen))
const orientedSquares = computed(() => orientSquares(squares.value, props.perspective))
const activeMoves = computed(() => legalMovesForSquare(props.legalMovesUci, selectedSquare.value))
const targetSquares = computed(() => new Set(activeMoves.value.map((move) => move.slice(2, 4))))
const orientationLabel = computed(() => `Vista ${props.perspective === 'WHITE' ? 'blancas' : 'negras'}`)

watch(() => props.fen, () => {
  selectedSquare.value = ''
})

watch(() => props.animatedMoveUci, async (nextMove, previousMove) => {
  if (!nextMove || nextMove === previousMove) {
    return
  }

  const from = nextMove.slice(0, 2)
  const to = nextMove.slice(2, 4)
  const targetSquare = squares.value.find((square) => square.square === to)
  if (!targetSquare?.pieceAsset || !boardGrid.value) {
    return
  }

  const geometry = squareGeometry(from, to)
  if (!geometry) {
    return
  }

  movingPiece.value = {
    asset: targetSquare.pieceAsset,
    alt: targetSquare.pieceAlt,
  }
  movingPieceStyle.value = {
    width: `${geometry.size}px`,
    height: `${geometry.size}px`,
    left: `${geometry.fromLeft}px`,
    top: `${geometry.fromTop}px`,
    transform: 'translate3d(0, 0, 0) scale(1)',
  }

  await nextTick()
  requestAnimationFrame(() => {
    movingPieceStyle.value = {
      ...movingPieceStyle.value,
      left: `${geometry.toLeft}px`,
      top: `${geometry.toTop}px`,
      transform: 'translate3d(0, 0, 0) scale(1)',
    }
  })

  window.setTimeout(() => {
    movingPiece.value = null
    movingPieceStyle.value = {}
  }, 380)
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
    'board-square__piece--hidden': movingPiece.value && props.animatedMoveUci?.slice(2, 4) === square.square,
  }
}

function squareGeometry(fromSquare, toSquare) {
  const board = boardGrid.value
  if (!board) {
    return null
  }

  const size = board.clientWidth / 8
  const fromIndex = orientedSquares.value.findIndex((square) => square.square === fromSquare)
  const toIndex = orientedSquares.value.findIndex((square) => square.square === toSquare)
  if (fromIndex < 0 || toIndex < 0) {
    return null
  }

  const fromRow = Math.floor(fromIndex / 8)
  const fromCol = fromIndex % 8
  const toRow = Math.floor(toIndex / 8)
  const toCol = toIndex % 8
  const pieceSize = size * 0.74
  const offset = (size - pieceSize) / 2

  return {
    size: pieceSize,
    fromLeft: fromCol * size + offset,
    fromTop: fromRow * size + offset + 1,
    toLeft: toCol * size + offset,
    toTop: toRow * size + offset + 1,
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

.board-stage {
  position: relative;
}

.board-grid {
  display: grid;
  grid-template-columns: repeat(8, minmax(0, 1fr));
  overflow: hidden;
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.board-status {
  position: absolute;
  top: 0.9rem;
  left: 50%;
  transform: translateX(-50%);
  z-index: 4;
  pointer-events: none;

  &__label {
    display: inline-flex;
    align-items: center;
    gap: 0.45rem;
    padding: 0.55rem 0.85rem;
    border-radius: 999px;
    background: rgba(17, 24, 31, 0.84);
    border: 1px solid rgba(255, 138, 102, 0.28);
    color: var(--text);
    font-weight: 700;
    box-shadow: 0 12px 30px rgba(0, 0, 0, 0.2);
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

    &--hidden {
      opacity: 0;
    }
  }
}

.board-ghost {
  position: absolute;
  z-index: 5;
  pointer-events: none;
  object-fit: contain;
  transition:
    left 0.32s cubic-bezier(0.22, 1, 0.36, 1),
    top 0.32s cubic-bezier(0.22, 1, 0.36, 1),
    transform 0.32s cubic-bezier(0.22, 1, 0.36, 1);
  filter: drop-shadow(0 12px 20px rgba(7, 11, 15, 0.22));
}

.board-ghost-enter-active,
.board-ghost-leave-active {
  transition: opacity 0.18s ease;
}

.board-ghost-enter-from,
.board-ghost-leave-to {
  opacity: 0;
}
</style>
