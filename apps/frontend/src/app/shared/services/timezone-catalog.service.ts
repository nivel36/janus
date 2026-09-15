/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { DestroyRef, Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

import { TimezoneOption } from '../models/timezone-option.model';
import { AutocompleteSearchState } from '../state/autocomplete-search.state';

const MAX_SEARCH_RESULTS = 50;

/**
 * Application-wide catalog and autocomplete adapter for IANA timezones.
 *
 * The catalog is created once by Angular's root injector. Offsets intentionally
 * describe the instant at which the singleton is created, including daylight
 * saving time where applicable.
 */
@Injectable({ providedIn: 'root' })
export class TimezoneCatalog {
  private readonly now = new Date();

  private readonly collator = new Intl.Collator(undefined, {
    usage: 'search',
    sensitivity: 'base',
  });

  readonly options: readonly TimezoneOption[] = Object.freeze(
    Intl.supportedValuesOf('timeZone').map((zoneId) =>
      Object.freeze({
        zoneId,
        literal: `${zoneId} (${this.getUtcOffsetLiteral(zoneId)})`,
      }),
    ),
  );

  private readonly optionsByZoneId = new Map(this.options.map((option) => [option.zoneId, option]));

  readonly displayWith = (option: TimezoneOption): string => option.literal;

  readonly valueWith = (option: TimezoneOption): string => option.zoneId;

  readonly resolve = (zoneId: string): TimezoneOption | null =>
    this.optionsByZoneId.get(zoneId) ?? null;

  readonly search = (query: string): Observable<TimezoneOption[]> => {
    const normalizedQuery = query.trim();
    if (!normalizedQuery) {
      return of([]);
    }

    return of(
      this.options
        .filter(
          (option) =>
            this.containsIanaIdentifier(option.zoneId, normalizedQuery) ||
            this.contains(option.literal, normalizedQuery),
        )
        .slice(0, MAX_SEARCH_RESULTS),
    );
  };

  createSearchState(destroyRef: DestroyRef): AutocompleteSearchState<TimezoneOption> {
    return new AutocompleteSearchState(this.search, destroyRef);
  }

  private containsIanaIdentifier(zoneId: string, query: string): boolean {
    return zoneId.toLowerCase().includes(query.toLowerCase());
  }

  private contains(value: string, query: string): boolean {
    if (query.length > value.length) {
      return false;
    }

    for (let index = 0; index <= value.length - query.length; index += 1) {
      if (this.collator.compare(value.slice(index, index + query.length), query) === 0) {
        return true;
      }
    }

    return false;
  }

  private getUtcOffsetLiteral(zoneId: string): string {
    const utcOffsetPart = new Intl.DateTimeFormat('en-US', {
      timeZone: zoneId,
      timeZoneName: 'shortOffset',
    })
      .formatToParts(this.now)
      .find((part) => part.type === 'timeZoneName')?.value;

    return utcOffsetPart?.replace('GMT', 'UTC') ?? 'UTC';
  }
}
