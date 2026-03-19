/**
 * SPDX-License-Identifier: Apache-2.0
 */

import { Pipe, PipeTransform } from '@angular/core';
import { Duration } from '../../features/timelogs/models/duration';

/**
 * Transforms a {@link Duration} value into a human-readable string.
 *
 * The formatted output is based on the numeric fields of the duration:
 * - `hours`
 * - `minutes`
 * - `seconds`
 *
 * The `iso8601` field is part of the {@link Duration} model, but it is not used
 * by this pipe for formatting.
 *
 * Output rules:
 * - If `seconds` is greater than `0`, the result includes hours, minutes, and seconds.
 * - Otherwise, if `minutes` is greater than `0`, the result includes hours and minutes.
 * - Otherwise, only hours are included.
 *
 * Examples:
 * - `{ hours: 2, minutes: 15, seconds: 30, iso8601: 'PT2H15M30S' }` → `2h 15m 30s`
 * - `{ hours: 1, minutes: 45, seconds: 0, iso8601: 'PT1H45M' }` → `1h 45m`
 * - `{ hours: 3, minutes: 0, seconds: 0, iso8601: 'PT3H' }` → `3h`
 *
 * If the input is `null` or `undefined`, an empty string is returned.
 */
@Pipe({
  name: 'duration',
  standalone: true,
})
export class DurationPipe implements PipeTransform {
  /**
   * Formats a {@link Duration} object into a human-readable string.
   *
   * Only the `hours`, `minutes`, and `seconds` fields are used to build the
   * returned value. The `iso8601` field is ignored by this transformation.
   *
   * @param duration The duration to format.
   * @returns A formatted duration string, or an empty string when the input is
   * `null` or `undefined`.
   */
  transform(duration?: Duration | null): string {
    if (!duration) {
      return '';
    }

    const { hours, minutes, seconds } = duration;

    if (seconds > 0) {
      return `${hours}h ${minutes}m ${seconds}s`;
    }

    if (minutes > 0) {
      return `${hours}h ${minutes}m`;
    }

    return `${hours}h`;
  }
}
