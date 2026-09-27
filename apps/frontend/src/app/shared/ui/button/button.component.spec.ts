/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ButtonComponent, ButtonType, ButtonVariant } from './button.component';

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
      [title]="title"
      (clicked)="onClicked($event)"
    >
      Save
    </app-button>
  `,
})
class TestHostComponent {
  variant: ButtonVariant = 'default';
  type: ButtonType = 'button';
  disabled = false;
  styleClass = '';
  ariaLabel: string | undefined = undefined;
  title: string | undefined = undefined;

  onClicked(_event: MouseEvent): void {
    void _event;
  }
}
@Component({
  standalone: true,
  imports: [ButtonComponent],
  template: ` <app-button disabled> Save </app-button> `,
})
class DisabledAttributeHostComponent {}

@Component({
  standalone: true,
  imports: [ButtonComponent],
  template: ` <app-button icon> Save </app-button> `,
})
class IconAttributeHostComponent {}

@Component({
  standalone: true,
  imports: [ButtonComponent],
  template: ` <app-button variant="main" routerLink="/worksites/new"> New </app-button> `,
})
class LinkHostComponent {}

describe('ButtonComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let host: TestHostComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        TestHostComponent,
        DisabledAttributeHostComponent,
        IconAttributeHostComponent,
        LinkHostComponent,
      ],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    host = fixture.componentInstance;
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();
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

  it.each([
    ['default', 'button'],
    ['main', 'submit'],
    ['secondary', 'reset'],
    ['text', 'button'],
  ] as const)('exposes the %s variant and %s native type consistently', async (variant, type) => {
    host.variant = variant;
    host.type = type;
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();

    const buttonEl = getButton();
    expect(buttonEl.type).toBe(type);
    expect(buttonEl.classList).toContain(`app-button--${variant}`);
  });

  it('should disable the button when disabled is true', async () => {
    host.disabled = true;
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();

    const buttonEl = getButton();
    expect(buttonEl.disabled).toBe(true);
  });

  it('should disable the button when disabled is provided as an attribute', () => {
    const attributeFixture = TestBed.createComponent(DisabledAttributeHostComponent);
    attributeFixture.detectChanges();

    const buttonEl: HTMLButtonElement = attributeFixture.nativeElement.querySelector('button');
    expect(buttonEl.disabled).toBe(true);
  });

  it('should render the icon class when icon is provided as an attribute', () => {
    const attributeFixture = TestBed.createComponent(IconAttributeHostComponent);
    attributeFixture.detectChanges();

    const buttonEl: HTMLButtonElement = attributeFixture.nativeElement.querySelector('button');
    expect(buttonEl.classList).toContain('app-button--icon');
  });

  it('should expose aria-label when provided', async () => {
    host.ariaLabel = 'Open menu';
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();

    const buttonEl = getButton();
    expect(buttonEl.getAttribute('aria-label')).toBe('Open menu');
  });

  it('should not render aria-label when not provided', () => {
    host.ariaLabel = undefined;
    fixture.detectChanges();

    const buttonEl = getButton();
    expect(buttonEl.hasAttribute('aria-label')).toBe(false);
  });

  it('should expose native tooltip text when provided', async () => {
    host.title = 'Save changes';
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();

    expect(getButton().title).toBe('Save changes');
  });

  it('should emit clicked when the button is pressed', () => {
    const onClickedSpy = vi.spyOn(host, 'onClicked');

    const buttonEl = getButton();
    buttonEl.click();

    expect(onClickedSpy).toHaveBeenCalled();
  });

  it('should render a router link instead of a button for a fixed destination', () => {
    const linkFixture = TestBed.createComponent(LinkHostComponent);
    linkFixture.detectChanges();

    const linkEl: HTMLAnchorElement = linkFixture.nativeElement.querySelector('a');
    expect(linkEl).toBeTruthy();
    expect(linkEl.getAttribute('href')).toBe('/worksites/new');
    expect(linkEl.classList).toContain('app-button--main');
    expect(linkEl.textContent?.trim()).toBe('New');
    expect(linkFixture.nativeElement.querySelector('button')).toBeNull();
  });
});
