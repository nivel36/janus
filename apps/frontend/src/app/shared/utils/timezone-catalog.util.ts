/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { TimezoneOption } from '../models/timezone-option.model';

/**
 * Builds a catalog of timezones suitable for autocomplete controls.
 *
 * <p>
 * This function retrieves all supported IANA timezone identifiers from the
 * runtime environment via {@link Intl.supportedValuesOf} and transforms them
 * into {@link TimezoneOption} objects.
 * </p>
 *
 * <p>
 * The UTC offset is dynamically computed using {@link getUtcOffsetLiteral}, so it
 * reflects daylight saving time when applicable.
 * </p>
 *
 * @returns an array of {@link TimezoneOption} representing all supported timezones
 */
export function createTimezoneCatalog(): TimezoneOption[] {
  return Intl.supportedValuesOf('timeZone').map((zoneId) => ({
    zoneId,
    literal: `${zoneId} (${getUtcOffsetLiteral(zoneId)})`,
  }));
}

/**
 * Computes a human-readable UTC offset string for a given timezone.
 *
 * <p>
 * The result is normalized to use the {@code UTC} prefix instead of {@code GMT}
 * (e.g. {@code UTC+1}, {@code UTC-5}). If the offset cannot be determined, it
 * falls back to {@code UTC}.
 * </p>
 *
 * <p>
 * Note that the returned value reflects the offset at the current date, so it may
 * vary depending on daylight saving time.
 * </p>
 *
 * @param zoneId the IANA timezone identifier (e.g. {@code Europe/Paris})
 * @returns a string representing the UTC offset (e.g. {@code UTC+2}), or {@code UTC} if unavailable
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
 * Resolves a {@link TimezoneOption} from a catalog using its timezone identifier.
 *
 * @param timezoneCatalog the list of available {@link TimezoneOption}
 * @param zoneId the IANA timezone identifier to search for
 * @returns the matching {@link TimezoneOption}, or {@code null} if not found
 */
export function resolveTimezoneByZoneId(
  timezoneCatalog: TimezoneOption[],
  zoneId: string,
): TimezoneOption | null {
  return timezoneCatalog.find((option) => option.zoneId === zoneId) ?? null;
}
