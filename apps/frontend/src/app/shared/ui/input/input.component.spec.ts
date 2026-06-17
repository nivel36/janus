/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { beforeEach, describe, expect, it } from 'vitest';

import { FieldComponent } from '../field/field.component';
import { InputComponent } from './input.component';

@Component({
  template: `
    <form [formGroup]="form">
      <app-field controlId="worksite-name" label="Name" hint="Enter a name" required>
        <app-input
          formControlName="name"
          inputId="worksite-name"
          autocomplete="organization"
          ariaLabelledBy="worksite-name-label"
          ariaDescribedBy="worksite-name-hint"
          required
        />
      </app-field>
    </form>
  `,
  standalone: true,
  imports: [ReactiveFormsModule, FieldComponent, InputComponent],
})
class TestHostComponent {
  readonly form = new FormGroup({
    name: new FormControl('Madrid Hub'),
  });
}

describe('InputComponent (ControlValueAccessor)', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let host: TestHostComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    host = fixture.componentInstance;
    fixture.detectChanges();
  });

  function getInput(): HTMLInputElement {
    return fixture.debugElement.query(By.css('input')).nativeElement;
  }

  it('should associate the field label with the native input', () => {
    const label: HTMLLabelElement = fixture.debugElement.query(By.css('label')).nativeElement;

    expect(label.htmlFor).toBe('worksite-name');
    expect(getInput().id).toBe('worksite-name');
  });

  it('should reflect the initial FormControl value', () => {
    expect(getInput().value).toBe('Madrid Hub');
  });

  it('should update the FormControl when the input changes', () => {
    const input = getInput();

    input.value = 'Barcelona Hub';
    input.dispatchEvent(new Event('input', { bubbles: true }));

    expect(host.form.controls.name.value).toBe('Barcelona Hub');
  });

  it('should update the view when the FormControl value changes', () => {
    host.form.controls.name.setValue('Valencia Hub');
    fixture.detectChanges();

    expect(getInput().value).toBe('Valencia Hub');
  });

  it('should respect the disabled state from FormControl', () => {
    host.form.controls.name.disable();
    fixture.detectChanges();

    expect(getInput().disabled).toBe(true);
  });

  it('should connect the hint through aria-describedby', () => {
    expect(getInput().getAttribute('aria-describedby')).toBe('worksite-name-hint');
  });

  it('should forward the autocomplete value', () => {
    expect(getInput().autocomplete).toBe('organization');
  });
});
