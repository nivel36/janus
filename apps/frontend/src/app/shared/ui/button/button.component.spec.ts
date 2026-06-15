/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ButtonComponent } from './button.component';

@Component({
  standalone: true,
  imports: [ButtonComponent],
  template: `
    <app-button
      [variant]="variant"
      [type]="type"
      [disabled]="disabled"
      [styleClass]="styleClass"
      [ariaLabel]="ariaLabel"
      (clicked)="onClicked($event)"
    >
      Save
    </app-button>
  `,
})
class TestHostComponent {
  variant: 'default' | 'main' | 'secondary' = 'default';
  type: 'button' | 'submit' | 'reset' = 'button';
  disabled = false;
  styleClass = '';
  ariaLabel: string | undefined = undefined;

  onClicked(_event: MouseEvent): void {
    // Intentionally empty.
  }
}

describe('ButtonComponent', () => {
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
    return fixture.nativeElement.querySelector('button');
  }

  it('should create', () => {
    const buttonDebugEl = fixture.debugElement.query(By.directive(ButtonComponent));
    expect(buttonDebugEl).toBeTruthy();
  });

  it('should render the default variant with the default type', () => {
    const buttonEl = getButton();

    expect(buttonEl).toBeTruthy();
    expect(buttonEl.type).toBe('button');
    expect(buttonEl.classList).toContain('app-button--default');
  });

  it('should disable the button when disabled is true', () => {
    host.disabled = true;
    fixture.detectChanges();

    const buttonEl = getButton();
    expect(buttonEl.disabled).toBe(true);
  });

  it('should expose aria-label when provided', () => {
    host.ariaLabel = 'Open menu';
    fixture.detectChanges();

    const buttonEl = getButton();
    expect(buttonEl.getAttribute('aria-label')).toBe('Open menu');
  });

  it('should not render aria-label when not provided', () => {
    host.ariaLabel = undefined;
    fixture.detectChanges();

    const buttonEl = getButton();
    expect(buttonEl.hasAttribute('aria-label')).toBe(false);
  });

  it('should emit clicked when the button is pressed', () => {
    const onClickedSpy = vi.spyOn(host, 'onClicked');

    const buttonEl = getButton();
    buttonEl.click();

    expect(onClickedSpy).toHaveBeenCalled();
  });
});
