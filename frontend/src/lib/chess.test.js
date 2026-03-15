import { describe, expect, it } from 'vitest'
import {
  legalMovesForSquare,
  orientSquares,
  parseFenBoard,
  pieceAlt,
  pieceSide,
  promotionOptions,
  squareColor,
} from './chess'

describe('chess helpers', () => {
  it('parses the initial fen into 64 squares', () => {
    const squares = parseFenBoard('rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1')
    expect(squares).toHaveLength(64)
    expect(squares.find((square) => square.square === 'a8')?.pieceCode).toBe('r')
    expect(squares.find((square) => square.square === 'e1')?.pieceCode).toBe('K')
  })

  it('orients squares for black perspective', () => {
    const squares = parseFenBoard('8/8/8/8/8/8/8/K6k w - - 0 1')
    const oriented = orientSquares(squares, 'BLACK')
    expect(oriented[0].square).toBe('h1')
    expect(oriented.at(-1)?.square).toBe('a8')
  })

  it('extracts legal moves for a selected square', () => {
    expect(legalMovesForSquare(['e2e4', 'e2e3', 'g1f3'], 'e2')).toEqual(['e2e4', 'e2e3'])
  })

  it('resolves piece side and alt labels', () => {
    expect(pieceSide('Q')).toBe('WHITE')
    expect(pieceSide('q')).toBe('BLACK')
    expect(pieceAlt('N')).toContain('White')
  })

  it('returns promotion choices for both colors', () => {
    expect(promotionOptions('WHITE').map((choice) => choice.code)).toEqual(['q', 'r', 'b', 'n'])
    expect(promotionOptions('BLACK')[0].pieceCode).toBe('q')
  })

  it('computes board square colors consistently', () => {
    expect(squareColor({ fileIndex: 0, rankIndex: 0 })).toBe('light')
    expect(squareColor({ fileIndex: 1, rankIndex: 0 })).toBe('dark')
    expect(squareColor({ fileIndex: 7, rankIndex: 7 })).toBe('light')
  })
})
