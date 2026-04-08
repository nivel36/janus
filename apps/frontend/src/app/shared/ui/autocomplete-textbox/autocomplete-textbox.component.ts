/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { CommonModule } from '@angular/common';
import {
  AfterViewInit,
  Component,
  DestroyRef,
  ElementRef,
  EventEmitter,
  Input,
  OnInit,
  Output,
  QueryList,
  ViewChild,
  ViewChildren,
  forwardRef,
  inject,
  isDevMode,
} from '@angular/core';
import { ConnectedPosition, OverlayModule } from '@angular/cdk/overlay';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  ControlValueAccessor,
  FormControl,
  NG_VALUE_ACCESSOR,
  ReactiveFormsModule,
} from '@angular/forms';
import {
  Observable,
  Subject,
  catchError,
  debounceTime,
  defer,
  distinctUntilChanged,
  finalize,
  from,
  isObservable,
  map,
  of,
  switchMap,
  take,
  takeUntil,
  tap,
} from 'rxjs';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

/**
 * Visual state of the autocomplete panel.
 *
 * @typeParam T Type of the item displayed in the results list
 */
type PanelModel<T> =
  | { kind: 'closed' }
  | { kind: 'loading'; query: string }
  | { kind: 'results'; query: string; items: T[] }
  | { kind: 'empty'; query: string };

/**
 * Internal result of resolving an external value written by Angular Forms.
 *
 * @typeParam T Type of the resolved option
 */
type ResolvedWriteValue<T> = {
  /**
   * Original value received from the parent form.
   *
   * When the full option cannot be resolved, this value is used
   * as fallback text shown in the input.
   */
  originalValue: string;

  /**
   * Domain option resolved from the external value.
   *
   * Will be {@code null} if the value could not be resolved or if the external
   * value represents an empty state.
   */
  resolvedOption: T | null;
};

/**
 * Standalone autocomplete component integrated with Angular Reactive Forms
 * through the {@link ControlValueAccessor} contract.
 *
 * This component behaves as a selection control rather than a free text field.
 * The user types only to search for candidates, while the value propagated to
 * the parent form always corresponds to the selected option or {@code null}.
 *
 * The component maintains two related but distinct states:
 *
 * - the visible text in the input, managed by {@link textControl}
 * - the currently selected object, stored in {@link selectedValue}
 *
 * The parent form never receives arbitrary text entered by the user.
 * It only receives:
 *
 * - the value produced by {@link valueWith} when selecting an option
 * - {@code null} when the current selection is invalidated or cleared
 *
 * Main capabilities:
 *
 * - asynchronous search via {@link searchMethod}
 * - reconstruction of a selection from an external value using {@link resolveByValue}
 * - keyboard navigation over the results list
 * - ARIA combobox and listbox semantics
 * - auxiliary {@link selectedChange} event with the full selected object
 *
 * @typeParam T Type of the domain object represented by each option
 */
@Component({
  selector: 'app-autocomplete-textbox',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, OverlayModule, TranslatePipe],
  templateUrl: './autocomplete-textbox.component.html',
  styleUrls: ['./autocomplete-textbox.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AutocompleteTextboxComponent),
      multi: true,
    },
  ],
})
export class AutocompleteTextboxComponent<T = unknown>
  implements OnInit, AfterViewInit, ControlValueAccessor
{
  /**
   * Reference to the DOM element used as origin for the connected overlay.
   */
  @ViewChild('inputWrapper', { static: true })
  private inputWrapper!: ElementRef<HTMLElement>;

  /**
   * Current width, in pixels, applied to the overlay so it matches the input container.
   */
  overlayWidth = 0;

  /**
   * Search function used to retrieve autocomplete candidates.
   *
   * It can return:
   *
   * - a synchronous value
   * - a promise
   * - an Observable
   *
   * If it returns an Observable, it may emit multiple result lists for the same
   * query. The component remains subscribed until:
   *
   * - the user types a new query
   * - the search is invalidated
   * - the component is destroyed
   */
  @Input({ required: true })
  searchMethod!: (query: string) => Observable<T[]> | Promise<T[]> | T[];

  /**
   * Function that converts an option into the visible label shown both in the
   * input and in the results list.
   *
   * The default implementation uses {@link String}.
   */
  @Input()
  displayWith: (option: T) => string = (option: T) => String(option);

  /**
   * Function that converts a selected option into the string value propagated
   * to the parent form.
   *
   * The default implementation uses {@link String}.
   */
  @Input()
  valueWith: (option: T) => string = (option: T) => String(option);

  /**
   * Function that resolves an external form value to its domain object.
   *
   * If it returns an Observable, it must emit at most one meaningful value and
   * complete. This contract represents a one-shot state reconstruction operation,
   * not a continuous update stream.
   */
  @Input()
  resolveByValue: (value: string) => Observable<T | null> | Promise<T | null> | T | null = () =>
    null;

  /**
   * Function that generates a stable key so Angular can correctly reuse DOM
   * nodes in the results list.
   *
   * By default, it delegates to {@link valueWith}, assuming the persisted value
   * also uniquely identifies each option.
   */
  @Input()
  trackByValue: (option: T) => string | number = (option: T) => this.valueWith(option);

  /**
   * Placeholder shown when the field is empty and {@link emptyHint} is not defined.
   */
  @Input() placeholder = '';

  /**
   * Alternative text shown when there is no current selection.
   *
   * If not empty, it takes precedence over {@link placeholder}.
   */
  @Input() emptyHint = '';

  /**
   * Debounce time, in milliseconds, before executing a search.
   */
  @Input() debounceMs = 350;

  /**
   * Minimum number of non-empty characters required to trigger a search.
   */
  @Input() minChars = 3;

  /**
   * Accessible name used when the component is not associated with an external label.
   */
  @Input() ariaLabel = '';

  /**
   * Identifier of the external element that provides the accessible name of the input.
   */
  @Input() ariaLabelledBy = '';

  /**
   * Accessible label announced for the button that clears the current selection.
   */
  @Input() clearButtonAriaLabel = '';

  /**
   * Auxiliary event emitted whenever the selected option changes.
   */
  @Output() selectedChange = new EventEmitter<T | null>();

  /**
   * References to the DOM elements representing the rendered options.
   */
  @ViewChildren('resultOption')
  private resultOptions!: QueryList<ElementRef<HTMLElement>>;

  /**
   * Internal stream used to serialize external writes received through {@link writeValue}.
   */
  private readonly writeValueRequests$ = new Subject<string | null>();

  /**
   * Stream used to invalidate ongoing asynchronous searches when component state
   * changes and their results are no longer relevant.
   *
   * Examples:
   *
   * - the user selects an option while a search is still pending
   * - the parent form invokes {@link writeValue}
   * - the control is cleared or disabled
   */
  private readonly cancelSearchRequests$ = new Subject<void>();

  /**
   * Reactive control bound to the visible input.
   *
   * This control manages only the text shown to the user.
   */
  readonly textControl = new FormControl('', { nonNullable: true });

  /**
   * Currently selected option, or {@code null} when no valid selection exists.
   */
  selectedValue: T | null = null;

  /**
   * Whether the component is disabled.
   */
  disabled = false;

  /**
   * Index of the currently active option for keyboard navigation.
   */
  activeIndex = -1;

  /**
   * Current visual state of the floating panel.
   */
  panel: PanelModel<T> = { kind: 'closed' };

  /**
   * Global counter used to generate unique identifiers for each component instance.
   */
  private static nextInstanceId = 0;

  /**
   * Unique identifier of this component instance.
   */
  private readonly instanceId = AutocompleteTextboxComponent.nextInstanceId++;

  /**
   * Base prefix used to build stable DOM identifiers for rendered options.
   */
  protected readonly optionIdPrefix = `autocomplete-option-${this.instanceId}`;

  /**
   * DOM identifier of the main input element.
   */
  protected readonly inputId = `autocomplete-input-${this.instanceId}`;

  /**
   * DOM identifier of the ARIA region used to announce transient status messages.
   */
  protected readonly statusMessageId = `autocomplete-status-${this.instanceId}`;

  /**
   * DOM identifier of the visual popup container.
   */
  protected readonly resultsContainerId = `autocomplete-popup-${this.instanceId}`;

  /**
   * DOM identifier of the listbox element containing the results.
   */
  protected readonly resultsListId = `autocomplete-results-${this.instanceId}`;

  /**
   * Preferred positions for the CDK connected overlay.
   */
  protected readonly overlayPositions: ConnectedPosition[] = [
    {
      originX: 'start',
      originY: 'bottom',
      overlayX: 'start',
      overlayY: 'top',
      offsetY: 4,
    },
    {
      originX: 'start',
      originY: 'top',
      overlayX: 'start',
      overlayY: 'bottom',
      offsetY: -4,
    },
  ];

  /**
   * Number of currently active external value resolutions.
   *
   * A counter is used instead of a boolean to avoid race conditions when a
   * previous resolution is cancelled by {@code switchMap} while a new one is
   * already in progress.
   */
  private resolvingValueCount = 0;

  /**
   * Sequential counter used to distinguish transient live-region announcements.
   *
   * This prevents an old timer from clearing a newer message with identical text.
   */
  private liveMessageSequence = 0;

  /**
   * Last transient message explicitly announced through the live region.
   */
  private transientLiveMessage = '';

  /**
   * Translation service used to build localized messages.
   */
  private readonly translateService = inject(TranslateService);

  /**
   * Reference used to automatically dispose subscriptions when the component is destroyed.
   */
  private readonly destroyRef = inject(DestroyRef);

  /**
   * Callback registered by Angular Forms to propagate value changes.
   */
  private onChange: (value: string | null) => void = () => {};

  /**
   * Callback registered by Angular Forms to mark the control as touched.
   */
  private onTouched: () => void = () => {};

  /**
   * Creates the component and registers cleanup logic for global listeners.
   */
  constructor() {
    this.destroyRef.onDestroy(() => {
      if (typeof window !== 'undefined') {
        window.removeEventListener('resize', this.handleWindowResize);
      }
    });
  }

  /**
   * Initializes the internal reactive pipelines of the component.
   */
  ngOnInit(): void {
    this.validateInputs();
    this.warnIfAccessibleNameIsMissing();
    this.initializeSearchPipeline();
    this.initializeWriteValuePipeline();
  }

  /**
   * Validates required inputs and their basic invariants.
   *
   * This is defensive validation intended to avoid ambiguous runtime states
   * when the component is used incorrectly.
   */
  private validateInputs(): void {
    if (typeof this.searchMethod !== 'function') {
      throw new Error('AutocompleteTextboxComponent: searchMethod is required.');
    }

    if (this.minChars < 0) {
      throw new Error('AutocompleteTextboxComponent: minChars cannot be negative.');
    }

    if (this.debounceMs < 0) {
      throw new Error('AutocompleteTextboxComponent: debounceMs cannot be negative.');
    }
  }

  /**
   * Emits a development-only warning when the component has no accessible name.
   */
  private warnIfAccessibleNameIsMissing(): void {
    if (!isDevMode()) {
      return;
    }

    if (!this.ariaLabel.trim() && !this.ariaLabelledBy.trim()) {
      console.warn(this.translateService.instant('autocomplete.missingAccessibleNameWarning'));
    }
  }

  /**
   * Initializes the reactive pipeline that reacts to user-entered text and
   * triggers asynchronous searches.
   *
   * The active search is explicitly invalidated when the component enters a new
   * state incompatible with those results:
   *
   * - option selection
   * - control clearing
   * - external write through {@link writeValue}
   * - component disabling
   */
  private initializeSearchPipeline(): void {
    this.textControl.valueChanges
      .pipe(
        /**
         * The visible text is normalized by trimming leading and trailing whitespace.
         */
        map((value) => value.trim()),

        /**
         * Consecutive identical emissions are ignored after normalization.
         */
        distinctUntilChanged(),

        /**
         * Selection and panel state are updated before debounce is applied.
         */
        tap((value) => this.handleTextChange(value)),

        /**
         * Debounce avoids launching a search on every keystroke.
         */
        debounceTime(this.debounceMs),

        /**
         * Only the latest search is allowed to remain active. In addition, a search
         * may be invalidated externally through {@link cancelSearchRequests$}.
         */
        switchMap((query) => {
          if (!this.canSearch(query)) {
            return of<PanelModel<T>>({ kind: 'closed' });
          }

          /**
           * Loading state is exposed immediately before the search starts.
           */
          this.panel = { kind: 'loading', query };

          return defer(() => this.toObservable(this.searchMethod(query))).pipe(
            /**
             * If component state changes before the response arrives, the search is
             * no longer relevant and must be ignored entirely.
             */
            takeUntil(this.cancelSearchRequests$),
            map((items) => this.toPanelModel(query, items)),
            catchError(() => of<PanelModel<T>>({ kind: 'empty', query })),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((panel) => {
        if (panel.kind === 'closed') {
          this.closePanelSilently();
          return;
        }

        /**
         * Additional defensive guard: even if a response arrives late for any reason,
         * it must not reopen the panel if the component no longer allows searching.
         */
        if (!this.canSearch(panel.query)) {
          return;
        }

        /**
         * Each new result block resets active keyboard navigation.
         */
        this.activeIndex = -1;
        this.panel = panel;
      });
  }

  /**
   * Handles changes to the text entered by the user.
   *
   * This method:
   *
   * - invalidates the current selection when the text no longer matches its label
   * - closes the panel when the text no longer allows searching
   *
   * @param trimmedValue Current text, already trimmed
   */
  private handleTextChange(trimmedValue: string): void {
    const selectedLabel = this.selectedValue ? this.displayWith(this.selectedValue).trim() : '';

    /**
     * If the user edits the text so it no longer matches the current selection,
     * the selection becomes invalid and must be propagated as null.
     */
    if (this.selectedValue && trimmedValue !== selectedLabel) {
      this.clearSelectionState();
    }

    /**
     * When the text no longer satisfies minimum conditions, the panel must close.
     */
    if (!this.canSearch(trimmedValue)) {
      this.closePanelSilently();
    }
  }

  /**
   * Clears internal selection state and propagates the change to external consumers.
   *
   * @param markAsTouched Whether the control must also be marked as touched
   */
  private clearSelectionState(markAsTouched = false): void {
    this.selectedValue = null;
    this.selectedChange.emit(null);
    this.onChange(null);

    if (markAsTouched) {
      this.onTouched();
    }
  }

  /**
   * Determines whether the current text allows launching a search.
   *
   * The component behaves as a selection control: while a selection exists,
   * the input is effectively read-only and must not start new searches.
   *
   * @param trimmedValue Already normalized text
   * @returns {@code true} if a search should be performed
   */
  private canSearch(trimmedValue: string): boolean {
    return trimmedValue.length >= this.minChars && !this.hasSelection && !this.disabled;
  }

  /**
   * Normalizes a synchronous or asynchronous source into an {@link Observable}.
   *
   * @typeParam V Type of the emitted value
   * @param value Source to normalize
   * @returns Equivalent Observable
   */
  private toObservable<V>(value: Observable<V> | Promise<V> | V): Observable<V> {
    if (isObservable(value)) {
      return value;
    }

    return from(Promise.resolve(value));
  }

  /**
   * Converts a retrieved item list into its visual panel representation.
   *
   * @param query Query associated with the results
   * @param items Items returned by the search
   * @returns Corresponding visual state
   */
  private toPanelModel(query: string, items: T[]): PanelModel<T> {
    return items.length > 0 ? { kind: 'results', query, items } : { kind: 'empty', query };
  }

  /**
   * Closes the panel without emitting accessibility announcements.
   */
  private closePanelSilently(): void {
    this.panel = { kind: 'closed' };
    this.activeIndex = -1;
  }

  /**
   * Initializes the reactive pipeline responsible for processing external values
   * written by Angular Forms via {@link writeValue}.
   */
  private initializeWriteValuePipeline(): void {
    this.writeValueRequests$
      .pipe(
        /**
         * Before processing a new external write, visual state and current selection
         * are reset.
         */
        tap(() => {
          this.resetBeforeExternalWrite();
        }),

        /**
         * Only the latest external write remains relevant.
         */
        switchMap((value) => {
          /**
           * {@code null} is treated as absence of external value: no selection and no text.
           */
          if (value === null || value === '') {
            return of<ResolvedWriteValue<T>>({
              originalValue: '',
              resolvedOption: null,
            });
          }

          this.resolvingValueCount++;

          return defer(() => this.toObservable(this.resolveByValue(value))).pipe(
            take(1),
            map(
              (resolvedOption): ResolvedWriteValue<T> => ({
                originalValue: value,
                resolvedOption,
              }),
            ),
            catchError(() =>
              of<ResolvedWriteValue<T>>({
                originalValue: value,
                resolvedOption: null,
              }),
            ),
            finalize(() => {
              this.resolvingValueCount--;
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(({ originalValue, resolvedOption }) => {
        /**
         * Internal state is updated with the best information available.
         *
         * - if an option was resolved, its visible label is shown
         * - otherwise, the original value is shown as fallback text
         */
        this.selectedValue = resolvedOption;
        this.setTextProgrammatically(
          resolvedOption ? this.displayWith(resolvedOption) : originalValue,
        );
      });
  }

  /**
   * Resets component state before applying a new external write coming from
   * Angular Forms.
   *
   * This reset:
   *
   * - invalidates pending searches
   * - closes the panel
   * - removes the current selection
   * - clears the visible text
   *
   * No events are propagated to the parent form because this is incoming
   * synchronization, not a user action.
   */
  private resetBeforeExternalWrite(): void {
    this.cancelPendingSearches();
    this.closePanelSilently();
    this.selectedValue = null;
    this.setTextProgrammatically('');
  }

  /**
   * Invalidates any asynchronous search currently in progress.
   *
   * Used when a possible future response must no longer modify the panel.
   */
  private cancelPendingSearches(): void {
    this.cancelSearchRequests$.next();
  }

  /**
   * Updates the visible input text without emitting reactive change events.
   *
   * This allows programmatic writes to be reflected without triggering the user
   * input pipeline.
   *
   * @param value Text to be displayed
   */
  private setTextProgrammatically(value: string): void {
    this.textControl.setValue(value, { emitEvent: false });
  }

  /**
   * Finalizes view-dependent initialization once child references are available.
   *
   * This currently synchronizes the overlay width with the host input container
   * and registers a global resize listener to keep that width updated.
   */
  ngAfterViewInit(): void {
    this.updateOverlayWidth();

    if (typeof window !== 'undefined') {
      window.addEventListener('resize', this.handleWindowResize, { passive: true });
    }
  }

  /**
   * Recomputes the width of the connected overlay so it matches the input wrapper.
   */
  private updateOverlayWidth(): void {
    this.overlayWidth = this.inputWrapper?.nativeElement.getBoundingClientRect().width ?? 0;
  }

  /**
   * Window resize handler used to keep the overlay width aligned with the input.
   */
  private readonly handleWindowResize = (): void => {
    this.updateOverlayWidth();
  };

  /**
   * Handles keyboard interaction originating from the main input.
   *
   * @param event Keyboard event
   */
  onInputKeydown(event: KeyboardEvent): void {
    if (this.disabled) {
      return;
    }

    switch (event.key) {
      case 'ArrowDown':
      case 'ArrowUp':
        if (!this.hasResults) {
          return;
        }

        event.preventDefault();
        this.moveActiveIndex(event.key === 'ArrowDown' ? 1 : -1);
        return;

      case 'Enter':
        if (this.hasResults && this.activeIndex >= 0 && this.activeIndex < this.results.length) {
          event.preventDefault();
          this.onSelect(this.results[this.activeIndex]);
        }
        return;

      case 'Escape':
        if (this.isOverlayOpen) {
          event.preventDefault();
          this.closeOverlay();
        }
        return;

      default:
        return;
    }
  }

  /**
   * Moves the active index through the current results list.
   *
   * @param step Offset to apply to the current index
   */
  private moveActiveIndex(step: number): void {
    if (!this.hasResults) {
      this.activeIndex = -1;
      return;
    }

    if (this.activeIndex < 0) {
      this.activeIndex = step > 0 ? 0 : this.results.length - 1;
      this.scrollActiveOptionIntoView();
      return;
    }

    this.activeIndex = (this.activeIndex + step + this.results.length) % this.results.length;
    this.scrollActiveOptionIntoView();
  }

  /**
   * Scrolls the list to ensure the active option remains visible.
   */
  private scrollActiveOptionIntoView(): void {
    if (this.activeIndex < 0) {
      return;
    }

    const option = this.resultOptions.get(this.activeIndex)?.nativeElement;

    option?.scrollIntoView({
      block: 'nearest',
    });
  }

  /**
   * Closes the overlay when the user clicks outside the panel.
   */
  onOutsideClick(): void {
    if (!this.isOverlayOpen) {
      return;
    }

    this.closeOverlay();
  }

  /**
   * Closes the overlay and announces the closure through the live region.
   */
  private closeOverlay(): void {
    /**
     * Closing the overlay invalidates any pending search so an old response
     * cannot reopen results the user has just dismissed.
     */
    this.cancelPendingSearches();
    this.closePanelSilently();
    this.announceTransientMessage(this.translateService.instant('autocomplete.resultsClosed'));
  }

  /**
   * Announces a transient message in the live region.
   *
   * A sequential identifier is used to ensure stale timers cannot clear a newer message.
   *
   * @param message Message to announce
   */
  private announceTransientMessage(message: string): void {
    const sequence = ++this.liveMessageSequence;
    this.transientLiveMessage = '';

    queueMicrotask(() => {
      /**
       * If a newer message was generated during the microtask, this one is no longer valid.
       */
      if (this.liveMessageSequence !== sequence) {
        return;
      }

      this.transientLiveMessage = message;

      setTimeout(() => {
        /**
         * Only the timer associated with the most recent message may clear it.
         */
        if (this.liveMessageSequence === sequence) {
          this.transientLiveMessage = '';
        }
      }, 300);
    });
  }

  /**
   * Prevents the input from losing focus before a pointer-based selection is processed.
   *
   * {@link PointerEvent} is used instead of {@link MouseEvent} to better cover
   * modern devices and input modes.
   *
   * @param event Pointerdown event fired on an option
   */
  onOptionPointerDown(event: PointerEvent): void {
    event.preventDefault();
  }

  /**
   * Selects an option from the current list and propagates the corresponding value
   * to the parent form.
   *
   * @param option Option selected by the user
   */
  onSelect(option: T): void {
    if (this.disabled) {
      return;
    }

    /**
     * Any pending search response must no longer affect the panel, because the
     * current selection becomes the single source of truth.
     */
    this.cancelPendingSearches();

    this.selectedValue = option;
    this.closePanelSilently();
    this.setTextProgrammatically(this.displayWith(option));
    this.selectedChange.emit(option);
    this.onChange(this.valueWith(option));
    this.onTouched();
  }

  /**
   * Clears the current selection, erases the visible text, and propagates {@code null}
   * to the parent form.
   */
  clearSelection(): void {
    if (this.disabled) {
      return;
    }

    /**
     * A pending search could return stale results and reopen the panel after the
     * control has been cleared. It is invalidated first.
     */
    this.cancelPendingSearches();

    this.clearSelectionState(true);
    this.closePanelSilently();
    this.setTextProgrammatically('');
  }

  /**
   * Receives an external value from Angular Forms and queues it for processing.
   *
   * @param value External value written by the parent form
   */
  writeValue(value: string | null): void {
    this.writeValueRequests$.next(value);
  }

  /**
   * Marks the control as touched when it loses focus and closes the overlay.
   */
  handleBlur(): void {
    this.onTouched();
    this.closeOverlay();
  }

  /**
   * Registers the callback used by Angular Forms to receive propagated value changes.
   *
   * @param fn Propagation callback function
   */
  registerOnChange(fn: (value: string | null) => void): void {
    this.onChange = fn;
  }

  /**
   * Registers the callback used by Angular Forms to mark the control as touched.
   *
   * @param fn Touched callback function
   */
  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  /**
   * Updates the disabled state of the component from Angular Forms.
   *
   * @param isDisabled {@code true} to disable; {@code false} to enable
   */
  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;

    if (isDisabled) {
      /**
       * Once disabled, no pending search must be allowed to modify the panel.
       */
      this.cancelPendingSearches();
      this.textControl.disable({ emitEvent: false });
      this.closePanelSilently();
      return;
    }

    this.textControl.enable({ emitEvent: false });
  }

  /**
   * Indicates whether the option at the given index is the current active option.
   *
   * @param index Option index
   * @returns {@code true} if the option is active
   */
  isActive(index: number): boolean {
    return this.activeIndex === index;
  }

  /**
   * Returns the DOM identifier of the option at the given index.
   *
   * @param index Option index
   * @returns Stable identifier for that option
   */
  getOptionId(index: number): string {
    return `${this.optionIdPrefix}-${index}`;
  }

  /**
   * Current message exposed by the persistent live region.
   */
  get liveRegionMessage(): string {
    if (this.transientLiveMessage) {
      return this.transientLiveMessage;
    }

    if (!this.isOverlayOpen) {
      return '';
    }

    if (this.isLoading) {
      return this.translateService.instant('autocomplete.loadingResults');
    }

    if (this.panel.kind === 'empty') {
      return this.translateService.instant('autocomplete.noResultsFound');
    }

    if (this.panel.kind !== 'results') {
      return '';
    }

    if (this.panel.items.length === 1) {
      return this.translateService.instant('autocomplete.oneResultAvailable');
    }

    return this.translateService.instant('autocomplete.manyResultsAvailable', {
      count: this.panel.items.length,
    });
  }

  /**
   * Indicates whether a valid selection exists.
   */
  get hasSelection(): boolean {
    return this.selectedValue !== null;
  }

  /**
   * Indicates whether the autocomplete overlay should be shown.
   *
   * While a selection exists, the input behaves as effectively read-only and the
   * panel must not open.
   *
   * @returns {@code true} when the panel should be visible
   */
  get isOverlayOpen(): boolean {
    return this.panel.kind !== 'closed' && !this.disabled && !this.hasSelection;
  }

  /**
   * Indicates whether the current panel contains navigable results.
   */
  get hasResults(): boolean {
    return this.panel.kind === 'results' && this.panel.items.length > 0;
  }

  /**
   * Returns the current set of rendered results.
   *
   * @returns Current result list
   */
  get results(): T[] {
    return this.panel.kind === 'results' ? this.panel.items : [];
  }

  /**
   * Indicates whether the component is performing asynchronous work.
   *
   * This includes:
   *
   * - active searches
   * - external value resolutions written by Angular Forms
   *
   * @returns {@code true} if work is pending
   */
  get isLoading(): boolean {
    return this.panel.kind === 'loading' || this.resolvingValueCount > 0;
  }

  /**
   * Indicates whether the current text contains enough characters to allow
   * search-related feedback.
   */
  get hasSearchableText(): boolean {
    return this.textControl.value.trim().length >= this.minChars;
  }
}
