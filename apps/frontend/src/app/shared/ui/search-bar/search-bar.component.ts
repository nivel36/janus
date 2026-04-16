import { Component, DestroyRef, OnInit, inject, input, output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import {
  Observable,
  Subject,
  catchError,
  debounceTime,
  defer,
  distinctUntilChanged,
  from,
  isObservable,
  map,
  merge,
  of,
  switchMap,
  takeUntil,
  tap,
} from 'rxjs';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faMagnifyingGlass } from '@fortawesome/free-solid-svg-icons';
import { SearchMethod } from '../../types/search.types';

/**
 * Visual state of the search bar.
 *
 * @typeParam T Type of the result items returned by the search function
 */
type SearchBarState<T> =
  | { kind: 'idle' }
  | { kind: 'loading'; query: string }
  | { kind: 'results'; query: string; items: T[] }
  | { kind: 'empty'; query: string }
  | { kind: 'error'; query: string };

/**
 * Standalone search bar component that owns the asynchronous search pipeline.
 *
 * Unlike a passive input that only emits raw text, this component centralizes
 * search execution so that stale requests are automatically invalidated when
 * the user changes the text or explicitly submits a new query.
 *
 * Main capabilities:
 *
 * - trims input before searching
 * - debounces typing
 * - ignores consecutive identical queries
 * - invalidates obsolete searches through {@link switchMap}
 * - exposes loading and result state
 * - supports both debounced search and explicit submit
 *
 * @typeParam T Type of the search result items
 */
@Component({
  selector: 'app-search-bar',
  standalone: true,
  imports: [ReactiveFormsModule, TranslatePipe, FontAwesomeModule],
  templateUrl: './search-bar.component.html',
  styleUrl: './search-bar.component.css',
})
export class SearchBarComponent<T = unknown> implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  /**
   * Debounce time, in milliseconds, applied to user typing.
   */
  readonly debounceMs = input(350);

  /**
   * Minimum number of non-empty characters required to launch a search.
   */
  readonly minChars = input(3);

  /**
   * Emits the effective query that has started processing.
   *
   * This is emitted only when the component actually starts a search.
   */
  readonly queryChange = output<string>();

  /**
   * Icon displayed in the submit button.
   */
  readonly faMagnifyingGlass = faMagnifyingGlass;

  /**
   * DOM identifier of the main input element.
   */
  protected readonly inputId = 'search-bar-input';

  /**
   * DOM identifier of the hidden helper text associated with the input.
   */
  protected readonly helpTextId = 'search-bar-help';

  /**
   * Reactive control bound to the visible search input.
   */
  protected readonly queryControl = new FormControl('', { nonNullable: true });

  /**
   * Initializes the internal reactive search pipeline.
   */
  ngOnInit(): void {
    this.validateInputs();
  }

  /**
   * Validates required inputs and basic invariants.
   */
  private validateInputs(): void {
    if (this.debounceMs() < 0) {
      throw new Error('SearchBarComponent: debounceMs cannot be negative.');
    }

    if (this.minChars() < 0) {
      throw new Error('SearchBarComponent: minChars cannot be negative.');
    }
  }

  constructor() {
    this.queryControl.valueChanges
      .pipe(
        map((value) => value.trim()),
        debounceTime(this.debounceMs()),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((query) => this.queryChange.emit(query));
  }

  protected submitCurrentQuery(): void {
    this.queryChange.emit(this.queryControl.value.trim());
  }
}
