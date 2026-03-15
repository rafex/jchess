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
      button.button.button--primary(type='button' :disabled='submittingMove || game.status !== "ACTIVE"' @click='resign') Rendirse

  section.game-view__layout
    .game-view__board-shell(ref='boardShell')
      ChessBoard.game-view__board(
        :fen='game.fen'
        :legal-moves-uci='game.legalMovesUci'
        :perspective='perspective'
        :active-side='game.turn'
        :interactive='canInteractWithBoard'
        :busy='boardBusy'
        :busy-label='boardBusyLabel'
        @move-intent='submitBoardMove'
      )
      PromotionDialog(
        :open='promotionState.open'
        :subtitle='promotionSubtitle'
        :options='promotionChoices'
        @select='confirmPromotion'
        @cancel='cancelPromotion'
      )
    aside.game-view__sidebar(:style='sidebarStyle')
      ClockPanel(
        v-if='showClock'
        :white-label='clock.whiteLabel.value'
        :black-label='clock.blackLabel.value'
        :active-side='game.turn'
        :time-control='game.timeControl'
      )
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
          li
            strong Conexión:
            span  {{ connectionLabel }}
      .glass-card.panel
        h2.panel__title Jugadores
        ul.panel__list
          li(v-for='player in game.players' :key='player.playerId')
            strong {{ player.side }}:
            span  {{ player.displayName }}
            small.panel__hint(v-if='player.side === currentControlSide') control actual
            small.panel__hint(v-else-if='player.connected') conectado
      .glass-card.panel(v-if='showRemoteInvite')
        h2.panel__title Invitación remota
        p.panel__muted Comparte estos accesos con cada jugador. El token da control de ese color.
        .panel__invite(v-for='invite in remoteInvites' :key='invite.side')
          strong {{ invite.side }}
          code.panel__code {{ invite.sessionId }}
          code.panel__code {{ invite.token }}
          button.button.button--tonal(type='button' @click='copyInvite(invite)') Copiar acceso
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
      ImportExportPanel(
        :fen='game.fen'
        :pgn='game.pgn'
        hint='Puedes copiar la posición actual o la partida completa para análisis y respaldo.'
      )
.page-shell.game-view(v-else)
  .glass-card.panel
    CubesLoader(label='Cargando partida...')
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ChessBoard from '../components/ChessBoard.vue'
import ClockPanel from '../components/ClockPanel.vue'
import CubesLoader from '../components/CubesLoader.vue'
import ImportExportPanel from '../components/ImportExportPanel.vue'
import PromotionDialog from '../components/PromotionDialog.vue'
import { loadGame, resignGame, submitMove } from '../lib/api'
import { promotionOptions } from '../lib/chess'
import { createGameSocket } from '../lib/realtime'
import { useSessionStore } from '../lib/sessionStore'
import { useChessClock } from '../lib/timeControl'

const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()

const game = ref(null)
const feedback = ref('')
const perspective = ref(sessionStore.state.perspective || 'WHITE')
const boardShell = ref(null)
const boardHeight = ref(0)
const submittingMove = ref(false)
const socketStatus = ref('idle')
const promotionState = ref({
  open: false,
  move: null,
})
let boardObserver = null
let socketClient = null

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
  if (game.value.status !== 'ACTIVE') {
    return `Partida finalizada por ${game.value.endReason}`
  }
  if (submittingMove.value) {
    return sessionStore.state.opponent === 'machine'
      ? 'El motor está pensando su respuesta...'
      : 'Enviando movimiento...'
  }
  if (promotionState.value.open) {
    return 'Selecciona la pieza para promocionar el peón.'
  }
  if (!sessionStore.state.localHotseat && !sessionStore.state.tokens[currentControlSide.value]) {
    return 'Vista de solo lectura desde el lobby. Usa un enlace de invitación para jugar en vivo.'
  }
  if (isRemoteHuman.value && game.value.turn !== currentControlSide.value) {
    return 'Esperando el movimiento del rival en tiempo real.'
  }
  return game.value.result === 'IN_PROGRESS'
    ? `Turno actual: ${game.value.turn}`
    : `Partida finalizada por ${game.value.endReason}`
})

const isRemoteHuman = computed(() => sessionStore.state.opponent === 'human' && !sessionStore.state.localHotseat)

const currentControlSide = computed(() => {
  if (!game.value) {
    return null
  }

  if (sessionStore.state.localHotseat) {
    return game.value.turn
  }

  return sessionStore.state.requesterSide || null
})

const canInteractWithBoard = computed(() => {
  if (!game.value || game.value.status !== 'ACTIVE') {
    return false
  }

  if (promotionState.value.open || submittingMove.value) {
    return false
  }

  if (!sessionStore.state.localHotseat && !sessionStore.state.tokens[currentControlSide.value]) {
    return false
  }

  return game.value.turn === currentControlSide.value
})

const boardBusy = computed(() => submittingMove.value || promotionState.value.open)

const boardBusyLabel = computed(() => {
  if (promotionState.value.open) {
    return 'Elige promoción'
  }
  if (submittingMove.value) {
    return sessionStore.state.opponent === 'machine'
      ? 'El motor está pensando...'
      : 'Enviando jugada...'
  }
  if (isRemoteHuman.value && game.value && game.value.turn !== currentControlSide.value && game.value.status === 'ACTIVE') {
    return 'Esperando al rival...'
  }
  return ''
})

const promotionChoices = computed(() => promotionOptions(currentControlSide.value))
const promotionSubtitle = computed(() => `Movimiento ${promotionState.value.move?.from || ''} → ${promotionState.value.move?.to || ''}`)
const connectionLabel = computed(() => {
  if (!isRemoteHuman.value) {
    return 'No requerida'
  }
  switch (socketStatus.value) {
    case 'connected':
      return 'En vivo'
    case 'connecting':
      return 'Conectando...'
    case 'error':
      return 'Error de enlace'
    case 'disconnected':
      return 'Reconectando...'
    default:
      return 'Inactivo'
  }
})
const showRemoteInvite = computed(() => isRemoteHuman.value
  && Object.values(sessionStore.state.inviteTokens || {}).some(Boolean))
const remoteInvites = computed(() => ['WHITE', 'BLACK']
  .filter((side) => sessionStore.state.inviteTokens?.[side])
  .map((side) => ({
    side,
    sessionId: game.value?.sessionId || sessionStore.state.sessionId,
    token: sessionStore.state.inviteTokens[side],
  })))
const effectiveClockSide = computed(() => {
  if (!game.value) {
    return 'WHITE'
  }

  if (submittingMove.value && game.value.status === 'ACTIVE') {
    return game.value.turn === 'WHITE' ? 'BLACK' : 'WHITE'
  }

  return game.value.turn
})
const showClock = computed(() => game.value?.timeControl && game.value.timeControl !== 'untimed')
const clock = useChessClock({
  activeSide: effectiveClockSide,
  version: computed(() => game.value?.version || 0),
  status: computed(() => game.value?.status || 'ACTIVE'),
  timeControl: computed(() => game.value?.timeControl || sessionStore.state.timeControl || '5+0'),
  whiteMs: computed(() => game.value?.whiteClockMs ?? null),
  blackMs: computed(() => game.value?.blackClockMs ?? null),
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
  connectRealtime()
  if (typeof ResizeObserver !== 'undefined' && boardShell.value) {
    boardObserver = new ResizeObserver(() => {
      syncBoardHeight()
    })
    boardObserver.observe(boardShell.value)
  }
})

onBeforeUnmount(() => {
  boardObserver?.disconnect()
  socketClient?.disconnect()
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
  if (!game.value || game.value.status !== 'ACTIVE') {
    return
  }

  if (move.promotionOptions?.length) {
    promotionState.value = {
      open: true,
      move,
    }
    return
  }

  await performMove(move)
}

async function performMove(move) {
  try {
    feedback.value = ''
    submittingMove.value = true
    const playerToken = sessionStore.state.tokens[currentControlSide.value]
    if (isRemoteHuman.value) {
      const sent = socketClient?.send('move', {
        from: move.from,
        to: move.to,
        promotion: move.promotion,
      })
      if (!sent) {
        throw new Error('La conexión en tiempo real no está lista')
      }
    } else {
      const response = await submitMove(game.value.sessionId, playerToken, move.from, move.to, move.promotion)
      game.value = response.data
    }

    if (sessionStore.state.localHotseat) {
      perspective.value = game.value.turn
    }
  } catch (error) {
    feedback.value = error.message || 'No se pudo aplicar el movimiento'
  } finally {
    if (!isRemoteHuman.value) {
      submittingMove.value = false
    }
  }
}

async function resign() {
  try {
    submittingMove.value = true
    const playerToken = sessionStore.state.tokens[currentControlSide.value]
    if (isRemoteHuman.value) {
      const sent = socketClient?.send('resign')
      if (!sent) {
        throw new Error('La conexión en tiempo real no está lista')
      }
    } else {
      const response = await resignGame(game.value.sessionId, playerToken)
      game.value = response.data
    }
  } catch (error) {
    feedback.value = error.message || 'No se pudo rendir la partida'
  } finally {
    if (!isRemoteHuman.value) {
      submittingMove.value = false
    }
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

function confirmPromotion(code) {
  const move = promotionState.value.move
  promotionState.value = { open: false, move: null }
  if (!move) {
    return
  }
  performMove({
    ...move,
    promotion: code,
  })
}

function cancelPromotion() {
  promotionState.value = { open: false, move: null }
}

function connectRealtime() {
  if (!isRemoteHuman.value) {
    return
  }

  const playerToken = sessionStore.state.tokens[currentControlSide.value]
  if (!playerToken) {
    feedback.value = 'No hay token local para conectarse a la partida remota'
    return
  }

  socketClient = createGameSocket({
    sessionId: route.params.sessionId,
    playerToken,
    onStatusChange(status) {
      socketStatus.value = status
    },
    onEnvelope(envelope) {
      if (envelope.error) {
        feedback.value = envelope.error.message || 'Error en tiempo real'
        submittingMove.value = false
        return
      }

      switch (envelope.type) {
        case 'player_joined':
          if (envelope.data?.game) {
            game.value = envelope.data.game
          }
          feedback.value = 'El rival se conectó a la mesa.'
          submittingMove.value = false
          break
        case 'game_state':
        case 'move_submitted':
        case 'move_undone':
        case 'game_finished':
        case 'player_disconnected':
          if (envelope.data?.sessionId) {
            game.value = envelope.data
          }
          submittingMove.value = false
          if (envelope.type === 'player_disconnected') {
            feedback.value = 'El rival se desconectó. La app intentará reconectar automáticamente.'
          }
          if (sessionStore.state.localHotseat && game.value?.turn) {
            perspective.value = game.value.turn
          }
          break
        case 'turn_changed':
          if (game.value) {
            game.value.turn = envelope.data.turn
            game.value.version = envelope.data.version
          }
          break
        default:
          break
      }
    },
  })

  socketClient.connect()
}

async function copyInvite(invite) {
  const joinUrl = `${window.location.origin}/?sessionId=${encodeURIComponent(invite.sessionId)}&playerToken=${encodeURIComponent(invite.token)}`
  const access = `Sesion: ${invite.sessionId}\nToken ${invite.side}: ${invite.token}\nAbrir: ${joinUrl}`
  await navigator.clipboard.writeText(access)
  feedback.value = `Acceso ${invite.side} copiado al portapapeles`
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
    overflow-y: auto;
    overflow-x: hidden;
    min-height: 0;
    padding-right: 0.2rem;
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

  &__muted {
    margin: 0;
    color: var(--muted);
  }

  &__invite {
    display: grid;
    gap: 0.5rem;
    padding-top: 0.8rem;
    margin-top: 0.8rem;
    border-top: 1px solid var(--line);
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
    min-height: 13rem;
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
