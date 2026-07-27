/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MessageComponent, MessagePresentation, MessageType } from './message.component';

@Component({
  standalone: true,
  imports: [MessageComponent],
  template: `<app-message [type]="type" [presentation]="presentation"
    >Translated text</app-message
  >`,
})
class TestHostComponent {
  type: MessageType = 'info';
  presentation: MessagePresentation = 'panel';
}

describe('MessageComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [TestHostComponent] }).compileComponents();
    fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
  });

  it.each(['error', 'success', 'warning', 'info'] as const)('applies the %s variant', (type) => {
    fixture.componentInstance.type = type;
    fixture.detectChanges();
    expect(message().classList).toContain(`message--${type}`);
  });

  it.each(['inline', 'panel'] as const)('applies the %s presentation', (presentation) => {
    fixture.componentInstance.presentation = presentation;
    fixture.detectChanges();
    expect(message().classList).toContain(`message--${presentation}`);
  });

  it('projects content and exposes errors as alerts', () => {
    fixture.componentInstance.type = 'error';
    fixture.detectChanges();
    expect(message().textContent?.trim()).toBe('Translated text');
    expect(message().getAttribute('role')).toBe('alert');
    expect(message().getAttribute('aria-live')).toBe('assertive');
  });

  it('politely announces informational states', () => {
    expect(message().hasAttribute('role')).toBe(false);
    expect(message().getAttribute('aria-live')).toBe('polite');
  });

  function message(): HTMLElement {
    return fixture.nativeElement.querySelector('.message');
  }
});
