/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { booleanAttribute, Component, forwardRef, input, output } from '@angular/core';
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
  readonly role = input<string | null>(null);
  readonly placeholder = input<string>('');
  readonly type = input<string>('text');
  readonly required = input(false, { transform: booleanAttribute });
  readonly readonly = input(false, { transform: booleanAttribute });
  readonly inputId = input<string>();
  readonly ariaLabel = input<string | null>(null);
  readonly ariaDescribedBy = input<string | null>(null);
  readonly ariaLabelledBy = input<string | null>(null);
  readonly ariaInvalid = input(false, { transform: booleanAttribute });
  readonly ariaErrorMessage = input<string | null>(null);
  readonly ariaReadonly = input<boolean | string | null>(null);
  readonly ariaExpanded = input<boolean | string | null>(null);
  readonly ariaHaspopup = input<boolean | string | null>(null);
  readonly ariaControls = input<string | null>(null);
  readonly ariaActiveDescendant = input<string | null>(null);
  readonly ariaBusy = input<boolean | string | null>(null);
  readonly ariaAutocomplete = input<string | null>(null);
  readonly autocapitalize = input<string | null>(null);
  readonly autocorrect = input<string | null>(null);
  readonly spellcheck = input<boolean | string | null>(null);

  // Native event output names are intentional so app-input can replace a native input in templates.
  // eslint-disable-next-line @angular-eslint/no-output-native
  readonly keydown = output<KeyboardEvent>();
  // eslint-disable-next-line @angular-eslint/no-output-native
  readonly blur = output<FocusEvent>();

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

  onKeydown(event: KeyboardEvent): void {
    event.stopPropagation();
    this.keydown.emit(event);
  }

  onBlur(event: FocusEvent): void {
    this.onTouched();
    this.blur.emit(event);
  }

  formatBooleanAria(value: boolean | string | null): string | null {
    if (value === null) {
      return null;
    }

    if (typeof value === 'boolean') {
      return value ? 'true' : 'false';
    }

    return value;
  }
}
