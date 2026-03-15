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
  StartGamePanel(@start='startGame')
</template>

<script setup>
import { useRouter } from 'vue-router'
import StartGamePanel from '../components/StartGamePanel.vue'
import { createGame } from '../lib/api'
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

  if (payload.localHotseat) {
    for (const player of game.players) {
      tokens[player.side] = player.playerToken
    }
  }

  sessionStore.setSession({
    sessionId: game.sessionId,
    opponent: payload.opponent,
    localHotseat: payload.localHotseat,
    perspective: payload.color.toUpperCase(),
    tokens,
    playerNames: {
      WHITE: game.players.find((player) => player.side === 'WHITE')?.displayName || payload.whitePlayerName,
      BLACK: game.players.find((player) => player.side === 'BLACK')?.displayName || payload.blackPlayerName,
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
}
</style>
