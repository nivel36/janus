/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { booleanAttribute, Component, computed, forwardRef, input } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { createUuid } from '../../utils/uuid.utils';

const noopToggleChange = (value: boolean): void => {
  void value;
};

const noopTouched = (): void => {
  void undefined;
};

@Component({
  selector: 'app-toggle-button',
  standalone: true,
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
  readonly inputId = input<string>();
  readonly required = input(false, { transform: booleanAttribute });
  readonly ariaDescribedBy = input<string | null>(null);
  readonly ariaErrorMessage = input<string | null>(null);
  readonly ariaInvalid = input(false, { transform: booleanAttribute });
  readonly ariaLabelledBy = input<string | null>(null);

  private readonly generatedInputId = `toggle-button-${createUuid()}`;

  readonly controlId = computed(() => this.inputId() ?? this.generatedInputId);

  checked = false;
  disabled = false;

  private onChange: (value: boolean) => void = noopToggleChange;
  private onTouched: () => void = noopTouched;

  writeValue(value: boolean | null): void {
    this.checked = !!value;
  }

  registerOnChange(fn: (value: boolean) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  markAsTouched(): void {
    this.onTouched();
  }

  onToggle(): void {
    if (this.disabled) {
      return;
    }

    this.checked = !this.checked;
    this.onChange(this.checked);
    this.onTouched();
  }
}
