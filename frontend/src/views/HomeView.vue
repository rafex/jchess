<template lang="pug">
.page-shell.home-view
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
  padding-top: 1.5rem;
}
</style>
