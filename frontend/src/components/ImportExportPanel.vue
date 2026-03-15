<template lang="pug">
.glass-card.panel.import-export
  h2.panel__title Importar / Exportar
  .import-export__actions
    button.button.button--tonal(type='button' @click='copy("fen", fen)') Copiar FEN
    button.button.button--tonal(type='button' @click='copy("pgn", pgn)') Copiar PGN
    button.button.button--secondary(type='button' :disabled='!pgn' @click='download("pgn", pgn, "jchess-game.pgn")') Descargar PGN
    button.button.button--secondary(type='button' :disabled='!fen' @click='download("fen", fen, "jchess-position.fen")') Descargar FEN
  label.field(v-if='allowImport')
    span.field__label FEN o PGN
    textarea.field__control.import-export__textarea(v-model='draft' rows='6' placeholder='Pega FEN o PGN')
  .import-export__actions(v-if='allowImport')
    button.button.button--secondary(type='button' :disabled='!draft.trim()' @click='$emit("import-fen", draft.trim())') Importar FEN
    button.button.button--secondary(type='button' :disabled='!draft.trim()' @click='$emit("import-pgn", draft.trim())') Importar PGN
    button.button.button--tonal(type='button' :disabled='!draft.trim()' @click='draft = ""') Limpiar
  p.import-export__hint(v-if='hint') {{ hint }}
</template>

<script setup>
import { ref } from 'vue'

defineEmits(['import-fen', 'import-pgn'])

const props = defineProps({
  fen: {
    type: String,
    default: '',
  },
  pgn: {
    type: String,
    default: '',
  },
  allowImport: {
    type: Boolean,
    default: false,
  },
  hint: {
    type: String,
    default: '',
  },
})

const draft = ref('')

async function copy(type, value) {
  if (!value) {
    return
  }
  await navigator.clipboard.writeText(value)
}

function download(type, value, filename) {
  if (!value) {
    return
  }

  const blob = new Blob([value], { type: type === 'pgn' ? 'application/x-chess-pgn' : 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  anchor.click()
  URL.revokeObjectURL(url)
}
</script>

<style lang="scss" scoped>
.import-export {
  display: grid;
  gap: 0.9rem;
}

.import-export__actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.import-export__textarea {
  min-height: 9rem;
  resize: vertical;
}

.import-export__hint {
  margin: 0;
  color: var(--muted);
}
</style>
