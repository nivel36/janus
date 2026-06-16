/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { booleanAttribute, Component, computed, forwardRef, input } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { FieldComponent } from '../field/field.component';
import { createUuid } from '../../utils/uuid.utils';

const noopToggleChange = (value: boolean): void => {
  void value;
};

const noopTouched = (): void => {
  void undefined;
};

@Component({
  selector: 'app-toggle-button-field',
  standalone: true,
  imports: [FieldComponent],
  templateUrl: './toggle-button-field.component.html',
  styleUrl: './toggle-button-field.component.css',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ToggleButtonFieldComponent),
      multi: true,
    },
  ],
})
export class ToggleButtonFieldComponent implements ControlValueAccessor {
  readonly label = input.required<string>();
  readonly hint = input<string>('');
  readonly error = input<string>('');
  readonly required = input(false, { transform: booleanAttribute });
  readonly inputId = input<string>();

  private readonly generatedInputId = `toggle-button-field-${createUuid()}`;

  readonly controlId = computed(() => this.inputId() ?? this.generatedInputId);

  readonly describedBy = computed(() => {
    const ids: string[] = [];

    if (this.hint()) {
      ids.push(`${this.controlId()}-hint`);
    }

    if (this.error()) {
      ids.push(`${this.controlId()}-error`);
    }

    return ids.length ? ids.join(' ') : null;
  });

  /**
   * Current toggle state bound to the host form control.
   */
  checked = false;

  /**
   * Prevents user interaction when the control is disabled by the form.
   */
  disabled = false;

  /**
   * Callback invoked by Angular forms when the control value changes.
   */
  private onChange: (value: boolean) => void = noopToggleChange;

  /**
   * Callback invoked by Angular forms when the control is marked as touched.
   */
  private onTouched: () => void = noopTouched;

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
   * Marks the control as touched when it loses focus.
   */
  markAsTouched(): void {
    this.onTouched();
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
