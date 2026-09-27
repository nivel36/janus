/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { DestroyRef, Directive, forwardRef, inject, input } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { outputToObservable, takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AutocompleteTextboxComponent } from './autocomplete-textbox.component';

const noop = (): void => undefined;

/** Forms adapter for the headless autocomplete selection control. */
@Directive({
  selector: '[appAutocompleteValueAccessor]',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AutocompleteValueAccessorDirective),
      multi: true,
    },
  ],
})
export class AutocompleteValueAccessorDirective<T, V = T> implements ControlValueAccessor {
  readonly valueWith = input<(option: T) => V>((option) => option as unknown as V);
  readonly resolveByValue = input<(value: V) => T | null>((value) => value as unknown as T);

  private onChange: (value: V | null) => void = noop;
  private onTouched: () => void = noop;
  private readonly control = inject(AutocompleteTextboxComponent<T>);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    outputToObservable(this.control.selectedChange)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((option) => this.onChange(option === null ? null : this.valueWith()(option)));
    outputToObservable(this.control.touched)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.onTouched());
  }

  writeValue(value: V | null): void {
    if (value === null || value === undefined || value === '') {
      this.control.setSelection(null);
      return;
    }
    this.control.setSelection(this.resolveByValue()(value), String(value));
  }
  registerOnChange(fn: (value: V | null) => void): void {
    this.onChange = fn;
  }
  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }
  setDisabledState(disabled: boolean): void {
    this.control.setDisabledState(disabled);
  }
}
