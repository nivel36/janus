/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { firstValueFrom } from 'rxjs';

import { TimezoneCatalog } from './timezone-catalog.service';

describe('TimezoneCatalog', () => {
  let catalog: TimezoneCatalog;

  beforeEach(() => {
    catalog = new TimezoneCatalog();
  });

  it('creates options from the timezones supported by Intl', () => {
    expect(catalog.options.map((option) => option.zoneId)).toEqual(
      Intl.supportedValuesOf('timeZone'),
    );
    expect(catalog.options.every((option) => option.literal.startsWith(option.zoneId))).toBe(true);
  });

  it('resolves known timezone identifiers', () => {
    const option = catalog.options[0];

    expect(catalog.resolve(option.zoneId)).toBe(option);
    expect(catalog.resolve('Unknown/Timezone')).toBeNull();
  });

  it('searches without case or diacritic sensitivity', async () => {
    const searchableCatalog = catalog as unknown as {
      contains(value: string, query: string): boolean;
    };

    expect(searchableCatalog.contains('Test/Québec', 'QUEBEC')).toBe(true);

    const option = catalog.options[0];
    await expect(firstValueFrom(catalog.search(option.zoneId.toUpperCase()))).resolves.toContain(
      option,
    );
  });

  it('matches IANA identifiers independently of the browser locale', async () => {
    const catalogWithTurkishCollation = catalog as unknown as { collator: Intl.Collator };
    catalogWithTurkishCollation.collator = new Intl.Collator('tr', {
      usage: 'search',
      sensitivity: 'base',
    });

    const istanbul = catalog.resolve('Europe/Istanbul');

    expect(istanbul).not.toBeNull();
    await expect(firstValueFrom(catalog.search('istanbul'))).resolves.toContain(istanbul);
  });

  it('returns no results for a blank query', async () => {
    await expect(firstValueFrom(catalog.search('   '))).resolves.toEqual([]);
  });
});
