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
  @Input() label = '';
  @Input() unit = '';
  @Input() min = 0;
  @Input() max = 100;
  @Input() step = 1;

  value = 0;
  disabled = false;

  private onChange: (value: number) => void = () => {};
  private onTouched: () => void = () => {};

  writeValue(value: number | null): void {
    this.value = typeof value === 'number' ? value : 0;
  }

  registerOnChange(fn: (value: number) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  onInput(event: Event): void {
    const target = event.target as HTMLInputElement;
    const nextValue = Number(target.value);
    this.value = nextValue;
    this.onChange(nextValue);
  }

  onBlur(): void {
    this.onTouched();
  }

  get progressPercent(): number {
    const range = this.max - this.min;
    if (range <= 0) {
      return 0;
    }

    return ((this.value - this.min) / range) * 100;
  }
}
