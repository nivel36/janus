/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { DestroyRef, Signal, computed, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, Subject, catchError, map, of, switchMap, timer } from 'rxjs';

export interface AutocompleteSearchSnapshot<T> {
  readonly items: readonly T[];
  readonly loading: boolean;
  readonly error: unknown | null;
  readonly panelOpen: boolean;
}

/** Owns asynchronous query policy outside the reusable visual control. */
export class AutocompleteSearchState<T> {
  private readonly state = signal<AutocompleteSearchSnapshot<T>>({
    items: [],
    loading: false,
    error: null,
    panelOpen: false,
  });
  private readonly queries = new Subject<string>();

  readonly items: Signal<readonly T[]> = computed(() => this.state().items);
  readonly loading: Signal<boolean> = computed(() => this.state().loading);
  readonly error: Signal<unknown | null> = computed(() => this.state().error);
  readonly panelOpen: Signal<boolean> = computed(() => this.state().panelOpen);

  constructor(
    search: (query: string) => Observable<readonly T[]>,
    destroyRef: DestroyRef,
    debounceMs = 350,
    private readonly minChars = 3,
  ) {
    this.queries
      .pipe(
        switchMap((query) => {
          if (query.length < this.minChars) {
            return of<AutocompleteSearchSnapshot<T>>({
              items: [],
              loading: false,
              error: null,
              panelOpen: false,
            });
          }
          return timer(debounceMs).pipe(
            switchMap(() => {
              this.state.set({ items: [], loading: true, error: null, panelOpen: true });
              return search(query).pipe(
                map((items) => ({ items, loading: false, error: null, panelOpen: true })),
                catchError((error) => of({ items: [], loading: false, error, panelOpen: true })),
              );
            }),
          );
        }),
        takeUntilDestroyed(destroyRef),
      )
      .subscribe((state) => this.state.set(state));
  }

  search(query: string): void {
    const normalized = query.trim();
    if (normalized.length < this.minChars) {
      this.state.set({ items: [], loading: false, error: null, panelOpen: false });
    }
    this.queries.next(normalized);
  }
}
