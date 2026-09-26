/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { booleanAttribute, Component, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

/**
 * Represents a single option rendered by the select component.
 *
 * @typeParam TValue - String-based value type propagated by Angular Forms.
 */
export interface SelectOption<TValue extends string = string> {
  /**
   * Value assigned to the native option element and propagated to the form model.
   */
  value: TValue;

  /**
   * Translation key used to render the visible option label.
   */
  labelKey: string;
}

/**
 * Select control with Angular Forms support.
 *
 * @typeParam TValue - String-based value type handled by the component.
 */
@Component({
  selector: 'app-select',
  standalone: true,
  imports: [TranslatePipe],
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
  readonly options = input.required<readonly SelectOption<TValue>[]>();
  readonly required = input(false, { transform: booleanAttribute });
  readonly inputId = input.required<string>();
  readonly ariaDescribedBy = input<string | null>(null);
  readonly ariaInvalid = input(false, { transform: booleanAttribute });

  private readonly valueState = signal<TValue | null>(null);
  private readonly disabledState = signal(false);

  get value(): TValue | null {
    return this.valueState();
  }

  get disabled(): boolean {
    return this.disabledState();
  }

  private onChange: (value: TValue | null) => void = () => undefined;
  private onTouched: () => void = () => undefined;

  writeValue(value: TValue | null): void {
    this.valueState.set(value);
  }

  registerOnChange(fn: (value: TValue | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabledState.set(isDisabled);
  }

  onSelectionChange(event: Event): void {
    const select = event.target as HTMLSelectElement;

    this.valueState.set((select.value || null) as TValue | null);
    this.onChange(this.value);
  }

  markTouched(): void {
    this.onTouched();
  }
}
