/**
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  inject,
  booleanAttribute,
  Component,
  computed,
  forwardRef,
  input,
  signal,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { ID_GENERATOR } from '../../services/id-generator.service';

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

  private readonly generatedInputId = inject(ID_GENERATOR).generate('toggle-button');

  readonly controlId = computed(() => this.inputId() ?? this.generatedInputId);

  private readonly checkedState = signal(false);
  private readonly disabledState = signal(false);

  get checked(): boolean {
    return this.checkedState();
  }

  get disabled(): boolean {
    return this.disabledState();
  }

  private onChange: (value: boolean) => void = noopToggleChange;
  private onTouched: () => void = noopTouched;

  writeValue(value: boolean | null): void {
    this.checkedState.set(!!value);
  }

  registerOnChange(fn: (value: boolean) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabledState.set(isDisabled);
  }

  markAsTouched(): void {
    this.onTouched();
  }

  onToggle(): void {
    if (this.disabled) {
      return;
    }

    this.checkedState.update((checked) => !checked);
    this.onChange(this.checked);
    this.onTouched();
  }
}
