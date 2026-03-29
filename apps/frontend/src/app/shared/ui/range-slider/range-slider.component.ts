import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-range-slider',
  standalone: true,
  templateUrl: './range-slider.component.html',
  styleUrl: './range-slider.component.css',
})
export class RangeSliderComponent {
  @Input() label = '';
  @Input() unit = '';
  @Input() min = 0;
  @Input() max = 100;
  @Input() step = 1;
  @Input() value = 0;
  @Input() disabled = false;

  @Output() valueChange = new EventEmitter<number>();

  onInput(event: Event): void {
    const target = event.target as HTMLInputElement;
    const nextValue = Number(target.value);
    this.value = nextValue;
    this.valueChange.emit(nextValue);
  }

  get progressPercent(): number {
    const range = this.max - this.min;
    if (range <= 0) {
      return 0;
    }

    return ((this.value - this.min) / range) * 100;
  }
}
