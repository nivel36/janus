/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { AfterViewInit, Component, ElementRef, ViewChild, effect, forwardRef, input } from '@angular/core';
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
 * Native select wrapper with application-specific styling and Angular Forms support.
 *
 * <p>This component implements {@link ControlValueAccessor} so it can be bound
 * through reactive forms or template-driven forms.</p>
 *
 * @typeParam TValue - String-based value type handled by the component.
 */
@Component({
  selector: 'app-select',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './select.component.html',
  styleUrls: ['./select.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SelectComponent),
      multi: true,
    },
  ],
})
export class SelectComponent<TValue extends string = string>
  implements ControlValueAccessor, AfterViewInit
{
  /**
   * List of options rendered in the native select element.
   */
  readonly options = input.required<readonly SelectOption<TValue>[]>();

  /**
   * Optional accessible label used when no visible external label is present.
   */
  readonly ariaLabel = input<string>();

  /**
   * Optional id forwarded to the native select element.
   */
  readonly inputId = input<string>();

  /**
   * Reference to the underlying native select element.
   */
  @ViewChild('nativeSelect') private nativeSelect?: ElementRef<HTMLSelectElement>;

  /**
   * Currently selected value.
   */
  value: TValue | null = null;

  /**
   * Whether the control is disabled.
   */
  disabled = false;

  /**
   * Callback registered by Angular Forms to propagate value changes.
   */
  private onChange: (value: TValue | null) => void = () => {};

  /**
   * Callback registered by Angular Forms to mark the control as touched.
   */
  private onTouched: () => void = () => {};

  constructor() {
    effect(() => {
      this.options();
      this.syncNativeSelection();
    });
  }

  ngAfterViewInit(): void {
    this.syncNativeSelection();
  }

  /**
   * Writes an external form value into the component.
   *
   * @param value - Value provided by Angular Forms.
   */
  writeValue(value: TValue | null): void {
    this.value = value;
    this.syncNativeSelection();
  }

  /**
   * Registers the callback invoked when the component value changes.
   *
   * @param fn - Change callback provided by Angular Forms.
   */
  registerOnChange(fn: (value: TValue | null) => void): void {
    this.onChange = fn;
  }

  /**
   * Registers the callback invoked when the component is touched.
   *
   * @param fn - Touched callback provided by Angular Forms.
   */
  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  /**
   * Updates the disabled state of the component.
   *
   * @param isDisabled - {@code true} to disable the component; {@code false} otherwise.
   */
  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  /**
   * Handles selection changes coming from the native select element.
   *
   * @param event - DOM change event emitted by the select element.
   */
  onSelectionChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const selectedValue = select.value;

    this.value = (selectedValue || null) as TValue | null;
    this.onChange(this.value);
  }

  /**
   * Marks the component as touched.
   */
  markTouched(): void {
    this.onTouched();
  }

  /**
   * Synchronizes the native select value with the internal model value.
   */
  private syncNativeSelection(): void {
    if (!this.nativeSelect) {
      return;
    }

    this.nativeSelect.nativeElement.value = this.value ?? '';
  }
}
