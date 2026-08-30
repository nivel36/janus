/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { beforeEach, describe, expect, it } from 'vitest';

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

@Component({
  template: `
    <form [formGroup]="form">
      <app-select
        formControlName="locale"
        inputId="preferences-locale"
        ariaDescribedBy="preferences-locale-hint"
        required
        [options]="options"
      />
    </form>
  `,
  standalone: true,
  imports: [ReactiveFormsModule, SelectComponent],
})
class TestHostComponent {
  readonly options = [
    { value: 'es-ES', labelKey: 'locale.es-ES' },
    { value: 'en-GB', labelKey: 'locale.en-GB' },
  ];

  readonly form = new FormGroup({
    locale: new FormControl('es-ES'),
  });
}

describe('SelectComponent (ControlValueAccessor)', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let host: TestHostComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
    })
      .overrideComponent(SelectComponent, {
        set: {
          imports: [MockTranslatePipe],
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

  it('should assign the input id to the native select', () => {
    expect(getSelect().id).toBe('preferences-locale');
  });

  it('should reflect the initial FormControl value', () => {
    expect(getSelect().value).toBe('es-ES');
  });

  it('should update the FormControl when the selection changes', () => {
    const select = getSelect();

    select.value = 'en-GB';
    select.dispatchEvent(new Event('change', { bubbles: true }));

    expect(host.form.controls.locale.value).toBe('en-GB');
  });

  it('should update the view when the FormControl value changes', () => {
    host.form.controls.locale.setValue('en-GB');
    fixture.detectChanges();

    expect(getSelect().value).toBe('en-GB');
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
