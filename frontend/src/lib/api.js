const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
    ...options,
  })

  const payload = await response.json().catch(() => ({}))

  if (!response.ok || payload.error) {
    throw new Error(payload?.error?.message || `HTTP ${response.status}`)
  }

  return payload
}

export async function createGame({
  color,
  opponent,
  llm,
  whitePlayerName,
  blackPlayerName,
  localHotseat = false,
  includePlayerTokens = false,
}) {
  return request('/api/v1/games', {
    method: 'POST',
    body: JSON.stringify({
      color,
      opponent,
      llm,
      whitePlayerName,
      blackPlayerName,
      localHotseat,
      includePlayerTokens,
    }),
  })
}

export async function loadGame(sessionId) {
  return request(`/api/v1/games/${sessionId}`)
}

export async function joinGame(sessionId, playerToken) {
  return request(`/api/v1/games/${sessionId}/join`, {
    method: 'POST',
    body: JSON.stringify({
      playerToken,
    }),
  })
}

export async function submitMove(sessionId, playerToken, from, to, promotion = null) {
  return request(`/api/v1/games/${sessionId}/moves`, {
    method: 'POST',
    body: JSON.stringify({
      playerToken,
      from,
      to,
      promotion,
    }),
  })
}

export async function resignGame(sessionId, playerToken) {
  return request(`/api/v1/games/${sessionId}/resign`, {
    method: 'POST',
    body: JSON.stringify({
      playerToken,
    }),
  })
}

export async function loadThemes() {
  return request('/api/v1/themes')
}
