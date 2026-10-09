import { describe, expect, it } from 'vitest';
import { timelogDateRange } from './timelog-date-range.util';

describe('timelogDateRange', () => {
  it('includes the last day in the selected timezone', () => {
    expect(timelogDateRange('2026-10-01', '2026-10-03', 'Europe/Madrid')).toEqual({
      start: '2026-09-30T22:00:00.000Z', end: '2026-10-03T22:00:00.000Z',
    });
  });
  it.each([
    ['2026-03-29', '2026-03-28T23:00:00.000Z', '2026-03-29T22:00:00.000Z'],
    ['2026-10-25', '2026-10-24T22:00:00.000Z', '2026-10-25T23:00:00.000Z'],
    ['2018-11-04', '2018-11-04T03:00:00.000Z', '2018-11-05T02:00:00.000Z', 'America/Sao_Paulo'],
  ])('handles daylight savings boundaries: %s', (day, start, end, zone = 'Europe/Madrid') => {
    expect(timelogDateRange(day, day, zone ?? 'Europe/Madrid')).toEqual({ start, end });
  });
  it.each([['2026-02-30', '2026-03-01'], ['2026-10-03', '2026-10-01'], ['', '2026-10-01']])(
    'rejects invalid dates or reversed intervals: %s', (from, to) => {
      expect(() => timelogDateRange(from, to, 'UTC')).toThrow();
    },
  );
});
