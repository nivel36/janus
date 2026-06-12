/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { RangeSliderFieldComponent } from './range-slider-field.component';

describe('RangeSliderFieldComponent', () => {
  let fixture: ComponentFixture<RangeSliderFieldComponent>;
  let component: RangeSliderFieldComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RangeSliderFieldComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(RangeSliderFieldComponent);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('label', 'Days until locked');
    fixture.componentRef.setInput('min', 0);
    fixture.componentRef.setInput('max', 100);
    fixture.componentRef.setInput('step', 5);

    fixture.detectChanges();
  });

  it('should propagate input value through ControlValueAccessor onChange callback', () => {
    const onChangeSpy = vi.fn();
    component.registerOnChange(onChangeSpy);

    const slider: HTMLInputElement = fixture.debugElement.query(
      By.css('input[type="range"]'),
    ).nativeElement;

    slider.value = '40';
    slider.dispatchEvent(new Event('input', { bubbles: true }));

    expect(component.value).toBe(40);
    expect(onChangeSpy).toHaveBeenCalledTimes(1);
    expect(onChangeSpy).toHaveBeenCalledWith(40);
  });

  it('should call onTouched callback on blur', () => {
    const onTouchedSpy = vi.fn();
    component.registerOnTouched(onTouchedSpy);

    const slider: HTMLInputElement = fixture.debugElement.query(
      By.css('input[type="range"]'),
    ).nativeElement;

    slider.dispatchEvent(new Event('blur', { bubbles: true }));

    expect(onTouchedSpy).toHaveBeenCalledTimes(1);
  });

  it('should disable native input when setDisabledState is called', () => {
    component.setDisabledState(true);
    fixture.detectChanges();

    const slider: HTMLInputElement = fixture.debugElement.query(
      By.css('input[type="range"]'),
    ).nativeElement;

    expect(component.disabled).toBe(true);
    expect(slider.disabled).toBe(true);
  });

  it('should use provided inputId when present', () => {
    fixture.componentRef.setInput('inputId', 'custom-slider-id');
    fixture.detectChanges();

    expect(component.controlId()).toBe('custom-slider-id');
  });

  it('should generate a field control id when inputId is not provided', () => {
    expect(component.controlId()).toMatch(/^range-slider-field-/);
  });

  it('should expose the value text with unit when unit is configured', () => {
    fixture.componentRef.setInput('unit', '%');
    component.writeValue(55);

    expect(component.valueText).toBe('55 %');
  });

  it('should display the current value and unit', () => {
    fixture.componentRef.setInput('unit', 'days');
    component.writeValue(30);
    fixture.detectChanges();

    const value: HTMLElement = fixture.debugElement.query(
      By.css('.range-slider__value'),
    ).nativeElement;

    expect(value.textContent?.trim()).toBe('30 days');
  });

  it('should return 0 progress when max is less than or equal to min', () => {
    fixture.componentRef.setInput('min', 10);
    fixture.componentRef.setInput('max', 10);
    component.writeValue(10);

    expect(component.progressPercent).toBe(0);
  });

  it('should associate the field label with the range input', () => {
    const label: HTMLLabelElement = fixture.debugElement.query(By.css('label')).nativeElement;
    const slider: HTMLInputElement = fixture.debugElement.query(
      By.css('input[type="range"]'),
    ).nativeElement;

    expect(label.htmlFor).toBe(slider.id);
    expect(label.textContent?.trim()).toBe('Days until locked');
  });

  it('should expose hint and error accessibility attributes', () => {
    fixture.componentRef.setInput('hint', 'Choose a number of days');
    fixture.detectChanges();

    const slider: HTMLInputElement = fixture.debugElement.query(
      By.css('input[type="range"]'),
    ).nativeElement;

    expect(slider.getAttribute('aria-describedby')).toBe(`${component.controlId()}-hint`);

    fixture.componentRef.setInput('error', 'Invalid number of days');
    fixture.detectChanges();

    expect(slider.getAttribute('aria-describedby')).toBe(`${component.controlId()}-error`);
    expect(slider.getAttribute('aria-invalid')).toBe('true');
    expect(slider.getAttribute('aria-errormessage')).toBe(`${component.controlId()}-error`);
  });
});
