<template lang="pug">
.start-panel.glass-card.animate__animated.animate__fadeInUp
  .start-panel__intro
    span.pill Nueva partida
    h1.start-panel__title Juega ya sobre el motor real
    p.start-panel__copy
      | Crea una partida contra otra persona en modo local o contra la máquina,
      |  elige color directo o usa la dinámica de adivinar la mano.

  form.start-panel__form(@submit.prevent='handleSubmit')
    .start-panel__grid
      label.field
        span.field__label Rival
        select.field__control(v-model='form.opponent')
          option(value='machine') Máquina
          option(value='human') Humano local
          option(value='human-remote') Humano remoto
      label.field(v-if='form.opponent === "machine"')
        span.field__label Motor remoto
        select.field__control(v-model='form.llm')
          option(value='') Sin LLM
          option(value='groq') Groq
          option(value='deepseek') DeepSeek
      label.field
        span.field__label Blancas
        input.field__control(v-model='form.whitePlayerName' placeholder='Alice')
      label.field
        span.field__label Negras
        input.field__control(v-model='form.blackPlayerName' placeholder='Bot o Bob')

    .choice-mode
      button.button.button--secondary.choice-mode__chip(
        type='button'
        :class='{ "choice-mode__chip--active": form.colorMode === "direct" }'
        @click='form.colorMode = "direct"'
      ) Elegir color
      button.button.button--secondary.choice-mode__chip(
        type='button'
        :class='{ "choice-mode__chip--active": form.colorMode === "guess" }'
        @click='activateGuessMode'
      ) Adivinar la mano

    .color-direct(v-if='form.colorMode === "direct"')
      button.button.button--secondary.color-direct__choice(
        type='button'
        :class='{ "color-direct__choice--active": form.color === "white" }'
        @click='form.color = "white"'
      ) Blancas
      button.button.button--secondary.color-direct__choice(
        type='button'
        :class='{ "color-direct__choice--active": form.color === "black" }'
        @click='form.color = "black"'
      ) Negras

    .guess-board(v-else)
      p.guess-board__copy
        | Elige una mano. Detrás hay una pieza blanca y una negra.
      .guess-board__hands
        button.button.button--secondary.guess-board__hand(
          v-for='hand in hands'
          :key='hand.id'
          type='button'
          @click='pickHand(hand.id)'
        )
          span.guess-board__emoji {{ revealed && selectedHand === hand.id ? hand.resultEmoji : '✊' }}
          span {{ hand.label }}
      p.guess-board__result(v-if='revealed')
        | Te tocaron las
        strong  {{ assignedColorLabel }}

    .start-panel__footer
      .start-panel__status(v-if='error') {{ error }}
      button.button.button--primary(type='submit' :disabled='loading || !resolvedColor')
        span(v-if='loading') Creando partida...
        span(v-else) Empezar a jugar
    CubesLoader.start-panel__loader(v-if='loading' compact label='Preparando el tablero...')

  .start-panel__divider
    span o si ya tienes acceso

  form.start-panel__join(@submit.prevent='handleJoin')
    .start-panel__grid
      label.field
        span.field__label Sesión remota
        input.field__control(v-model='joinForm.sessionId' placeholder='UUID de la sesión')
      label.field
        span.field__label Token del jugador
        input.field__control(v-model='joinForm.playerToken' placeholder='Token de acceso')
    .start-panel__footer
      .start-panel__status(v-if='joinError') {{ joinError }}
      button.button.button--tonal(type='submit' :disabled='joining || !joinForm.sessionId || !joinForm.playerToken')
        span(v-if='joining') Uniéndome...
        span(v-else) Unirme a partida remota
    CubesLoader.start-panel__loader(v-if='joining' compact label='Conectando a la mesa...')
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import CubesLoader from './CubesLoader.vue'

const emit = defineEmits(['start', 'join'])

const loading = ref(false)
const error = ref('')
const joining = ref(false)
const joinError = ref('')
const selectedHand = ref(null)
const hiddenWhiteHand = ref(Math.random() > 0.5 ? 'left' : 'right')
const revealed = ref(false)

const form = reactive({
  opponent: 'machine',
  llm: '',
  whitePlayerName: 'Player',
  blackPlayerName: 'Bot',
  colorMode: 'direct',
  color: 'white',
})

const joinForm = reactive({
  sessionId: '',
  playerToken: '',
})

const hands = computed(() => [
  {
    id: 'left',
    label: 'Mano izquierda',
    resultEmoji: hiddenWhiteHand.value === 'left' ? '♔' : '♚',
  },
  {
    id: 'right',
    label: 'Mano derecha',
    resultEmoji: hiddenWhiteHand.value === 'right' ? '♔' : '♚',
  },
])

const resolvedColor = computed(() => {
  if (form.colorMode === 'direct') {
    return form.color
  }
  if (!selectedHand.value) {
    return ''
  }
  return hiddenWhiteHand.value === selectedHand.value ? 'white' : 'black'
})

const assignedColorLabel = computed(() => (resolvedColor.value === 'white' ? 'blancas' : 'negras'))

function activateGuessMode() {
  form.colorMode = 'guess'
  selectedHand.value = null
  revealed.value = false
  hiddenWhiteHand.value = Math.random() > 0.5 ? 'left' : 'right'
}

function pickHand(handId) {
  selectedHand.value = handId
  revealed.value = true
}

async function handleSubmit() {
  loading.value = true
  error.value = ''

  try {
        await emit('start', {
      opponent: form.opponent === 'human-remote' ? 'human' : form.opponent,
      llm: form.llm || null,
      color: resolvedColor.value,
      whitePlayerName: form.whitePlayerName || 'White',
      blackPlayerName: form.blackPlayerName || (form.opponent === 'machine' ? 'Machine' : 'Black'),
      localHotseat: form.opponent === 'human',
      includePlayerTokens: form.opponent === 'human-remote',
    })
  } catch (submitError) {
    error.value = submitError.message || 'No fue posible crear la partida'
  } finally {
    loading.value = false
  }
}

async function handleJoin() {
  joining.value = true
  joinError.value = ''

  try {
    await emit('join', {
      sessionId: joinForm.sessionId.trim(),
      playerToken: joinForm.playerToken.trim(),
    })
  } catch (submitError) {
    joinError.value = submitError.message || 'No fue posible unirse a la partida'
  } finally {
    joining.value = false
  }
}
</script>

<style lang="scss" scoped>
.start-panel {
  padding: 2rem;
  display: grid;
  gap: 1.75rem;
  background: linear-gradient(180deg, rgba(24, 34, 45, 0.94), rgba(18, 28, 36, 0.9));

  &__title {
    margin: 0.5rem 0 0.35rem;
    font-size: clamp(2rem, 4vw, 3.7rem);
    line-height: 0.95;
    letter-spacing: -0.05em;
  }

  &__copy {
    margin: 0;
    max-width: 60ch;
    color: var(--muted);
    font-size: 1.02rem;
  }

  &__form {
    display: grid;
    gap: 1.25rem;
  }

  &__grid {
    display: grid;
    gap: 1rem;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  }

  &__footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 1rem;
    flex-wrap: wrap;
  }

  &__status {
    color: var(--danger);
    font-weight: 600;
  }

  &__loader {
    justify-self: center;
  }

  &__divider {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    color: var(--muted);
    font-size: 0.92rem;

    &::before,
    &::after {
      content: '';
      flex: 1;
      height: 1px;
      background: var(--line);
    }
  }

  &__join {
    display: grid;
    gap: 1.25rem;
  }
}

.field {
  display: grid;
  gap: 0.5rem;

  &__label {
    color: var(--muted);
    font-size: 0.9rem;
    font-weight: 600;
  }

  &__control {
    width: 100%;
    border-radius: var(--radius-sm);
    border: 1px solid var(--line);
    background: rgba(255, 255, 255, 0.05);
    color: var(--text);
    padding: 0.9rem 1rem;
    transition: border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;

    &:focus {
      outline: none;
      border-color: rgba(255, 138, 102, 0.55);
      box-shadow: 0 0 0 4px rgba(255, 138, 102, 0.16);
      background: rgba(255, 255, 255, 0.08);
    }
  }
}

.choice-mode,
.color-direct,
.guess-board__hands {
  display: flex;
  gap: 0.8rem;
  flex-wrap: wrap;
}

.choice-mode__chip--active,
.color-direct__choice--active {
  border-color: rgba(255, 138, 102, 0.45);
  background: rgba(255, 138, 102, 0.14);
}

.guess-board {
  display: grid;
  gap: 0.9rem;

  &__copy,
  &__result {
    margin: 0;
  }

  &__hand {
    min-width: 180px;
    display: grid;
    gap: 0.55rem;
    justify-items: center;
  }

  &__emoji {
    font-size: 2rem;
  }

  &__result strong {
    color: var(--primary);
  }
}
</style>
