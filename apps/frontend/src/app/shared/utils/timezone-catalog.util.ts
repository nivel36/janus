/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { TimezoneOption } from '../models/timezone-option.model';

/**
 * Builds the full timezone catalog used by autocomplete controls.
 */
export function createTimezoneCatalog(): TimezoneOption[] {
  return Intl.supportedValuesOf('timeZone').map((zoneId) => ({
    zoneId,
    literal: `${zoneId} (${getUtcOffsetLiteral(zoneId)})`,
  }));
}

/**
 * Computes a human-readable UTC offset label for a timezone.
 */
export function getUtcOffsetLiteral(zoneId: string): string {
  const utcOffsetPart = new Intl.DateTimeFormat('en-US', {
    timeZone: zoneId,
    timeZoneName: 'shortOffset',
  })
    .formatToParts(new Date())
    .find((part) => part.type === 'timeZoneName')?.value;

  return utcOffsetPart?.replace('GMT', 'UTC') ?? 'UTC';
}

/**
 * Finds the timezone option corresponding to a stored timezone id.
 */
export function resolveTimezoneByZoneId(
  timezoneCatalog: TimezoneOption[],
  zoneId: string,
): TimezoneOption | null {
  return timezoneCatalog.find((option) => option.zoneId === zoneId) ?? null;
}
