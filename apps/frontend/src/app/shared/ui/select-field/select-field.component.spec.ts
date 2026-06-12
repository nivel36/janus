/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { beforeEach, describe, expect, it } from 'vitest';

import { FieldComponent } from '../field/field.component';
import { SelectFieldComponent } from './select-field.component';

@Pipe({
  name: 'translate',
  standalone: true,
})
class MockTranslatePipe implements PipeTransform {
  transform(value: string): string {
    return value;
  }
}

@Component({
  template: `
    <form [formGroup]="form">
      <app-select-field
        formControlName="locale"
        label="Language"
        hint="Select a language"
        inputId="preferences-locale"
        required
        [options]="options"
      />
    </form>
  `,
  standalone: true,
  imports: [ReactiveFormsModule, SelectFieldComponent],
})
class TestHostComponent {
  readonly options = [
    { value: 'es-ES', labelKey: 'locale.es-ES' },
    { value: 'en-EN', labelKey: 'locale.en-EN' },
  ];

  readonly form = new FormGroup({
    locale: new FormControl('es-ES'),
  });
}

describe('SelectFieldComponent (ControlValueAccessor)', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let host: TestHostComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
    })
      .overrideComponent(SelectFieldComponent, {
        set: {
          imports: [FieldComponent, MockTranslatePipe],
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    host = fixture.componentInstance;
    fixture.detectChanges();
  });

  function getSelect(): HTMLSelectElement {
    return fixture.debugElement.query(By.css('select')).nativeElement;
  }

  it('should associate the field label with the native select', () => {
    const label: HTMLLabelElement = fixture.debugElement.query(By.css('label')).nativeElement;

    expect(label.htmlFor).toBe('preferences-locale');
    expect(getSelect().id).toBe('preferences-locale');
  });

  it('should reflect the initial FormControl value', () => {
    expect(getSelect().value).toBe('es-ES');
  });

  it('should update the FormControl when the selection changes', () => {
    const select = getSelect();

    select.value = 'en-EN';
    select.dispatchEvent(new Event('change', { bubbles: true }));

    expect(host.form.controls.locale.value).toBe('en-EN');
  });

  it('should update the view when the FormControl value changes', () => {
    host.form.controls.locale.setValue('en-EN');
    fixture.detectChanges();

    expect(getSelect().value).toBe('en-EN');
  });

  it('should respect the disabled state from FormControl', () => {
    host.form.controls.locale.disable();
    fixture.detectChanges();

    expect(getSelect().disabled).toBe(true);
  });

  it('should connect the hint through aria-describedby', () => {
    expect(getSelect().getAttribute('aria-describedby')).toBe('preferences-locale-hint');
  });
});
