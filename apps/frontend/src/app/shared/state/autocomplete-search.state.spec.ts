/** SPDX-License-Identifier: Apache-2.0 */
import { DestroyRef } from '@angular/core';
import { Subject, of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { AutocompleteSearchState } from './autocomplete-search.state';

describe('AutocompleteSearchState', () => {
  const destroyRef = { onDestroy: vi.fn(() => noop) } as unknown as DestroyRef;
  const noop = (): void => undefined;

  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('owns debounce and minimum-query policy', () => {
    const search = vi.fn((query: string) => of([query]));
    const state = new AutocompleteSearchState(search, destroyRef, 100, 3);
    state.search('ab');
    vi.advanceTimersByTime(100);
    expect(search).not.toHaveBeenCalled();
    state.search('abc');
    vi.advanceTimersByTime(100);
    expect(search).toHaveBeenCalledWith('abc');
    expect(state.items()).toEqual(['abc']);
  });

  it('cancels the previous request when the query changes', () => {
    const first = new Subject<readonly string[]>();
    const search = vi.fn((query: string) => (query === 'first' ? first : of([query])));
    const state = new AutocompleteSearchState(search, destroyRef, 0, 1);
    state.search('first');
    vi.runAllTimers();
    state.search('second');
    vi.runAllTimers();
    first.next(['stale']);
    expect(state.items()).toEqual(['second']);
  });
});
