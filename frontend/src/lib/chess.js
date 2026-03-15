export const FILES = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h']
export const RANKS = ['8', '7', '6', '5', '4', '3', '2', '1']

const PIECES = {
  p: '♟',
  r: '♜',
  n: '♞',
  b: '♝',
  q: '♛',
  k: '♚',
  P: '♙',
  R: '♖',
  N: '♘',
  B: '♗',
  Q: '♕',
  K: '♔',
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
  return (square.fileIndex + (7 - square.rankIndex)) % 2 === 0 ? 'light' : 'dark'
}

export function pieceSide(pieceCode) {
  if (!pieceCode) {
    return null
  }
  return pieceCode === pieceCode.toUpperCase() ? 'WHITE' : 'BLACK'
}

export function pieceGlyph(pieceCode) {
  return pieceCode ? PIECES[pieceCode] : ''
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
    pieceGlyph: pieceGlyph(pieceCode),
    side: pieceSide(pieceCode),
  }
}
