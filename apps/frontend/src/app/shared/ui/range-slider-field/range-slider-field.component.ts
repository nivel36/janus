/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { booleanAttribute, Component, computed, forwardRef, input } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { createUuid } from '../../utils/uuid.utils';
import { FieldComponent } from '../field/field.component';

/**
 * Range slider field compatible with Angular Forms.
 *
 * <p>This component exposes its configuration through signal-based inputs and
 * implements {@link ControlValueAccessor} so it can be bound to reactive or
 * template-driven forms.</p>
 *
 * <p>The slider id can be provided externally. When no id is supplied, the
 * component generates a stable UUID-based id for accessibility bindings.</p>
 */
@Component({
  selector: 'app-range-slider-field',
  standalone: true,
  imports: [FieldComponent],
  templateUrl: './range-slider-field.component.html',
  styleUrl: './range-slider-field.component.css',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RangeSliderFieldComponent),
      multi: true,
    },
  ],
})
export class RangeSliderFieldComponent implements ControlValueAccessor {
  readonly label = input.required<string>();
  readonly hint = input<string>('');
  readonly error = input<string>('');
  readonly required = input(false, { transform: booleanAttribute });

  /**
   * Optional suffix used in the accessible value text, for example `min` or `%`.
   */
  readonly unit = input<string>();

  /**
   * Minimum value allowed by the slider.
   */
  readonly min = input<number>(0);

  /**
   * Maximum value allowed by the slider.
   */
  readonly max = input<number>(100);

  /**
   * Step size between valid slider values.
   */
  readonly step = input<number>(1);

  /**
   * Optional externally assigned id for the native range input.
   */
  readonly inputId = input<string>();

  /**
   * Internally generated stable id used when no external id is provided.
   */
  private readonly generatedInputId = `range-slider-field-${createUuid()}`;

  /**
   * Effective id used by the native range input.
   */
  readonly controlId = computed(() => this.inputId() ?? this.generatedInputId);

  readonly describedBy = computed(() => {
    if (this.error()) {
      return `${this.controlId()}-error`;
    }

    if (this.hint()) {
      return `${this.controlId()}-hint`;
    }

    return null;
  });

  /**
   * Current numeric slider value.
   */
  value = 0;

  /**
   * Disabled state propagated from Angular Forms.
   */
  disabled = false;

  /**
   * Angular Forms callback used to propagate value changes.
   */
  private onChange: (value: number) => void = () => undefined;

  /**
   * Angular Forms callback used to mark the control as touched.
   */
  private onTouched: () => void = () => undefined;

  /**
   * Writes an external form value into the component state.
   *
   * @param value Value received from the parent form control.
   */
  writeValue(value: number | null): void {
    this.value = typeof value === 'number' ? value : 0;
  }

  /**
   * Registers the callback that Angular Forms uses to receive value updates.
   *
   * @param fn Function invoked when the component value changes.
   */
  registerOnChange(fn: (value: number) => void): void {
    this.onChange = fn;
  }

  /**
   * Registers the callback that Angular Forms uses to mark the control as touched.
   *
   * @param fn Function invoked when the control loses focus.
   */
  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  /**
   * Updates the disabled state from the parent form control.
   *
   * @param isDisabled Whether the control should be disabled.
   */
  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  /**
   * Handles the native `input` event, updates local state, and propagates the value.
   *
   * @param event Browser input event emitted by the range element.
   */
  onInput(event: Event): void {
    const target = event.target as HTMLInputElement;
    const nextValue = Number(target.value);
    this.value = nextValue;
    this.onChange(nextValue);
  }

  /**
   * Marks the control as touched when the slider loses focus.
   */
  onBlur(): void {
    this.onTouched();
  }

  /**
   * Returns the current progress percentage used by the slider track UI.
   * Returns `0` when the configured range is invalid.
   *
   * @returns Progress percentage for the active slider track.
   */
  get progressPercent(): number {
    const range = this.max() - this.min();
    if (range <= 0) {
      return 0;
    }

    return ((this.value - this.min()) / range) * 100;
  }

  /**
   * Returns the formatted value text for accessibility purposes.
   *
   * @returns Value string including the configured unit when available.
   */
  get valueText(): string {
    return this.unit() ? `${this.value} ${this.unit()}` : `${this.value}`;
  }
}
