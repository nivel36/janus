/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { beforeEach, describe, expect, it } from 'vitest';

import { ToggleButtonComponent } from './toggle-button.component';

@Component({
  template: `
    <form [formGroup]="form">
      <label>
        Notifications
        <app-toggle-button formControlName="flag"></app-toggle-button>
      </label>
    </form>
  `,
  standalone: true,
  imports: [ReactiveFormsModule, ToggleButtonComponent],
})
class TestHostComponent {
  form = new FormGroup({
    flag: new FormControl(false),
  });
}

describe('ToggleButtonComponent (ControlValueAccessor)', () => {
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

  function getButton(): HTMLButtonElement {
    return fixture.debugElement.query(By.css('button')).nativeElement;
  }

  it('should reflect the initial FormControl value', () => {
    const button = getButton();
    expect(button.getAttribute('aria-checked')).toBe('false');
  });

  it('should update the view when the FormControl value changes', () => {
    host.form.get('flag')?.setValue(true);
    fixture.detectChanges();

    const button = getButton();
    expect(button.getAttribute('aria-checked')).toBe('true');
  });

  it('should not override the visible label with aria-label', () => {
    const button = getButton();
    expect(button.getAttribute('aria-label')).toBeNull();
  });

  it('should update the FormControl when clicked', () => {
    const button = getButton();

    button.click();
    fixture.detectChanges();

    expect(host.form.get('flag')?.value).toBe(true);
  });

  it('should toggle true/false on successive clicks', () => {
    const button = getButton();

    button.click();
    fixture.detectChanges();
    expect(host.form.get('flag')?.value).toBe(true);

    button.click();
    fixture.detectChanges();
    expect(host.form.get('flag')?.value).toBe(false);
  });

  it('should respect the disabled state from FormControl', () => {
    host.form.get('flag')?.disable();
    fixture.detectChanges();

    const button = getButton();
    expect(button.disabled).toBe(true);

    button.click();
    fixture.detectChanges();

    expect(host.form.get('flag')?.value).toBe(false);
  });

  it('should not emit changes when disabled', () => {
    host.form.get('flag')?.disable();
    fixture.detectChanges();

    const button = getButton();
    button.click();
    fixture.detectChanges();

    expect(host.form.get('flag')?.value).toBe(false);
  });
});
