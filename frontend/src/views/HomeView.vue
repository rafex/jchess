<template lang="pug">
.page-shell.home-view
  section.home-view__hero.glass-card
    .home-view__copy
      span.pill Material-inspired JChess
      h1.home-view__title Juega ajedrez desde una interfaz pensada para web y escritorio
      p.home-view__text
        | Arranca una partida, elige color o deja que el azar decida por ti,
        |  y entra directo al tablero sobre el motor real.
    .home-view__meta
      .pill PWA instalable
      .pill Partidas locales o vs máquina
  section.home-view__install.glass-card
    .home-view__install-copy
      h2 Lleva JChess a tu pantalla de inicio
      p
        | En escritorio usa el botón
        strong  Instalar app
        |  del encabezado. En iPhone/iPad usa Compartir y luego
        strong  Añadir a pantalla de inicio.
  StartGamePanel(@start='startGame' @join='joinRemoteGame')
  section.home-view__lobby.glass-card
    .home-view__lobby-head
      div
        span.pill Lobby
        h2.home-view__section-title Partidas recientes
      button.button.button--secondary(type='button' @click='refreshGames') Actualizar
    p.home-view__empty(v-if='loadingGames') Cargando partidas...
    p.home-view__empty(v-else-if='gamesError') {{ gamesError }}
    p.home-view__empty(v-else-if='!games.length') Aún no hay partidas guardadas.
    .home-view__games(v-else)
      article.home-view__game-card(v-for='game in games' :key='game.sessionId')
        .home-view__game-meta
          h3 {{ game.whitePlayerName }} vs {{ game.blackPlayerName }}
          .pill {{ game.status }}
        p.home-view__game-detail Turno: {{ game.turn }} · Movimientos: {{ game.moveCount }}
        p.home-view__game-detail Resultado: {{ game.result }}
        .home-view__game-actions
          button.button.button--tonal(type='button' @click='resumeGame(game.sessionId)') Abrir
          code {{ shortSession(game.sessionId) }}
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import StartGamePanel from '../components/StartGamePanel.vue'
import { createGame, joinGame, listGames } from '../lib/api'
import { useSessionStore } from '../lib/sessionStore'

const router = useRouter()
const route = useRoute()
const sessionStore = useSessionStore()
const games = ref([])
const loadingGames = ref(false)
const gamesError = ref('')

onMounted(() => {
  refreshGames()
  maybeAutoJoinFromInvite()
})

async function startGame(payload) {
  if (payload.opponent === 'offline-local') {
    sessionStore.setSession({
      sessionId: 'offline',
      opponent: 'offline-local',
      requesterSide: payload.color.toUpperCase(),
      localHotseat: true,
      perspective: payload.color.toUpperCase(),
      tokens: { WHITE: null, BLACK: null },
      inviteTokens: { WHITE: null, BLACK: null },
      playerNames: {
        WHITE: payload.whitePlayerName || 'White',
        BLACK: payload.blackPlayerName || 'Black',
      },
      timeControl: payload.timeControl,
    })
    await router.push({ name: 'offline-game' })
    return
  }

  const response = await createGame(payload)
  const game = response.data.game
  const requester = response.data.requester

  const tokens = {
    WHITE: requester?.side === 'WHITE' ? requester.playerToken : null,
    BLACK: requester?.side === 'BLACK' ? requester.playerToken : null,
  }

  const inviteTokens = {
    WHITE: null,
    BLACK: null,
  }

  if (payload.localHotseat) {
    for (const player of game.players) {
      tokens[player.side] = player.playerToken
    }
  }

  if (payload.includePlayerTokens) {
    for (const player of game.players) {
      inviteTokens[player.side] = player.playerToken
    }
  }

  sessionStore.setSession({
    sessionId: game.sessionId,
    opponent: payload.opponent,
    requesterSide: requester?.side || payload.color.toUpperCase(),
    localHotseat: payload.localHotseat,
    perspective: requester?.side || payload.color.toUpperCase(),
    tokens,
    inviteTokens,
    timeControl: payload.timeControl,
    playerNames: {
      WHITE: game.players.find((player) => player.side === 'WHITE')?.displayName || payload.whitePlayerName,
      BLACK: game.players.find((player) => player.side === 'BLACK')?.displayName || payload.blackPlayerName,
    },
  })

  await router.push({ name: 'game', params: { sessionId: game.sessionId } })
}

async function joinRemoteGame(payload) {
  const response = await joinGame(payload.sessionId, payload.playerToken)
  const game = response.data.game
  const requester = response.data.requester

  sessionStore.setSession({
    sessionId: game.sessionId,
    opponent: 'human',
    requesterSide: requester.side,
    localHotseat: false,
    perspective: requester.side,
    tokens: {
      WHITE: requester.side === 'WHITE' ? requester.playerToken : null,
      BLACK: requester.side === 'BLACK' ? requester.playerToken : null,
    },
    inviteTokens: {
      WHITE: null,
      BLACK: null,
    },
    timeControl: sessionStore.state.timeControl,
    playerNames: {
      WHITE: game.players.find((player) => player.side === 'WHITE')?.displayName || 'White',
      BLACK: game.players.find((player) => player.side === 'BLACK')?.displayName || 'Black',
    },
  })

  await router.push({ name: 'game', params: { sessionId: game.sessionId } })
}

async function refreshGames() {
  loadingGames.value = true
  gamesError.value = ''
  try {
    const response = await listGames()
    games.value = response.data.games || []
  } catch (error) {
    gamesError.value = error.message || 'No fue posible cargar las partidas'
  } finally {
    loadingGames.value = false
  }
}

async function maybeAutoJoinFromInvite() {
  const sessionId = typeof route.query.sessionId === 'string' ? route.query.sessionId : ''
  const playerToken = typeof route.query.playerToken === 'string' ? route.query.playerToken : ''

  if (!sessionId || !playerToken) {
    return
  }

  try {
    await joinRemoteGame({ sessionId, playerToken })
  } catch (error) {
    gamesError.value = error.message || 'No fue posible abrir la invitación compartida'
  }
}

async function resumeGame(sessionId) {
  sessionStore.setSession({
    sessionId,
    opponent: 'human',
    requesterSide: null,
    localHotseat: false,
    perspective: sessionStore.state.perspective,
    tokens: { WHITE: null, BLACK: null },
    inviteTokens: { WHITE: null, BLACK: null },
    playerNames: sessionStore.state.playerNames,
    timeControl: sessionStore.state.timeControl,
  })
  await router.push({ name: 'game', params: { sessionId } })
}

function shortSession(sessionId) {
  return sessionId.slice(0, 8)
}
</script>

<style lang="scss" scoped>
.home-view {
  padding-top: 1rem;
  display: grid;
  gap: 1rem;

  &__hero {
    padding: 1.6rem;
    display: flex;
    justify-content: space-between;
    gap: 1rem;
    flex-wrap: wrap;
    background:
      radial-gradient(circle at top right, rgba(255, 138, 102, 0.12), transparent 28%),
      linear-gradient(180deg, rgba(26, 38, 49, 0.94), rgba(18, 28, 36, 0.9));
  }

  &__copy {
    max-width: 46rem;
  }

  &__title {
    margin: 0.7rem 0 0.45rem;
    font-size: clamp(2rem, 4vw, 3.6rem);
    line-height: 0.95;
    letter-spacing: -0.05em;
  }

  &__text {
    margin: 0;
    color: var(--muted);
    max-width: 60ch;
  }

  &__meta {
    display: flex;
    gap: 0.75rem;
    align-items: flex-start;
    flex-wrap: wrap;
  }

  &__install {
    padding: 1rem 1.25rem;
    background: linear-gradient(180deg, rgba(23, 35, 46, 0.92), rgba(18, 28, 37, 0.88));
  }

  &__install-copy {
    h2,
    p {
      margin: 0;
    }

    h2 {
      font-size: 1.08rem;
      margin-bottom: 0.35rem;
    }

    p {
      color: var(--muted);
    }
  }

  &__lobby {
    padding: 1.25rem;
    display: grid;
    gap: 1rem;
  }

  &__lobby-head {
    display: flex;
    justify-content: space-between;
    gap: 1rem;
    align-items: center;
    flex-wrap: wrap;
  }

  &__section-title {
    margin: 0.55rem 0 0;
    font-size: 1.4rem;
    letter-spacing: -0.04em;
  }

  &__games {
    display: grid;
    gap: 0.9rem;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  }

  &__game-card {
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
    padding: 1rem;
    background: rgba(255, 255, 255, 0.04);
    display: grid;
    gap: 0.8rem;
  }

  &__game-meta {
    display: flex;
    justify-content: space-between;
    gap: 0.75rem;
    align-items: flex-start;

    h3 {
      margin: 0;
      font-size: 1rem;
    }
  }

  &__game-detail,
  &__empty {
    margin: 0;
    color: var(--muted);
  }

  &__game-actions {
    display: flex;
    justify-content: space-between;
    gap: 0.75rem;
    align-items: center;
  }
}
</style>
