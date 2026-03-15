import { reactive } from 'vue'

const STORAGE_KEY = 'jchess.frontend.session'

const state = reactive({
  sessionId: null,
  opponent: 'machine',
  requesterSide: null,
  tokens: {
    WHITE: null,
    BLACK: null,
  },
  inviteTokens: {
    WHITE: null,
    BLACK: null,
  },
  perspective: 'WHITE',
  localHotseat: false,
  playerNames: {
    WHITE: 'White',
    BLACK: 'Black',
  },
})

function persist() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
}

function restore() {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) {
    return
  }

  try {
    const parsed = JSON.parse(raw)
    Object.assign(state, parsed)
  } catch {
    localStorage.removeItem(STORAGE_KEY)
  }
}

restore()

export function useSessionStore() {
  return {
    state,
    setSession(payload) {
      state.sessionId = payload.sessionId
      state.opponent = payload.opponent
      state.requesterSide = payload.requesterSide
      state.tokens = { ...state.tokens, ...payload.tokens }
      state.inviteTokens = { ...state.inviteTokens, ...(payload.inviteTokens || {}) }
      state.perspective = payload.perspective
      state.localHotseat = payload.localHotseat
      state.playerNames = { ...state.playerNames, ...payload.playerNames }
      persist()
    },
    setPerspective(side) {
      state.perspective = side
      persist()
    },
    clear() {
      state.sessionId = null
      state.opponent = 'machine'
      state.requesterSide = null
      state.tokens = { WHITE: null, BLACK: null }
      state.inviteTokens = { WHITE: null, BLACK: null }
      state.perspective = 'WHITE'
      state.localHotseat = false
      state.playerNames = { WHITE: 'White', BLACK: 'Black' }
      persist()
    },
  }
}
