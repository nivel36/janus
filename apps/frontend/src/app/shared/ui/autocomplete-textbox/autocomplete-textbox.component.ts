/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { CommonModule } from '@angular/common';
import {
  Component,
  DestroyRef,
  ElementRef,
  EventEmitter,
  Input,
  OnInit,
  Output,
  QueryList,
  ViewChildren,
  forwardRef,
  inject,
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
  catchError,
  debounceTime,
  distinctUntilChanged,
  filter,
  finalize,
  from,
  isObservable,
  map,
  of,
  switchMap,
  tap,
} from 'rxjs';
import { TranslatePipe } from '@ngx-translate/core';

/**
 * Standalone autocomplete textbox component that integrates with Angular reactive forms
 * through the {@link ControlValueAccessor} contract.
 *
 * This component is designed as a selection control, not as a free-text input.
 * The text field is used only to search for candidate options, while the value
 * propagated to the parent form is the value derived from the selected option.
 *
 * The component therefore manages two related but distinct pieces of state:
 *
 * - the current text typed by the user, stored in {@link textControl}
 * - the currently selected domain object, stored in {@link selectedValue}
 *
 * A parent form never receives arbitrary text typed by the user. It only receives:
 *
 * - the string produced by {@link valueWith} when an option is selected
 * - {@code null} when the selection is cleared or invalidated
 *
 * @typeParam T Type of the domain object represented by each autocomplete option
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
export class AutocompleteTextboxComponent<T = unknown> implements OnInit, ControlValueAccessor {
  /**
   * Search function used to retrieve autocomplete candidates for the current query.
   *
   * The function receives the already-trimmed user input and may return either
   * an {@link Observable} or a {@link Promise}.
   *
   * This input is required because the component cannot perform any search without it.
   */
  @Input({ required: true })
  searchMethod!: (query: string) => Observable<T[]> | Promise<T[]>;

  /**
   * Maps an option into the label displayed in the text input and in the dropdown list.
   *
   * The default implementation converts the option to string using {@link String}.
   */
  @Input()
  displayWith: (option: T) => string = (option: T) => String(option);

  /**
   * Maps a selected option into the string value propagated to the parent form control.
   *
   * This is the actual persisted form value exposed through the
   * {@link ControlValueAccessor} interface.
   *
   * The default implementation converts the option to string using {@link String}.
   */
  @Input()
  valueWith: (option: T) => string = (option: T) => String(option);

  /**
   * Resolves an external form value back into its corresponding option instance.
   *
   * Angular calls {@link writeValue} when the parent form writes a value into this
   * component. This resolver allows the component to reconstruct the selected option
   * and the text shown to the user from that stored form value.
   *
   * The resolver may return synchronously or asynchronously:
   *
   * - {@link T} or {@code null}
   * - {@link Promise} of {@link T} or {@code null}
   * - {@link Observable} of {@link T} or {@code null}
   *
   * If resolution fails or returns {@code null}, the raw incoming value is displayed
   * in the input as a fallback, but no selected option is kept internally.
   */
  @Input()
  resolveByValue: (value: string) => Observable<T | null> | Promise<T | null> | T | null = () =>
    null;

  /**
   * Maps an option into a stable identifier used by Angular to track items
   * in the rendered result list.
   *
   * This function is used by the template `@for` directive to determine the
   * identity of each element across change detection cycles. A correct tracking
   * key allows Angular to efficiently update the DOM by reusing existing nodes
   * instead of recreating them.
   *
   * The returned value must satisfy the following properties:
   *
   * - **Uniqueness within the current result set**: two different options must
   *   not produce the same tracking value
   * - **Stability across renders**: the same option must always produce the same
   *   value while it represents the same logical entity
   * - **Primitive type**: typically a `string` or `number`
   *
   * By default, this function delegates to {@link valueWith}, assuming it
   * represents a unique identifier for the option. This keeps consistency
   * between the value stored in the form and the identity used for rendering.
   *
   * Overriding this input may be necessary when:
   *
   * - the value returned by {@link valueWith} is not unique within the result list
   * - the rendering identity differs from the persisted form value
   *
   * Using a non-unique or unstable tracking value may lead to subtle UI issues,
   * such as:
   *
   * - incorrect reuse of DOM elements
   * - visual glitches when updating the list
   * - event handlers bound to the wrong item
   *
   * @defaultValue (option: T) => this.valueWith(option)
   *
   * @example
   * // Track by unique identifier
   * trackByValue = (user) => user.id;
   *
   * @example
   * // Track by composite key
   * trackByValue = (item) => `${item.type}-${item.id}`;
   */
  @Input()
  trackByValue: (option: T) => string | number = (option: T) => this.valueWith(option);

  /**
   * Placeholder shown when the input is empty and {@link emptyHint} is not provided.
   */
  @Input() placeholder = '';

  /**
   * Alternative hint shown when there is no current selection.
   *
   * When non-empty, this value takes precedence over {@link placeholder}.
   */
  @Input() emptyHint = '';

  /**
   * Debounce time, in milliseconds, applied before triggering a search request.
   *
   * This helps reduce unnecessary backend calls while the user is typing.
   */
  @Input() debounceMs = 350;

  /**
   * Minimum number of non-whitespace characters required before a search is executed.
   */
  @Input() minChars = 3;

  /**
   * Auxiliary event emitted when the selected option changes.
   *
   * This event is intended for consumers that need access to the full selected object,
   * not just the string value propagated to the parent form.
   *
   * Emitted values:
   *
   * - the selected option when an option is chosen
   * - {@code null} when the selection is cleared or invalidated
   */
  @Output() selectedChange = new EventEmitter<T | null>();

  /**
   * References to the DOM elements representing each result option button.
   *
   * This query collects all elements marked with the template reference
   * variable `#resultButton` inside the result list, preserving their order
   * as rendered in the view.
   *
   * The resulting {@link QueryList} is automatically kept in sync with the DOM:
   *
   * - when the result list changes, the collection is updated
   * - elements are added, removed, or reordered accordingly
   *
   * This allows the component to interact directly with the rendered options,
   * enabling behaviors that depend on their physical presence in the DOM,
   * such as:
   *
   * - programmatic scrolling of the active option into view
   * - measuring element dimensions or positions if needed
   *
   * Each entry in the collection is wrapped in an {@link ElementRef} pointing
   * to the underlying {@link HTMLButtonElement}.
   *
   * The non-null assertion (`!`) is used because Angular initializes the query
   * after view creation, ensuring it is available during runtime.
   */
  @ViewChildren('resultButton')
  private resultButtons!: QueryList<ElementRef<HTMLButtonElement>>;

  /**
   * Internal text control bound to the visible input element.
   *
   * This control represents the current search text shown in the UI.
   * It does not represent the parent form value.
   */
  readonly textControl = new FormControl('', { nonNullable: true });

  /**
   * Current search results displayed in the autocomplete dropdown.
   */
  results: T[] = [];

  /**
   * Currently selected option, or {@code null} when no valid selection exists.
   */
  selectedValue: T | null = null;

  /**
   * Indicates whether an asynchronous operation is currently in progress.
   *
   * This flag is used both while searching for options and while resolving an incoming
   * form value back into its corresponding option.
   */
  isLoading = false;

  /**
   * Indicates whether the component is disabled through Angular forms.
   *
   * When disabled, the internal text control is disabled as well and selection actions
   * are ignored.
   */
  disabled = false;

  /**
   * Index of the option currently highlighted for keyboard navigation.
   *
   * A value of {@code -1} means that no option is currently active.
   */
  activeIndex = -1;

  /**
   * Indicates whether the overlay has been explicitly closed by the user.
   *
   * When this flag is set, the overlay remains closed until the input changes again
   * and a new search cycle starts.
   */
  private overlayDismissed = false;

  /**
   * Internal counter used to generate unique identifiers for each component instance.
   *
   * This static value is incremented every time a new instance of the component is created.
   * It ensures that each instance can derive a distinct identifier, avoiding collisions
   * when multiple autocomplete components are present in the same document.
   */
  private static nextInstanceId = 0;

  /**
   * Unique identifier assigned to this specific component instance.
   *
   * The value is derived from {@link nextInstanceId} and remains stable for the lifetime
   * of the component. It is primarily used to build DOM ids that must be unique across
   * the entire page.
   */
  private readonly instanceId = AutocompleteTextboxComponent.nextInstanceId++;

  /**
   * Base identifier used to generate stable and globally unique DOM ids for the listbox options.
   *
   * This prefix incorporates the instance-specific identifier to prevent collisions between
   * multiple autocomplete components rendered simultaneously.
   *
   * The generated ids are referenced by accessibility attributes such as
   * `aria-activedescendant`, allowing assistive technologies to correctly associate
   * the active option with the input element.
   *
   * Example generated ids:
   *
   * - autocomplete-option-0-0
   * - autocomplete-option-0-1
   * - autocomplete-option-1-0
   *
   * Format:
   * `${optionIdPrefix}-${index}`
   */
  protected readonly optionIdPrefix = `autocomplete-option-${this.instanceId}`;

  /**
   * Preferred positions used by the CDK connected overlay that renders the result list.
   *
   * The overlay first attempts to open below the input. If there is not enough space,
   * it may open above it.
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
   * Reference used to automatically dispose subscriptions when the component is destroyed.
   */
  private readonly destroyRef = inject(DestroyRef);

  /**
   * Callback registered by Angular forms to receive value changes from this component.
   *
   * The propagated value is the string produced by {@link valueWith}, or {@code null}
   * when there is no valid selection.
   */
  private onChange: (value: string | null) => void = () => {};

  /**
   * Callback registered by Angular forms to mark this control as touched.
   */
  private onTouched: () => void = () => {};

  /**
   * Initializes the reactive search pipeline bound to the input text control.
   *
   * The pipeline performs the following steps:
   *
   * - trims the incoming text
   * - ignores repeated values
   * - invalidates the current selection when the typed text no longer matches it
   * - waits for the configured debounce period
   * - runs a search only when the input is eligible
   * - cancels previous searches when a new query arrives
   * - safely handles errors by exposing an empty result list
   */
  ngOnInit(): void {
    this.textControl.valueChanges
      .pipe(
        map((value) => value.trim()),
        distinctUntilChanged(),
        tap((value) => this.handleTextChange(value)),
        debounceTime(this.debounceMs),
        filter((value) => this.canSearch(value)),
        switchMap((query) => {
          this.isLoading = true;

          return from(this.searchMethod(query)).pipe(
            catchError(() => of([] as T[])),
            finalize(() => {
              this.isLoading = false;
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((results) => {
        this.results = results;
        this.activeIndex = results.length > 0 ? 0 : -1;
      });
  }

  /**
   * Indicates whether the component currently holds a valid selected option.
   *
   * @returns {@code true} when {@link selectedValue} is not {@code null}; {@code false} otherwise
   */
  get hasSelection(): boolean {
    return this.selectedValue !== null;
  }

  /**
   * Selects one option from the current result list.
   *
   * Once an option is selected, the component:
   *
   * - stores the selected domain object internally
   * - clears the dropdown results
   * - updates the visible text with {@link displayWith}
   * - emits {@link selectedChange}
   * - propagates the transformed form value through {@link onChange}
   * - marks the control as touched
   *
   * If the component is disabled, the method does nothing.
   *
   * @param option Option selected by the user
   */
  onSelect(option: T): void {
    if (this.disabled) {
      return;
    }

    this.overlayDismissed = false;
    this.activeIndex = -1;
    this.selectedValue = option;
    this.results = [];
    this.textControl.setValue(this.displayWith(option), { emitEvent: false });
    this.selectedChange.emit(option);
    this.onChange(this.valueWith(option));
    this.onTouched();
  }

  /**
   * Clears the current selection and propagates a {@code null} value to the parent form.
   *
   * The visible input text is reset to an empty string and the dropdown results are removed.
   *
   * If the component is disabled, the method does nothing.
   */
  clearSelection(): void {
    if (this.disabled) {
      return;
    }

    this.overlayDismissed = false;
    this.activeIndex = -1;
    this.selectedValue = null;
    this.results = [];
    this.textControl.setValue('', { emitEvent: false });
    this.selectedChange.emit(null);
    this.onChange(null);
    this.onTouched();
  }

  /**
   * Writes an external form value into this component.
   *
   * Angular forms call this method whenever the parent model pushes a new value into
   * the control. The component then tries to resolve that value back into its full
   * domain object using {@link resolveByValue}.
   *
   * Behavior:
   *
   * - when the incoming value is {@code null}, {@code undefined}, or an empty string,
   *   the component is reset to the empty state
   * - otherwise the component attempts to resolve the stored value into an option
   * - if resolution succeeds, the selected option is restored and its display label
   *   is shown in the text box
   * - if resolution fails or returns {@code null}, the raw incoming value is shown
   *   in the text box as a fallback, while no selected option is kept
   *
   * @param value External form value written by Angular forms
   */
  writeValue(value: string | null): void {
    this.overlayDismissed = false;
    this.activeIndex = -1;
    this.results = [];

    if (!value) {
      this.selectedValue = null;
      this.textControl.setValue('', { emitEvent: false });
      return;
    }

    this.isLoading = true;

    this.toObservable(this.resolveByValue(value))
      .pipe(
        catchError(() => of(null)),
        finalize(() => {
          this.isLoading = false;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((resolvedOption) => {
        this.selectedValue = resolvedOption;
        this.textControl.setValue(resolvedOption ? this.displayWith(resolvedOption) : value, {
          emitEvent: false,
        });
      });
  }

  private toObservable<V>(value: Observable<V> | Promise<V> | V): Observable<V> {
    if (isObservable(value)) {
      return value;
    }

    return from(Promise.resolve(value));
  }

  /**
   * Registers the callback used by Angular forms to receive value updates from this control.
   *
   * @param fn Callback invoked whenever the component propagates a new form value
   */
  registerOnChange(fn: (value: string | null) => void): void {
    this.onChange = fn;
  }

  /**
   * Registers the callback used by Angular forms to mark this control as touched.
   *
   * @param fn Callback invoked when the control should be marked as touched
   */
  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  /**
   * Updates the disabled state of this component from Angular forms.
   *
   * The internal text control is kept in sync with the externally supplied disabled state.
   *
   * @param isDisabled {@code true} to disable the component; {@code false} to enable it
   */
  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;

    if (isDisabled) {
      this.textControl.disable({ emitEvent: false });
    } else {
      this.textControl.enable({ emitEvent: false });
    }
  }

  /**
   * Marks the component as touched when the input loses focus.
   */
  handleBlur(): void {
    this.onTouched();
  }

  /**
   * Handles text changes coming from the input control after trimming.
   *
   * This method is responsible for preserving the invariant that the parent form only
   * contains a valid selected value, never arbitrary search text.
   *
   * If the user edits the text so that it no longer matches the currently selected
   * option label, the existing selection is invalidated and the parent form receives
   * {@code null}.
   *
   * The method also clears the current result list when:
   *
   * - the text is shorter than {@link minChars}
   * - a valid selection already exists
   *
   * @param trimmedValue Current trimmed text from the input control
   */
  private handleTextChange(trimmedValue: string): void {
    this.overlayDismissed = false;
    const selectedLabel = this.selectedValue ? this.displayWith(this.selectedValue).trim() : '';

    if (this.selectedValue && trimmedValue !== selectedLabel) {
      this.selectedValue = null;
      this.selectedChange.emit(null);
      this.onChange(null);
    }

    if (trimmedValue.length < this.minChars || this.hasSelection) {
      this.results = [];
      this.activeIndex = -1;
    }
  }

  /**
   * Determines whether a search should be executed for the current text.
   *
   * A search is allowed only when all of the following conditions are met:
   *
   * - the trimmed text length is at least {@link minChars}
   * - there is no active selection
   * - the component is not disabled
   *
   * @param trimmedValue Current trimmed text from the input control
   * @returns {@code true} when a search should be performed; {@code false} otherwise
   */
  private canSearch(trimmedValue: string): boolean {
    return trimmedValue.length >= this.minChars && !this.hasSelection && !this.disabled;
  }

  /**
   * Indicates whether the current input is long enough to display autocomplete feedback.
   *
   * @returns {@code true} when the trimmed input length is at least {@link minChars}
   */
  get hasSearchableText(): boolean {
    return this.textControl.value.trim().length >= this.minChars;
  }

  /**
   * Indicates whether the autocomplete overlay should be visible.
   *
   * The overlay is shown only when:
   *
   * - there is no current selection
   * - the input contains enough text to search
   * - the overlay has not been explicitly dismissed
   *
   * This allows the overlay to show loading feedback, results, or an empty-state
   * message while still letting the user close it with the Escape key.
   *
   * @returns {@code true} when the overlay should be displayed; {@code false} otherwise
   */
  get isOverlayOpen(): boolean {
    return !this.hasSelection && this.hasSearchableText && !this.overlayDismissed;
  }

  /**
   * Handles keyboard interaction from the text input.
   *
   * Supported keys:
   *
   * - ArrowDown: moves the active option down
   * - ArrowUp: moves the active option up
   * - Enter: selects the active option
   * - Escape: closes the overlay result state
   *
   * @param event Keyboard event raised by the input element
   */
  onInputKeydown(event: KeyboardEvent): void {
    if (this.disabled) {
      return;
    }

    switch (event.key) {
      case 'ArrowDown':
        if (!this.isOverlayOpen || this.results.length === 0) {
          return;
        }

        event.preventDefault();
        this.moveActiveIndex(1);
        break;

      case 'ArrowUp':
        if (!this.isOverlayOpen || this.results.length === 0) {
          return;
        }

        event.preventDefault();
        this.moveActiveIndex(-1);
        break;

      case 'Enter':
        if (!this.isOverlayOpen || this.results.length === 0 || this.activeIndex < 0) {
          return;
        }

        event.preventDefault();
        this.onSelect(this.results[this.activeIndex]);
        break;

      case 'Escape':
        if (!this.isOverlayOpen) {
          return;
        }

        event.preventDefault();
        this.closeOverlay();
        break;

      default:
        break;
    }
  }

  /**
   * Moves the active option index by the provided offset.
   *
   * Navigation wraps around the result list boundaries.
   *
   * @param step Offset to apply. Usually {@code 1} or {@code -1}
   */
  private moveActiveIndex(step: number): void {
    if (this.results.length === 0) {
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

  private scrollActiveOptionIntoView(): void {
    if (this.activeIndex < 0) {
      return;
    }

    const button = this.resultButtons.get(this.activeIndex)?.nativeElement;

    button?.scrollIntoView({
      block: 'nearest',
    });
  }

  /**
   * Closes the autocomplete overlay state without clearing the current input text.
   *
   * This is used, for example, when the user presses the Escape key.
   */
  private closeOverlay(): void {
    this.results = [];
    this.overlayDismissed = true;
    this.activeIndex = -1;
    this.isLoading = false;
  }

  /**
   * Returns whether the option at the provided index is currently active.
   *
   * @param index Zero-based result index
   * @returns {@code true} when the option is active; {@code false} otherwise
   */
  isActive(index: number): boolean {
    return this.activeIndex === index;
  }

  /**
   * Returns the DOM id used for the option at the provided index.
   *
   * @param index Zero-based result index
   * @returns Stable DOM id for the option
   */
  getOptionId(index: number): string {
    return `${this.optionIdPrefix}-${index}`;
  }

  /**
   * Closes the autocomplete overlay when the user clicks outside the overlay panel.
   *
   * The current input text is preserved. Only the overlay state is dismissed.
   */
  onOutsideClick(): void {
    if (!this.isOverlayOpen) {
      return;
    }

    this.closeOverlay();
  }
}
