<template lang="pug">
.page-shell.game-view(v-if='game')
  section.game-view__hero.glass-card.animate__animated.animate__fadeIn
    .game-view__headline
      .pill Partida en curso
      h1 {{ title }}
      p {{ subtitle }}
    .game-view__actions
      button.button.button--secondary(type='button' @click='flipBoard') Girar tablero
      button.button.button--secondary(type='button' @click='leaveGame') Salir
      button.button.button--primary(type='button' @click='resign') Rendirse

  section.game-view__layout
    .game-view__board-shell(ref='boardShell')
      ChessBoard.game-view__board(
        :fen='game.fen'
        :legal-moves-uci='game.legalMovesUci'
        :perspective='perspective'
        :active-side='game.turn'
        @move-intent='submitBoardMove'
      )
    aside.game-view__sidebar(:style='sidebarStyle')
      .glass-card.panel
        h2.panel__title Estado
        ul.panel__list
          li
            strong Turno:
            span  {{ game.turn }}
          li
            strong Resultado:
            span  {{ game.result }}
          li
            strong Sesión:
            span  {{ game.sessionId }}
          li
            strong FEN:
            code.panel__code {{ game.fen }}
      .glass-card.panel
        h2.panel__title Jugadores
        ul.panel__list
          li(v-for='player in game.players' :key='player.playerId')
            strong {{ player.side }}:
            span  {{ player.displayName }}
            small.panel__hint(v-if='player.side === currentControlSide') control actual
      .glass-card.panel.panel--moves
        h2.panel__title Movimientos
        .panel__moves-scroll
          ol.panel__moves
            li(v-for='move in game.moves' :key='`${move.ply}-${move.uci}`')
              span {{ move.ply }}.
              strong  {{ move.canonicalNotation }}
              small  ({{ move.uci }})
      .glass-card.panel(v-if='feedback')
        h2.panel__title Aviso
        p.panel__message {{ feedback }}
.page-shell.game-view(v-else)
  .glass-card.panel
    CubesLoader(label='Cargando partida...')
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ChessBoard from '../components/ChessBoard.vue'
import CubesLoader from '../components/CubesLoader.vue'
import { loadGame, resignGame, submitMove } from '../lib/api'
import { useSessionStore } from '../lib/sessionStore'

const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()

const game = ref(null)
const feedback = ref('')
const perspective = ref(sessionStore.state.perspective || 'WHITE')
const boardShell = ref(null)
const boardHeight = ref(0)
let boardObserver = null

const title = computed(() => {
  if (!game.value) {
    return 'Cargando'
  }
  return `${playerName('WHITE')} vs ${playerName('BLACK')}`
})

const subtitle = computed(() => {
  if (!game.value) {
    return ''
  }
  return game.value.result === 'IN_PROGRESS'
    ? `Turno actual: ${game.value.turn}`
    : `Partida finalizada por ${game.value.endReason}`
})

const currentControlSide = computed(() => {
  if (!game.value) {
    return 'WHITE'
  }

  if (sessionStore.state.localHotseat) {
    return game.value.turn
  }

  return Object.entries(sessionStore.state.tokens).find(([, token]) => token)?.[0] || 'WHITE'
})

const sidebarStyle = computed(() => {
  if (!boardHeight.value) {
    return {}
  }

  return {
    height: `${boardHeight.value}px`,
    maxHeight: `${boardHeight.value}px`,
  }
})

onMounted(async () => {
  await refreshGame()
  await nextTick()
  syncBoardHeight()
  if (typeof ResizeObserver !== 'undefined' && boardShell.value) {
    boardObserver = new ResizeObserver(() => {
      syncBoardHeight()
    })
    boardObserver.observe(boardShell.value)
  }
})

onBeforeUnmount(() => {
  boardObserver?.disconnect()
})

watch(() => game.value?.fen, async () => {
  await nextTick()
  syncBoardHeight()
})

async function refreshGame() {
  const response = await loadGame(route.params.sessionId)
  game.value = response.data
  if (sessionStore.state.localHotseat) {
    perspective.value = game.value.turn
  }
}

async function submitBoardMove(move) {
  try {
    feedback.value = ''
    const playerToken = sessionStore.state.tokens[currentControlSide.value]
    const response = await submitMove(game.value.sessionId, playerToken, move.from, move.to, move.promotion)
    game.value = response.data

    if (sessionStore.state.localHotseat) {
      perspective.value = game.value.turn
    }
  } catch (error) {
    feedback.value = error.message || 'No se pudo aplicar el movimiento'
  }
}

async function resign() {
  try {
    const playerToken = sessionStore.state.tokens[currentControlSide.value]
    const response = await resignGame(game.value.sessionId, playerToken)
    game.value = response.data
  } catch (error) {
    feedback.value = error.message || 'No se pudo rendir la partida'
  }
}

function flipBoard() {
  perspective.value = perspective.value === 'WHITE' ? 'BLACK' : 'WHITE'
  sessionStore.setPerspective(perspective.value)
}

async function leaveGame() {
  sessionStore.clear()
  await router.push({ name: 'home' })
}

function playerName(side) {
  return game.value?.players.find((player) => player.side === side)?.displayName || side
}

function syncBoardHeight() {
  boardHeight.value = boardShell.value?.getBoundingClientRect().height
    ? Math.round(boardShell.value.getBoundingClientRect().height)
    : 0
}
</script>

<style lang="scss" scoped>
.game-view {
  display: grid;
  gap: 1.25rem;

  &__hero {
    padding: 1.5rem;
    display: flex;
    justify-content: space-between;
    gap: 1rem;
    flex-wrap: wrap;
    background:
      radial-gradient(circle at top right, rgba(255, 138, 102, 0.1), transparent 26%),
      linear-gradient(180deg, rgba(25, 36, 47, 0.94), rgba(19, 29, 38, 0.9));
  }

  &__headline {
    h1 {
      margin: 0.7rem 0 0.45rem;
      font-size: clamp(1.7rem, 4vw, 3rem);
      letter-spacing: -0.05em;
    }

    p {
      margin: 0;
      color: var(--muted);
    }
  }

  &__actions {
    display: flex;
    gap: 0.8rem;
    flex-wrap: wrap;
    align-items: center;
  }

  &__layout {
    display: grid;
    gap: 1.25rem;
    grid-template-columns: minmax(0, 2fr) minmax(300px, 1fr);
    align-items: start;
  }

  &__board-shell {
    min-width: 0;
  }

  &__sidebar {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    overflow: hidden;
    min-height: 0;
  }
}

.panel {
  padding: 1.2rem;
  background: linear-gradient(180deg, rgba(24, 35, 46, 0.96), rgba(19, 29, 38, 0.92));

  &__title {
    margin: 0 0 0.85rem;
    font-size: 1.05rem;
  }

  &__list,
  &__moves {
    margin: 0;
    padding-left: 1rem;
    display: grid;
    gap: 0.55rem;
    color: var(--muted);
  }

  &__code {
    display: inline-block;
    margin-top: 0.3rem;
    word-break: break-word;
    color: var(--text);
  }

  &__hint {
    margin-left: 0.5rem;
    color: var(--primary);
  }

  &__message {
    margin: 0;
    color: var(--danger);
  }

  &__moves-scroll {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    overflow-x: hidden;
    padding-right: 0.35rem;
    scrollbar-width: thin;
    scrollbar-color: rgba(255, 179, 71, 0.65) rgba(255, 255, 255, 0.05);

    &::-webkit-scrollbar {
      width: 10px;
    }

    &::-webkit-scrollbar-track {
      background: rgba(255, 255, 255, 0.05);
      border-radius: 999px;
    }

    &::-webkit-scrollbar-thumb {
      background: linear-gradient(180deg, rgba(255, 179, 71, 0.9), rgba(255, 124, 67, 0.8));
      border-radius: 999px;
      border: 2px solid rgba(10, 16, 20, 0.35);
    }
  }

  &--moves {
    display: flex;
    flex-direction: column;
    min-height: 0;
  }
}

@media (max-width: 960px) {
  .game-view__layout {
    grid-template-columns: 1fr;
  }

  .game-view__sidebar {
    position: static;
    height: auto !important;
    max-height: none !important;
    overflow: visible;
  }

  .panel__moves-scroll {
    max-height: 20rem;
  }
}
</style>
