/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { CommonModule } from '@angular/common';
import {
  Component,
  DestroyRef,
  EventEmitter,
  Input,
  OnInit,
  Output,
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
  of,
  switchMap,
  tap,
} from 'rxjs';

@Component({
  selector: 'app-autocomplete-textbox',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, OverlayModule],
  templateUrl: './autocomplete-textbox.component.html',
  styleUrl: './autocomplete-textbox.component.css',
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
   * Search function used to retrieve matching options.
   */
  @Input({ required: true })
  searchMethod!: (query: string) => Observable<T[]> | Promise<T[]>;

  /**
   * Maps an option into the text displayed to the user.
   */
  @Input()
  displayWith: (option: T) => string = (option: T) => String(option);

  /**
   * Maps an option into the string value stored in the parent form control.
   */
  @Input()
  valueWith: (option: T) => string = (option: T) => String(option);

  /**
   * Resolves a stored form value back into its corresponding option.
   *
   * This is used by writeValue when Angular writes the model value into
   * the component.
   */
  @Input()
  resolveByValue: (value: string) => T | null = () => null;

  @Input() placeholder = '';
  @Input() emptyHint = '';
  @Input() debounceMs = 350;
  @Input() minChars = 3;

  /**
   * Auxiliary event emitted when the selected option changes.
   *
   * The form value is propagated independently through ControlValueAccessor.
   */
  @Output() selectedChange = new EventEmitter<T | null>();

  /**
   * Internal text control used by the input element.
   */
  readonly textControl = new FormControl('', { nonNullable: true });

  /**
   * Current search results displayed in the dropdown.
   */
  results: T[] = [];

  /**
   * Currently selected option, if any.
   */
  selectedValue: T | null = null;

  /**
   * Indicates whether a search request is in progress.
   */
  isLoading = false;

  /**
   * Indicates whether the component is disabled by Angular forms.
   */
  disabled = false;

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

  private readonly destroyRef = inject(DestroyRef);

  /**
   * Callback registered by Angular to propagate value changes.
   */
  private onChange: (value: string | null) => void = () => {};

  /**
   * Callback registered by Angular to propagate touch state.
   */
  private onTouched: () => void = () => {};

  ngOnInit(): void {
    this.textControl.valueChanges
      .pipe(
        distinctUntilChanged(),
        tap((value) => {
          this.handleTextChange(value);
        }),
        debounceTime(this.debounceMs),
        filter((value) => this.canSearch(value)),
        switchMap((query) => {
          this.isLoading = true;

          return from(this.searchMethod(query.trim())).pipe(
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
      });
  }

  /**
   * Indicates whether the component currently holds a valid selection.
   */
  get hasSelection(): boolean {
    return this.selectedValue !== null;
  }

  /**
   * Handles the selection of one option from the dropdown.
   *
   * @param option Selected option
   */
  onSelect(option: T): void {
    if (this.disabled) {
      return;
    }

    this.selectedValue = option;
    this.results = [];
    this.textControl.setValue(this.displayWith(option), { emitEvent: false });
    this.selectedChange.emit(option);
    this.onChange(this.valueWith(option));
    this.onTouched();
  }

  /**
   * Clears the current selection and propagates a null value to the form.
   */
  clearSelection(): void {
    if (this.disabled) {
      return;
    }

    this.selectedValue = null;
    this.results = [];
    this.textControl.setValue('', { emitEvent: false });
    this.selectedChange.emit(null);
    this.onChange(null);
    this.onTouched();
  }

  /**
   * Writes a form value into the component.
   *
   * @param value Stored form value
   */
  writeValue(value: string | null): void {
    this.results = [];

    if (value === null || value === '') {
      this.selectedValue = null;
      this.textControl.setValue('', { emitEvent: false });
      return;
    }

    const resolvedOption = this.resolveByValue(value);
    this.selectedValue = resolvedOption;

    this.textControl.setValue(resolvedOption ? this.displayWith(resolvedOption) : value, {
      emitEvent: false,
    });
  }

  /**
   * Registers the Angular callback used to propagate value changes.
   *
   * @param fn Change callback
   */
  registerOnChange(fn: (value: string | null) => void): void {
    this.onChange = fn;
  }

  /**
   * Registers the Angular callback used to propagate touch state.
   *
   * @param fn Touch callback
   */
  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  /**
   * Updates the disabled state of the component.
   *
   * @param isDisabled Whether the component must be disabled
   */
  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;

    if (isDisabled) {
      this.textControl.disable({ emitEvent: false });
      return;
    }

    this.textControl.enable({ emitEvent: false });
  }

  /**
   * Marks the control as touched when the input loses focus.
   */
  handleBlur(): void {
    this.onTouched();
  }

  /**
   * Handles raw text changes in the input.
   *
   * If the user edits the text after a valid selection, the selection is
   * invalidated and the form value becomes null.
   *
   * @param rawValue Raw input text
   */
  private handleTextChange(rawValue: string): void {
    const trimmedValue = rawValue.trim();
    const selectedLabel = this.selectedValue ? this.displayWith(this.selectedValue) : '';

    if (this.selectedValue && trimmedValue !== selectedLabel.trim()) {
      this.selectedValue = null;
      this.selectedChange.emit(null);
      this.onChange(null);
    }

    if (trimmedValue.length < this.minChars || this.hasSelection) {
      this.results = [];
    }
  }

  /**
   * Determines whether a search should be executed for the current input.
   *
   * @param rawValue Raw input text
   * @returns True when search should proceed
   */
  private canSearch(rawValue: string): boolean {
    const trimmedValue = rawValue.trim();
    return trimmedValue.length >= this.minChars && !this.hasSelection;
  }
}
