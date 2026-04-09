/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { SelectComponent } from './select.component';

describe('SelectComponent', () => {
  let fixture: ComponentFixture<SelectComponent<string>>;
  let component: SelectComponent<string>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SelectComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SelectComponent<string>);
    component = fixture.componentInstance;
    component.options = [
      { value: 'es-ES', labelKey: 'locale.es-ES' },
      { value: 'en-EN', labelKey: 'locale.en-EN' },
    ];

    fixture.detectChanges();
  });

  it('should propagate selected value through ControlValueAccessor callback', () => {
    const onChangeSpy = jasmine.createSpy('onChange');
    component.registerOnChange(onChangeSpy);

    const select: HTMLSelectElement = fixture.debugElement.query(By.css('select')).nativeElement;
    select.value = 'en-EN';
    select.dispatchEvent(new Event('change'));

    expect(component.value).toBe('en-EN');
    expect(onChangeSpy).toHaveBeenCalledOnceWith('en-EN');
  });

  it('should disable native select when setDisabledState is called', () => {
    component.setDisabledState(true);
    fixture.detectChanges();

    const select: HTMLSelectElement = fixture.debugElement.query(By.css('select')).nativeElement;

    expect(component.disabled).toBeTrue();
    expect(select.disabled).toBeTrue();
  });
});
