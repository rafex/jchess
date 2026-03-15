import { describe, expect, it } from 'vitest'
import { formatClock, parseTimeControl } from './timeControl'

describe('time control helpers', () => {
  it('parses minutes and increment from a compact control', () => {
    expect(parseTimeControl('10+5')).toEqual({
      minutes: 10,
      incrementSeconds: 5,
      initialMs: 600000,
    })
  })

  it('falls back to a sane default control', () => {
    expect(parseTimeControl('')).toEqual({
      minutes: 5,
      incrementSeconds: 0,
      initialMs: 300000,
    })
  })

  it('formats remaining time as mm:ss', () => {
    expect(formatClock(301000)).toBe('5:01')
    expect(formatClock(999)).toBe('0:01')
    expect(formatClock(0)).toBe('0:00')
  })
})
