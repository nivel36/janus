/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { CommonModule } from '@angular/common';
import { Component, Input, forwardRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

/**
 * Option item rendered in the select dropdown.
 */
export interface SelectOption<TValue extends string = string> {
  /**
   * Persisted value propagated through Angular Forms.
   */
  value: TValue;

  /**
   * Translation key rendered as label.
   */
  labelKey: string;
}

/**
 * Styled select component that keeps a consistent visual appearance
 * with the shared autocomplete component.
 */
@Component({
  selector: 'app-select',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  templateUrl: './select.component.html',
  styleUrl: './select.component.css',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SelectComponent),
      multi: true,
    },
  ],
})
export class SelectComponent<TValue extends string = string> implements ControlValueAccessor {
  /**
   * Options displayed in the native select element.
   */
  @Input({ required: true }) options: readonly SelectOption<TValue>[] = [];

  /**
   * Identifier forwarded to the native control.
   */
  @Input() id = '';

  /**
   * Accessible name used when no external label exists.
   */
  @Input() ariaLabel = '';

  value: TValue | null = null;
  disabled = false;

  private onChange: (value: TValue | null) => void = () => {};
  private onTouched: () => void = () => {};

  writeValue(value: TValue | null): void {
    this.value = value;
  }

  registerOnChange(fn: (value: TValue | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  onSelectionChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value as TValue;
    this.value = value;
    this.onChange(value);
  }

  markTouched(): void {
    this.onTouched();
  }
}
