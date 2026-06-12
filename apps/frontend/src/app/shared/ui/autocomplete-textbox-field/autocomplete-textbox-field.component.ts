/**
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  booleanAttribute,
  Component,
  computed,
  DestroyRef,
  forwardRef,
  inject,
  input,
  output,
} from '@angular/core';
import {
  ControlValueAccessor,
  FormControl,
  NG_VALUE_ACCESSOR,
  ReactiveFormsModule,
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';

import { SearchMethod } from '../../types/search.types';
import { createUuid } from '../../utils/uuid.utils';
import { AutocompleteTextboxComponent } from '../autocomplete-textbox/autocomplete-textbox.component';
import { FieldComponent } from '../field/field.component';

/**
 * Autocomplete control wrapped in a standard application field.
 *
 * @typeParam T Type of the domain object represented by each option.
 */
@Component({
  selector: 'app-autocomplete-textbox-field',
  standalone: true,
  imports: [AutocompleteTextboxComponent, FieldComponent, ReactiveFormsModule],
  templateUrl: './autocomplete-textbox-field.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AutocompleteTextboxFieldComponent),
      multi: true,
    },
  ],
})
export class AutocompleteTextboxFieldComponent<T = unknown> implements ControlValueAccessor {
  readonly label = input.required<string>();
  readonly searchMethod = input.required<SearchMethod<T>>();

  readonly displayWith = input<(option: T) => string>((option: T) => String(option));
  readonly valueWith = input<(option: T) => string>((option: T) => String(option));
  readonly resolveByValue = input<
    (value: string) => Observable<T | null> | Promise<T | null> | T | null
  >(() => null);
  readonly trackByValueInput = input<(option: T) => string | number>();

  readonly hint = input<string>('');
  readonly error = input<string>('');
  readonly placeholder = input<string>();
  readonly emptyHint = input<string>();
  readonly debounceMs = input(350);
  readonly minChars = input(3);
  readonly required = input(false, { transform: booleanAttribute });
  readonly inputId = input<string>();
  readonly clearButtonAriaLabel = input<string>();

  readonly selectedChange = output<T | null>();

  private readonly generatedInputId = `autocomplete-textbox-field-${createUuid()}`;

  readonly controlId = computed(() => this.inputId() ?? this.generatedInputId);

  readonly describedBy = computed(() => {
    const ids: string[] = [];

    if (this.hint() && !this.error()) {
      ids.push(`${this.controlId()}-hint`);
    }

    if (this.error()) {
      ids.push(`${this.controlId()}-error`);
    }

    return ids.length ? ids.join(' ') : null;
  });

  readonly valueControl = new FormControl<string | null>(null);

  disabled = false;

  private readonly destroyRef = inject(DestroyRef);

  private onChange: (value: string | null) => void = () => undefined;
  private onTouched: () => void = () => undefined;

  constructor() {
    this.valueControl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((value) => {
      this.onChange(value);
    });
  }

  writeValue(value: string | null): void {
    this.valueControl.setValue(value, { emitEvent: false });
  }

  registerOnChange(fn: (value: string | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;

    if (isDisabled) {
      this.valueControl.disable({ emitEvent: false });
      return;
    }

    this.valueControl.enable({ emitEvent: false });
  }

  handleSelectedChange(option: T | null): void {
    this.selectedChange.emit(option);
    this.markTouched();
  }

  markTouched(): void {
    this.onTouched();
  }
}
