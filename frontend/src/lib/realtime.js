function resolveWebSocketUrl() {
  if (import.meta.env.VITE_WS_BASE_URL) {
    return import.meta.env.VITE_WS_BASE_URL
  }

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws`
}

export function createGameSocket({ sessionId, playerToken, onEnvelope, onStatusChange }) {
  let socket = null
  let heartbeat = null
  let reconnectTimer = null
  let closedManually = false

  function connect() {
    if (!sessionId || !playerToken) {
      return
    }

    clearReconnect()
    onStatusChange?.('connecting')
    socket = new WebSocket(resolveWebSocketUrl())

    socket.addEventListener('open', () => {
      onStatusChange?.('connected')
    })

    socket.addEventListener('message', (event) => {
      let payload = null
      try {
        payload = JSON.parse(event.data)
      } catch {
        return
      }

      if (payload.type === 'connected') {
        socket.send(JSON.stringify({
          type: 'join_game',
          sessionId,
          playerToken,
        }))
        heartbeat = window.setInterval(() => {
          if (socket?.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify({ type: 'heartbeat' }))
          }
        }, 25000)
      }

      onEnvelope?.(payload)
    })

    socket.addEventListener('close', () => {
      onStatusChange?.('disconnected')
      clearHeartbeat()
      if (!closedManually) {
        reconnectTimer = window.setTimeout(() => {
          connect()
        }, 2000)
      }
    })

    socket.addEventListener('error', () => {
      onStatusChange?.('error')
    })
  }

  function send(type, data = {}) {
    if (!socket || socket.readyState !== WebSocket.OPEN) {
      return false
    }
    socket.send(JSON.stringify({ type, sessionId, playerToken, ...data }))
    return true
  }

  function disconnect() {
    closedManually = true
    clearReconnect()
    clearHeartbeat()
    socket?.close()
  }

  function clearHeartbeat() {
    if (heartbeat) {
      window.clearInterval(heartbeat)
      heartbeat = null
    }
  }

  function clearReconnect() {
    if (reconnectTimer) {
      window.clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
  }

  return {
    connect,
    send,
    disconnect,
  }
}
