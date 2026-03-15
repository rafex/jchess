<template lang="pug">
.page-shell.game-view
  section.game-view__hero.glass-card
    .game-view__headline
      .pill Modo offline local
      h1 {{ title }}
      p {{ subtitle }}
    .game-view__actions
      button.button.button--secondary(type='button' @click='flipBoard') Girar tablero
      button.button.button--secondary(type='button' @click='resetGame') Reiniciar
      RouterLink.button.button--tonal(to='/') Volver al inicio

  section.game-view__layout
    .game-view__board-shell
      ChessBoard(
        :fen='fen'
        :legal-moves-uci='legalMovesUci'
        :animated-move-uci='lastMoveUci'
        :perspective='perspective'
        :active-side='turn'
        :interactive='status === "ACTIVE"'
        @move-intent='submitBoardMove'
      )
      PromotionDialog(
        :open='promotionState.open'
        :subtitle='promotionSubtitle'
        :options='promotionChoices'
        @select='confirmPromotion'
        @cancel='cancelPromotion'
      )
    aside.game-view__sidebar
      ClockPanel(
        :white-label='clock.whiteLabel.value'
        :black-label='clock.blackLabel.value'
        :active-side='turn'
        :time-control='timeControl'
      )
      .glass-card.panel
        h2.panel__title Estado
        ul.panel__list
          li
            strong Turno:
            span  {{ turn }}
          li
            strong Resultado:
            span  {{ result }}
          li
            strong FEN:
            code.panel__code {{ fen }}
      .glass-card.panel.panel--moves
        h2.panel__title Movimientos
        .panel__moves-scroll
          ol.panel__moves
            li(v-for='move in moveHistory' :key='`${move.ply}-${move.uci}`')
              span {{ move.ply }}.
              strong  {{ move.san }}
              small  ({{ move.uci }})
      ImportExportPanel(
        :fen='fen'
        :pgn='pgn'
        allow-import
        hint='Puedes pegar una posición FEN o una partida PGN para seguir jugando offline.'
        @import-fen='importFen'
        @import-pgn='importPgn'
      )
      .glass-card.panel(v-if='feedback')
        h2.panel__title Aviso
        p.panel__message {{ feedback }}
</template>

<script setup>
import { Chess } from 'chess.js'
import { computed, ref, watch } from 'vue'
import ChessBoard from '../components/ChessBoard.vue'
import ClockPanel from '../components/ClockPanel.vue'
import ImportExportPanel from '../components/ImportExportPanel.vue'
import PromotionDialog from '../components/PromotionDialog.vue'
import { promotionOptions } from '../lib/chess'
import { useSessionStore } from '../lib/sessionStore'
import { useChessClock } from '../lib/timeControl'

const sessionStore = useSessionStore()
const OFFLINE_STATE_KEY = 'jchess.frontend.offline-game'
const timeControl = sessionStore.state.timeControl || '5+0'
const chess = ref(new Chess())
const perspective = ref(sessionStore.state.perspective || 'WHITE')
const feedback = ref('')
const promotionState = ref({ open: false, move: null })
const historyVersion = ref(0)

const fen = computed(() => chess.value.fen())
const turn = computed(() => chess.value.turn() === 'w' ? 'WHITE' : 'BLACK')
const status = computed(() => (chess.value.isGameOver() ? 'FINISHED' : 'ACTIVE'))
const result = computed(() => {
  if (chess.value.isCheckmate()) {
    return turn.value === 'WHITE' ? 'BLACK_WIN' : 'WHITE_WIN'
  }
  if (chess.value.isDraw()) {
    return 'DRAW'
  }
  return 'IN_PROGRESS'
})
const title = computed(() => 'Blancas vs Negras')
const subtitle = computed(() => status.value === 'ACTIVE'
  ? `Turno actual: ${turn.value}`
  : `Partida finalizada: ${result.value}`)
const legalMovesUci = computed(() => chess.value.moves({ verbose: true }).map((move) => `${move.from}${move.to}${move.promotion || ''}`))
const moveHistory = computed(() => chess.value.history({ verbose: true }).map((move, index) => ({
  ply: index + 1,
  san: move.san,
  uci: `${move.from}${move.to}${move.promotion || ''}`,
})))
const lastMoveUci = computed(() => moveHistory.value.at(-1)?.uci || '')
const pgn = computed(() => chess.value.pgn())
const promotionChoices = computed(() => promotionOptions(turn.value))
const promotionSubtitle = computed(() => `Movimiento ${promotionState.value.move?.from || ''} → ${promotionState.value.move?.to || ''}`)

const clock = useChessClock({
  activeSide: turn,
  version: historyVersion,
  status,
  timeControl: computed(() => timeControl),
})

restoreOfflineSnapshot()

watch([fen, pgn, perspective], () => {
  persistOfflineSnapshot()
})

function submitBoardMove(move) {
  feedback.value = ''
  if (move.promotionOptions?.length) {
    promotionState.value = { open: true, move }
    return
  }

  applyMove(move)
}

function applyMove(move) {
  const resultMove = chess.value.move({
    from: move.from,
    to: move.to,
    promotion: move.promotion || 'q',
  })

  if (!resultMove) {
    feedback.value = 'Movimiento ilegal en modo offline'
    return
  }

  historyVersion.value += 1
  if (sessionStore.state.localHotseat) {
    perspective.value = turn.value
  }
}

function confirmPromotion(code) {
  const move = promotionState.value.move
  promotionState.value = { open: false, move: null }
  if (move) {
    applyMove({ ...move, promotion: code })
  }
}

function cancelPromotion() {
  promotionState.value = { open: false, move: null }
}

function flipBoard() {
  perspective.value = perspective.value === 'WHITE' ? 'BLACK' : 'WHITE'
}

function resetGame() {
  chess.value = new Chess()
  historyVersion.value += 1
  feedback.value = ''
  localStorage.removeItem(OFFLINE_STATE_KEY)
}

function importFen(value) {
  try {
    chess.value.load(value)
    historyVersion.value += 1
    feedback.value = 'FEN cargado en modo offline'
  } catch (error) {
    feedback.value = 'FEN inválido'
  }
}

function importPgn(value) {
  try {
    chess.value.loadPgn(value)
    historyVersion.value += 1
    feedback.value = 'PGN cargado en modo offline'
  } catch (error) {
    feedback.value = 'PGN inválido'
  }
}

function persistOfflineSnapshot() {
  localStorage.setItem(OFFLINE_STATE_KEY, JSON.stringify({
    fen: fen.value,
    pgn: pgn.value,
    perspective: perspective.value,
    updatedAt: new Date().toISOString(),
  }))
}

function restoreOfflineSnapshot() {
  const raw = localStorage.getItem(OFFLINE_STATE_KEY)
  if (!raw) {
    return
  }

  try {
    const snapshot = JSON.parse(raw)
    if (snapshot.pgn) {
      chess.value.loadPgn(snapshot.pgn)
    } else if (snapshot.fen) {
      chess.value.load(snapshot.fen)
    }

    if (snapshot.perspective === 'WHITE' || snapshot.perspective === 'BLACK') {
      perspective.value = snapshot.perspective
    }

    if (snapshot.pgn || snapshot.fen) {
      historyVersion.value += 1
      feedback.value = 'Se restauró la última partida offline guardada en este dispositivo'
    }
  } catch {
    localStorage.removeItem(OFFLINE_STATE_KEY)
  }
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
    position: relative;
  }

  &__sidebar {
    display: flex;
    flex-direction: column;
    gap: 1rem;
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

  &__message {
    margin: 0;
    color: var(--danger);
  }

  &__moves-scroll {
    max-height: 22rem;
    overflow-y: auto;
    padding-right: 0.35rem;
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
}
</style>
