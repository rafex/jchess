import { expect, test } from '@playwright/test'

const createdGame = {
  v: 1,
  type: 'game_created',
  data: {
    game: {
      sessionId: '11111111-1111-1111-1111-111111111111',
      status: 'ACTIVE',
      result: 'IN_PROGRESS',
      endReason: 'NONE',
      turn: 'WHITE',
      humanSide: 'WHITE',
      fen: '4k3/P7/8/8/8/8/8/4K3 w - - 0 1',
      version: 1,
      createdAt: '2026-03-15T00:00:00Z',
      updatedAt: '2026-03-15T00:00:00Z',
      players: [
        { playerId: 'p1', side: 'WHITE', participantType: 'HUMAN', displayName: 'Player', connected: false, playerToken: 'white-token' },
        { playerId: 'p2', side: 'BLACK', participantType: 'MACHINE', displayName: 'Bot', connected: false, playerToken: 'black-token' },
      ],
      moves: [],
      legalMovesEnglish: [],
      legalMovesSpanish: [],
      legalMovesUci: ['a7a8q', 'a7a8r', 'a7a8b', 'a7a8n'],
      boardAscii: '',
      pgn: '',
    },
    requester: {
      playerId: 'p1',
      side: 'WHITE',
      participantType: 'HUMAN',
      displayName: 'Player',
      connected: false,
      playerToken: 'white-token',
    },
  },
}

const afterPromotion = {
  v: 1,
  type: 'move_submitted',
  data: {
    sessionId: '11111111-1111-1111-1111-111111111111',
    status: 'ACTIVE',
    result: 'IN_PROGRESS',
    endReason: 'NONE',
    turn: 'WHITE',
    humanSide: 'WHITE',
    fen: 'Q3k3/8/8/8/8/8/8/4K3 w - - 0 2',
    version: 2,
    createdAt: '2026-03-15T00:00:00Z',
    updatedAt: '2026-03-15T00:00:10Z',
    players: [
      { playerId: 'p1', side: 'WHITE', participantType: 'HUMAN', displayName: 'Player', connected: false },
      { playerId: 'p2', side: 'BLACK', participantType: 'MACHINE', displayName: 'Bot', connected: false },
    ],
    moves: [
      { ply: 1, side: 'WHITE', submittedNotation: 'a8=Q', canonicalNotation: 'a8=Q', uci: 'a7a8q', fenBefore: '', fenAfter: '', playedAt: '2026-03-15T00:00:10Z' },
    ],
    legalMovesEnglish: [],
    legalMovesSpanish: [],
    legalMovesUci: [],
    boardAscii: '',
    pgn: '',
  },
}

test('creates a game and promotes a pawn from the board UI', async ({ page }) => {
  await page.route('**/api/v1/games?limit=12', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        v: 1,
        type: 'games_list',
        data: {
          games: [],
        },
      }),
    })
  })

  await page.route('**/api/v1/games', async (route) => {
    if (route.request().method() === 'POST') {
      await route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(createdGame) })
      return
    }
    await route.continue()
  })

  await page.route('**/api/v1/games/11111111-1111-1111-1111-111111111111', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        v: 1,
        type: 'game_state',
        data: createdGame.data.game,
      }),
    })
  })

  await page.route('**/api/v1/games/11111111-1111-1111-1111-111111111111/moves', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(afterPromotion) })
  })

  await page.goto('/')
  await page.getByRole('button', { name: 'Empezar a jugar' }).click()
  await expect(page).toHaveURL(/\/game\/11111111-1111-1111-1111-111111111111/)
  await expect(page.locator('button.board-square').filter({ hasText: 'a7' })).toBeVisible()

  await page.locator('button.board-square').filter({ hasText: 'a7' }).click()
  await page.locator('button.board-square').filter({ hasText: 'a8' }).click()
  await expect(page.getByRole('heading', { name: 'Elige promoción' })).toBeVisible()
  await page.getByRole('button', { name: 'Reina' }).click()
  await expect(page.getByText('a8=Q')).toBeVisible()
})

test('starts an offline local game and shows import-export tools', async ({ page }) => {
  await page.route('**/api/v1/games?limit=12', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        v: 1,
        type: 'games_list',
        data: {
          games: [
            {
              sessionId: '22222222-2222-2222-2222-222222222222',
              status: 'ACTIVE',
              result: 'IN_PROGRESS',
              turn: 'WHITE',
              whitePlayerName: 'Alice',
              blackPlayerName: 'Bob',
              moveCount: 12,
              createdAt: '2026-03-15T00:00:00Z',
              updatedAt: '2026-03-15T00:00:00Z',
            },
          ],
        },
      }),
    })
  })

  await page.goto('/')
  await expect(page.getByText('Alice vs Bob')).toBeVisible()
  await page.getByLabel('Rival').selectOption('offline-local')
  await page.getByRole('button', { name: 'Empezar a jugar' }).click()
  await expect(page).toHaveURL(/\/offline/)
  await expect(page.getByRole('heading', { name: 'Importar / Exportar' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Copiar FEN' })).toBeVisible()
})
