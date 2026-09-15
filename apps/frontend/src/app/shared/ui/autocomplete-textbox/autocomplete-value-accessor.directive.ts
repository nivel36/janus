/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Directive, OnDestroy, forwardRef, inject, input } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { OutputRefSubscription } from '@angular/core';

import { AutocompleteTextboxComponent } from './autocomplete-textbox.component';

const noop = (): void => undefined;

/** Forms adapter for the headless autocomplete selection control. */
@Directive({
  selector: '[appAutocompleteValueAccessor]',
  standalone: true,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AutocompleteValueAccessorDirective),
      multi: true,
    },
  ],
})
export class AutocompleteValueAccessorDirective<T, V = T>
  implements ControlValueAccessor, OnDestroy
{
  readonly valueWith = input<(option: T) => V>((option) => option as unknown as V);
  readonly resolveByValue = input<(value: V) => T | null>((value) => value as unknown as T);

  private onChange: (value: V | null) => void = noop;
  private onTouched: () => void = noop;
  private readonly subscriptions: OutputRefSubscription[];
  private readonly control = inject(AutocompleteTextboxComponent<T>);

  constructor() {
    this.subscriptions = [
      this.control.selectedChange.subscribe((option) =>
        this.onChange(option === null ? null : this.valueWith()(option)),
      ),
      this.control.touched.subscribe(() => this.onTouched()),
    ];
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
  ngOnDestroy(): void {
    this.subscriptions.forEach((subscription) => subscription.unsubscribe());
  }
}
