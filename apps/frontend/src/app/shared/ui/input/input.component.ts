/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { booleanAttribute, Component, forwardRef, input } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { createUuid } from '../../utils/uuid.utils';

/**
 * Native text input with Angular Forms support.
 */
@Component({
  selector: 'app-input',
  standalone: true,
  templateUrl: './input.component.html',
  styleUrl: './input.component.css',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => InputComponent),
      multi: true,
    },
  ],
})
export class InputComponent implements ControlValueAccessor {
  readonly autocomplete = input<string>('off');
  readonly placeholder = input<string>('');
  readonly type = input<string>('text');
  readonly required = input(false, { transform: booleanAttribute });
  readonly inputId = input<string>();
  readonly ariaDescribedBy = input<string | null>(null);
  readonly ariaLabelledBy = input<string | null>(null);
  readonly ariaInvalid = input(false, { transform: booleanAttribute });
  readonly ariaErrorMessage = input<string | null>(null);

  private readonly generatedInputId = `input-${createUuid()}`;

  value = '';
  disabled = false;

  private onChange: (value: string) => void = () => undefined;
  private onTouched: () => void = () => undefined;

  get controlId(): string {
    return this.inputId() ?? this.generatedInputId;
  }

  writeValue(value: string | null): void {
    this.value = value ?? '';
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  onInput(event: Event): void {
    this.value = (event.target as HTMLInputElement).value;
    this.onChange(this.value);
  }

  markTouched(): void {
    this.onTouched();
  }
}
