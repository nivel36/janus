/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { faUsers } from '@fortawesome/free-solid-svg-icons';
import { beforeEach, describe, expect, it } from 'vitest';

import { SummaryCardComponent } from './summary-card.component';

@Component({
  standalone: true,
  imports: [SummaryCardComponent],
  template: `
    <app-summary-card
      [icon]="icon"
      [label]="label"
      [value]="value"
      [styleClass]="styleClass"
    />
  `,
})
class TestHostComponent {
  icon = faUsers;
  label = 'Employees';
  value: string | number = 24;
  styleClass = '';
}

describe('SummaryCardComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let hostComponent: TestHostComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    hostComponent = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    const summaryCardDebugElement = fixture.debugElement.query(By.directive(SummaryCardComponent));

    expect(summaryCardDebugElement).toBeTruthy();
  });

  it('should render the label and value', () => {
    const labelElement = fixture.nativeElement.querySelector('.summary-card__label');
    const valueElement = fixture.nativeElement.querySelector('.summary-card__value');

    expect(labelElement.textContent.trim()).toBe('Employees');
    expect(valueElement.textContent.trim()).toBe('24');
  });

  it('should apply custom CSS classes to the card root', () => {
    hostComponent.styleClass = 'custom-summary-card';
    fixture.detectChanges();

    const cardElement = fixture.nativeElement.querySelector('.summary-card');

    expect(cardElement.classList.contains('custom-summary-card')).toBe(true);
  });
});
