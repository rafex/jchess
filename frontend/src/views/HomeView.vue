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
</template>

<script setup>
import { useRouter } from 'vue-router'
import StartGamePanel from '../components/StartGamePanel.vue'
import { createGame, joinGame } from '../lib/api'
import { useSessionStore } from '../lib/sessionStore'

const router = useRouter()
const sessionStore = useSessionStore()

async function startGame(payload) {
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
    playerNames: {
      WHITE: game.players.find((player) => player.side === 'WHITE')?.displayName || 'White',
      BLACK: game.players.find((player) => player.side === 'BLACK')?.displayName || 'Black',
    },
  })

  await router.push({ name: 'game', params: { sessionId: game.sessionId } })
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
}
</style>
