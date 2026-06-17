/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { beforeEach, describe, expect, it, vi } from 'vitest';

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
          ariaLabel="Worksite name"
          role="combobox"
          [readonly]="isReadonly"
          [ariaExpanded]="isExpanded"
          ariaHaspopup="listbox"
          ariaControls="worksite-results"
          ariaActiveDescendant="worksite-option-1"
          [ariaBusy]="isBusy"
          ariaAutocomplete="list"
          autocapitalize="off"
          autocorrect="off"
          [spellcheck]="false"
          required
          (keydown)="handleKeydown($event)"
          (blur)="handleBlur($event)"
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

  isReadonly = false;
  isExpanded = false;
  isBusy = true;

  readonly handleKeydown = vi.fn();
  readonly handleBlur = vi.fn();
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

  it('should default to a text input type', () => {
    expect(getInput().type).toBe('text');
  });

  it('should forward combobox ARIA and native text attributes', () => {
    const input = getInput();

    expect(input.getAttribute('role')).toBe('combobox');
    expect(input.getAttribute('aria-label')).toBe('Worksite name');
    expect(input.getAttribute('aria-expanded')).toBe('false');
    expect(input.getAttribute('aria-haspopup')).toBe('listbox');
    expect(input.getAttribute('aria-controls')).toBe('worksite-results');
    expect(input.getAttribute('aria-activedescendant')).toBe('worksite-option-1');
    expect(input.getAttribute('aria-busy')).toBe('true');
    expect(input.getAttribute('aria-autocomplete')).toBe('list');
    expect(input.getAttribute('autocapitalize')).toBe('off');
    expect(input.getAttribute('autocorrect')).toBe('off');
    expect(input.getAttribute('spellcheck')).toBe('false');
  });

  it('should update reflected combobox state without changing the CVA value', () => {
    host.isExpanded = true;
    host.isBusy = false;
    host.isReadonly = true;
    fixture.detectChanges();

    const input = getInput();

    expect(input.getAttribute('aria-expanded')).toBe('true');
    expect(input.getAttribute('aria-busy')).toBe('false');
    expect(input.readOnly).toBe(true);
    expect(host.form.controls.name.value).toBe('Madrid Hub');
  });

  it('should emit native keydown and blur events while keeping CVA touch handling', () => {
    const input = getInput();
    const keydownEvent = new KeyboardEvent('keydown', { key: 'ArrowDown', bubbles: true });
    const blurEvent = new FocusEvent('blur');

    input.dispatchEvent(keydownEvent);
    input.dispatchEvent(blurEvent);

    expect(host.handleKeydown).toHaveBeenCalledWith(keydownEvent);
    expect(host.handleBlur).toHaveBeenCalledWith(blurEvent);
    expect(host.form.controls.name.touched).toBe(true);
  });
});
