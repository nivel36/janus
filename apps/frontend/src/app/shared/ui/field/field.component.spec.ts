/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { describe, expect, it } from 'vitest';

import { FieldComponent } from './field.component';

@Component({
  template: `
    <app-field
      controlId="name"
      styleClass="custom-field"
    >
      <input id="name" />
    </app-field>
  `,
  standalone: true,
  imports: [FieldComponent],
})
class TestHostComponent {}

describe('FieldComponent', () => {
  it('should add extra CSS classes to the field container', async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
    }).compileComponents();

    const fixture: ComponentFixture<TestHostComponent> = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();

    const field = fixture.debugElement.query(By.css('.app-field')).nativeElement as HTMLElement;

    expect(field.classList.contains('custom-field')).toBe(true);
  });
});
