/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { CommonModule } from '@angular/common';
import { Component, forwardRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
  selector: 'app-toggle-button',
  standalone: true,
  imports: [],
  templateUrl: './toggle-button.component.html',
  styleUrl: './toggle-button.component.css',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ToggleButtonComponent),
      multi: true,
    },
  ],
})
export class ToggleButtonComponent implements ControlValueAccessor {

  /** Current toggle state bound to the host form control. */
  checked = false;
  /** Prevents user interaction when the control is disabled by the form. */
  disabled = false;

  /** Callback invoked by Angular forms when the control value changes. */
  private onChange: (value: boolean) => void = () => {};
  /** Callback invoked by Angular forms when the control is marked as touched. */
  private onTouched: () => void = () => {};

  /**
   * Receives a value from the form model and normalizes it to a boolean.
   * @param value Incoming control value from Angular forms.
   */
  writeValue(value: boolean | null): void {
    this.checked = !!value;
  }

  /**
   * Registers the callback used to notify Angular forms when the value changes.
   * @param fn Change propagation callback provided by Angular forms.
   */
  registerOnChange(fn: (value: boolean) => void): void {
    this.onChange = fn;
  }

  /**
   * Registers the callback used to notify Angular forms when the control is touched.
   * @param fn Touch callback provided by Angular forms.
   */
  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  /**
   * Synchronizes the disabled state from the parent form control.
   * @param isDisabled Whether the control should be disabled.
   */
  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  /**
   * Toggles the control value and propagates change and touch events.
   * No action is taken when the control is disabled.
   */
  onToggle(): void {
    if (this.disabled) {
      return;
    }

    this.checked = !this.checked;
    this.onChange(this.checked);
    this.onTouched();
  }
}
