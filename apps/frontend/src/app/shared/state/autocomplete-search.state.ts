/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { DestroyRef, Signal, computed, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, Subject, catchError, debounceTime, map, of, switchMap } from 'rxjs';

export interface AutocompleteSearchSnapshot<T> {
  readonly items: readonly T[];
  readonly loading: boolean;
  readonly error: unknown | null;
}

/** Owns asynchronous query policy outside the reusable visual control. */
export class AutocompleteSearchState<T> {
  private readonly state = signal<AutocompleteSearchSnapshot<T>>({
    items: [],
    loading: false,
    error: null,
  });
  private readonly queries = new Subject<string>();

  readonly items: Signal<readonly T[]> = computed(() => this.state().items);
  readonly loading: Signal<boolean> = computed(() => this.state().loading);
  readonly error: Signal<unknown | null> = computed(() => this.state().error);

  constructor(
    search: (query: string) => Observable<readonly T[]>,
    destroyRef: DestroyRef,
    debounceMs = 350,
    minChars = 3,
  ) {
    this.queries
      .pipe(
        debounceTime(debounceMs),
        switchMap((query) => {
          if (query.length < minChars)
            return of<AutocompleteSearchSnapshot<T>>({ items: [], loading: false, error: null });
          this.state.set({ items: [], loading: true, error: null });
          return search(query).pipe(
            map((items) => ({ items, loading: false, error: null })),
            catchError((error) => of({ items: [], loading: false, error })),
          );
        }),
        takeUntilDestroyed(destroyRef),
      )
      .subscribe((state) => this.state.set(state));
  }

  search(query: string): void {
    const normalized = query.trim();
    if (!normalized) this.state.set({ items: [], loading: false, error: null });
    this.queries.next(normalized);
  }
}
