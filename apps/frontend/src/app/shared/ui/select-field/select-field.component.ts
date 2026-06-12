/**
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  AfterViewInit,
  booleanAttribute,
  Component,
  computed,
  effect,
  ElementRef,
  forwardRef,
  input,
  ViewChild,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import { createUuid } from '../../utils/uuid.utils';
import { FieldComponent } from '../field/field.component';

/**
 * Represents a single option rendered by the select field.
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
 * Select control wrapped in an application field with Angular Forms support.
 *
 * @typeParam TValue - String-based value type handled by the component.
 */
@Component({
  selector: 'app-select-field',
  standalone: true,
  imports: [FieldComponent, TranslatePipe],
  templateUrl: './select-field.component.html',
  styleUrl: './select-field.component.css',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SelectFieldComponent),
      multi: true,
    },
  ],
})
export class SelectFieldComponent<TValue extends string = string>
  implements ControlValueAccessor, AfterViewInit
{
  readonly label = input.required<string>();
  readonly options = input.required<readonly SelectOption<TValue>[]>();
  readonly hint = input<string>('');
  readonly error = input<string>('');
  readonly required = input(false, { transform: booleanAttribute });
  readonly inputId = input<string>();

  private readonly generatedInputId = `select-field-${createUuid()}`;

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

  @ViewChild('nativeSelect') private nativeSelect?: ElementRef<HTMLSelectElement>;

  value: TValue | null = null;
  disabled = false;

  private onChange: (value: TValue | null) => void = () => undefined;
  private onTouched: () => void = () => undefined;

  constructor() {
    effect(() => {
      this.options();
      this.syncNativeSelection();
    });
  }

  ngAfterViewInit(): void {
    this.syncNativeSelection();
  }

  writeValue(value: TValue | null): void {
    this.value = value;
    this.syncNativeSelection();
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
    const select = event.target as HTMLSelectElement;

    this.value = (select.value || null) as TValue | null;
    this.onChange(this.value);
  }

  markTouched(): void {
    this.onTouched();
  }

  private syncNativeSelection(): void {
    if (!this.nativeSelect) {
      return;
    }

    this.nativeSelect.nativeElement.value = this.value ?? '';
  }
}
