import { Component, forwardRef, Input } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
  selector: 'app-range-slider',
  standalone: true,
  templateUrl: './range-slider.component.html',
  styleUrl: './range-slider.component.css',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RangeSliderComponent),
      multi: true,
    },
  ],
})
export class RangeSliderComponent implements ControlValueAccessor {
  /**
   * Incremental counter used to generate unique ids per component instance,
   * preserving the accessible `label` to `input` association.
   */
  private static nextId = 0;

  /** Visible label displayed above the slider. */
  @Input() label = '';
  /** Optional suffix used to display the current value unit (for example `min`, `%`). */
  @Input() unit = '';
  /** Minimum value allowed by the control. */
  @Input() min = 0;
  /** Maximum value allowed by the control. */
  @Input() max = 100;
  /** Step size between valid slider values. */
  @Input() step = 1;

  /** Stable instance id used by `for`/`id` attributes. */
  readonly sliderId = `range-slider-${RangeSliderComponent.nextId++}`;
  /** Current numeric slider value. */
  value = 0;
  /** Disabled state propagated from Angular Forms. */
  disabled = false;

  /** Angular Forms callback used to propagate value changes. */
  private onChange: (value: number) => void = () => {};
  /** Angular Forms callback used to mark the control as touched. */
  private onTouched: () => void = () => {};

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
   * Returns the current progress percentage (0-100) used by the slider track UI.
   * Returns `0` when the range is invalid to avoid division by zero.
   *
   * @returns Progress percentage for the active slider track.
   */
  get progressPercent(): number {
    const range = this.max - this.min;
    if (range <= 0) {
      return 0;
    }

    return ((this.value - this.min) / range) * 100;
  }

  /**
   * Returns the formatted value text for display in the UI.
   *
   * @returns Value string including the configured unit when available.
   */
  get valueText(): string {
    return this.unit ? `${this.value} ${this.unit}` : `${this.value}`;
  }
}
