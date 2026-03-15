import bb from '../assets/pieces/bb.svg'
import bk from '../assets/pieces/bk.svg'
import bn from '../assets/pieces/bn.svg'
import bp from '../assets/pieces/bp.svg'
import bq from '../assets/pieces/bq.svg'
import br from '../assets/pieces/br.svg'
import wb from '../assets/pieces/wb.svg'
import wk from '../assets/pieces/wk.svg'
import wn from '../assets/pieces/wn.svg'
import wp from '../assets/pieces/wp.svg'
import wq from '../assets/pieces/wq.svg'
import wr from '../assets/pieces/wr.svg'

export const FILES = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h']
export const RANKS = ['8', '7', '6', '5', '4', '3', '2', '1']

const PIECES = {
  p: { src: bp, alt: 'Black pawn' },
  r: { src: br, alt: 'Black rook' },
  n: { src: bn, alt: 'Black knight' },
  b: { src: bb, alt: 'Black bishop' },
  q: { src: bq, alt: 'Black queen' },
  k: { src: bk, alt: 'Black king' },
  P: { src: wp, alt: 'White pawn' },
  R: { src: wr, alt: 'White rook' },
  N: { src: wn, alt: 'White knight' },
  B: { src: wb, alt: 'White bishop' },
  Q: { src: wq, alt: 'White queen' },
  K: { src: wk, alt: 'White king' },
}

export function parseFenBoard(fen) {
  const boardFen = fen.split(' ')[0]
  const ranks = boardFen.split('/')

  return ranks.flatMap((rank, rankIndex) => {
    const squares = []
    let fileIndex = 0

    for (const token of rank) {
      if (/\d/.test(token)) {
        const count = Number(token)
        for (let offset = 0; offset < count; offset += 1) {
          squares.push(squareRecord(fileIndex, rankIndex, null))
          fileIndex += 1
        }
      } else {
        squares.push(squareRecord(fileIndex, rankIndex, token))
        fileIndex += 1
      }
    }

    return squares
  })
}

export function orientSquares(squares, perspective) {
  if (perspective === 'BLACK') {
    return [...squares].sort((left, right) => right.rankIndex - left.rankIndex || right.fileIndex - left.fileIndex)
  }

  return [...squares].sort((left, right) => left.rankIndex - right.rankIndex || left.fileIndex - right.fileIndex)
}

export function squareColor(square) {
  return (square.fileIndex + square.rankIndex) % 2 === 0 ? 'light' : 'dark'
}

export function pieceSide(pieceCode) {
  if (!pieceCode) {
    return null
  }
  return pieceCode === pieceCode.toUpperCase() ? 'WHITE' : 'BLACK'
}

export function pieceAsset(pieceCode) {
  return pieceCode ? PIECES[pieceCode]?.src || '' : ''
}

export function pieceAlt(pieceCode) {
  return pieceCode ? PIECES[pieceCode]?.alt || '' : ''
}

export function promotionOptions(side) {
  const color = side === 'BLACK' ? 'b' : 'w'
  return [
    { code: 'q', label: 'Reina', pieceCode: `${color === 'w' ? 'Q' : 'q'}`, asset: pieceAsset(color === 'w' ? 'Q' : 'q') },
    { code: 'r', label: 'Torre', pieceCode: `${color === 'w' ? 'R' : 'r'}`, asset: pieceAsset(color === 'w' ? 'R' : 'r') },
    { code: 'b', label: 'Alfil', pieceCode: `${color === 'w' ? 'B' : 'b'}`, asset: pieceAsset(color === 'w' ? 'B' : 'b') },
    { code: 'n', label: 'Caballo', pieceCode: `${color === 'w' ? 'N' : 'n'}`, asset: pieceAsset(color === 'w' ? 'N' : 'n') },
  ]
}

export function legalMovesForSquare(legalMovesUci, square) {
  return legalMovesUci.filter((move) => move.startsWith(square))
}

function squareRecord(fileIndex, rankIndex, pieceCode) {
  const square = `${FILES[fileIndex]}${RANKS[rankIndex]}`
  return {
    key: square,
    square,
    fileIndex,
    rankIndex,
    pieceCode,
    pieceAsset: pieceAsset(pieceCode),
    pieceAlt: pieceAlt(pieceCode),
    side: pieceSide(pieceCode),
  }
}
