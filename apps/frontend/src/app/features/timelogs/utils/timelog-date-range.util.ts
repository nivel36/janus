/** SPDX-License-Identifier: Apache-2.0 */
import { TimelogSearchRange } from '../models/timelog-search-range';

function calendarDate(value: string): Date {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) throw new Error('Invalid date');
  const date = new Date(`${value}T00:00:00Z`);
  if (!Number.isFinite(date.getTime()) || date.toISOString().slice(0, 10) !== value) {
    throw new Error('Invalid date');
  }
  return date;
}

/** First instant of a calendar day, including zones where midnight is skipped. */
function startOfDay(date: Date, timezone: string): string {
  const formatter = new Intl.DateTimeFormat('en-GB', {
    timeZone: timezone, year: 'numeric', month: '2-digit', day: '2-digit',
  });
  const target = date.toISOString().slice(0, 10);
  const dateAt = (instant: number): string => {
    const parts = formatter.formatToParts(instant);
    const part = (type: string) => parts.find((item) => item.type === type)!.value;
    return `${part('year').padStart(4, '0')}-${part('month')}-${part('day')}`;
  };
  // All IANA offsets fit within this window. Search calendar boundaries rather
  // than adding 24 hours, since days can be shorter or longer across DST.
  let low = date.getTime() - 36 * 60 * 60 * 1000;
  let high = date.getTime() + 36 * 60 * 60 * 1000;
  while (low < high) {
    const middle = Math.floor((low + high) / 2);
    if (dateAt(middle) < target) low = middle + 1;
    else high = middle;
  }
  return new Date(low).toISOString();
}

export function timelogDateRange(from: string, to: string, timezone: string): TimelogSearchRange {
  const firstDay = calendarDate(from);
  const lastDay = calendarDate(to);
  if (from > to) throw new Error('Invalid range');
  lastDay.setUTCDate(lastDay.getUTCDate() + 1);
  const start = startOfDay(firstDay, timezone);
  const end = startOfDay(lastDay, timezone);
  if (start >= end) throw new Error('Empty range');
  return { start, end };
}
