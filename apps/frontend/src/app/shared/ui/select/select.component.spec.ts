/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Pipe, PipeTransform } from '@angular/core';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { SelectComponent } from './select.component';

@Pipe({
  name: 'translate',
  standalone: true,
})
class MockTranslatePipe implements PipeTransform {
  transform(value: string): string {
    return value;
  }
}

describe('SelectComponent', () => {
  let fixture: ComponentFixture<SelectComponent<string>>;
  let component: SelectComponent<string>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SelectComponent, MockTranslatePipe],
    })
      .overrideComponent(SelectComponent, {
        set: {
          imports: [MockTranslatePipe],
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(SelectComponent<string>);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('options', [
      { value: 'es-ES', labelKey: 'locale.es-ES' },
      { value: 'en-EN', labelKey: 'locale.en-EN' },
    ]);

    fixture.detectChanges();
  });

  it('should propagate selected value through ControlValueAccessor callback', () => {
    const onChangeSpy = vi.fn();
    component.registerOnChange(onChangeSpy);

    const select: HTMLSelectElement = fixture.debugElement.query(By.css('select')).nativeElement;

    select.value = 'en-EN';
    select.dispatchEvent(new Event('change', { bubbles: true }));

    expect(component.value).toBe('en-EN');
    expect(onChangeSpy).toHaveBeenCalledTimes(1);
    expect(onChangeSpy).toHaveBeenCalledWith('en-EN');
  });

  it('should disable native select when setDisabledState is called', () => {
    component.setDisabledState(true);
    fixture.detectChanges();

    const select: HTMLSelectElement = fixture.debugElement.query(By.css('select')).nativeElement;

    expect(component.disabled).toBe(true);
    expect(select.disabled).toBe(true);
  });
});
