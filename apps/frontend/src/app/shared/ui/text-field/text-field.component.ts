/**
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  booleanAttribute,
  Component,
  computed,
  forwardRef,
  input,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { createUuid } from '../../utils/uuid.utils';
import { FieldComponent } from '../field/field.component';

/**
 * Text input wrapped in an application field with Angular Forms support.
 */
@Component({
  selector: 'app-text-field',
  standalone: true,
  imports: [FieldComponent],
  templateUrl: './text-field.component.html',
  styleUrl: './text-field.component.css',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TextFieldComponent),
      multi: true,
    },
  ],
})
export class TextFieldComponent implements ControlValueAccessor {
  readonly label = input.required<string>();
  readonly hint = input<string>('');
  readonly error = input<string>('');
  readonly autocomplete = input<string>('off');
  readonly required = input(false, { transform: booleanAttribute });
  readonly inputId = input<string>();

  private readonly generatedInputId = `text-field-${createUuid()}`;

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

  value = '';
  disabled = false;

  private onChange: (value: string) => void = () => undefined;
  private onTouched: () => void = () => undefined;

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
