/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { ButtonComponent } from '../button/button.component';
import { InputComponent } from '../input/input.component';
import { InputGroupComponent } from './input-group.component';

@Component({
  standalone: true,
  imports: [InputComponent, InputGroupComponent],
  template: `
    <app-input-group ariaLabel="Price filter">
      <span inputGroupAddon>$</span>
      <app-input placeholder="Price" />
      <span inputGroupAddon>.00</span>
    </app-input-group>
  `,
})
class TextAddonHostComponent {}

@Component({
  standalone: true,
  imports: [ButtonComponent, InputComponent, InputGroupComponent],
  template: `
    <app-input-group ariaLabel="Search keyword">
      <app-button type="submit">Search</app-button>
      <app-input placeholder="Keyword" />
    </app-input-group>
  `,
})
class ButtonAddonHostComponent {}

@Component({
  standalone: true,
  imports: [ButtonComponent, InputComponent, InputGroupComponent],
  template: `
    <app-input-group ariaLabel="Account" [disabled]="disabled()">
      <app-button [disabled]="disabled()">Find</app-button>
      <app-input placeholder="Account number" />
      <span inputGroupAddon>#</span>
    </app-input-group>
  `,
})
class StateHostComponent {
  readonly disabled = signal(false);
}

describe('InputGroupComponent', () => {
  it('should group text addons with the shared input component', async () => {
    const fixture = await createFixture(TextAddonHostComponent);

    const group = fixture.debugElement.query(By.css('[role="group"]'));
    const addons = fixture.debugElement.queryAll(By.css('[inputGroupAddon]'));
    const input = fixture.debugElement.query(By.directive(InputComponent));

    expect(group.attributes['aria-label']).toBe('Price filter');
    expect(addons.map((addon) => addon.nativeElement.textContent.trim())).toEqual(['$', '.00']);
    expect(input).toBeTruthy();
  });

  it('should allow the shared button component as an addon', async () => {
    const fixture = await createFixture(ButtonAddonHostComponent);

    const button = fixture.debugElement.query(By.directive(ButtonComponent));
    const input = fixture.debugElement.query(By.directive(InputComponent));

    expect(button).toBeTruthy();
    expect(input).toBeTruthy();
  });

  it('should configure first, intermediate and last projected hosts through CSS properties', async () => {
    const fixture = await createFixture(StateHostComponent);
    const items = fixture.nativeElement.querySelector('.input-group').children;

    expect(
      getComputedStyle(items[0])
        .getPropertyValue('--button-control-border-radius')
        .replaceAll(/\s+/g, ''),
    ).toBe('var(--form-control-border-radius)00var(--form-control-border-radius)');
    expect(getComputedStyle(items[1]).getPropertyValue('--input-border-radius').trim()).toBe('0');
    expect(
      getComputedStyle(items[2]).getPropertyValue('--input-border-radius').replaceAll(/\s+/g, ''),
    ).toBe('0var(--form-control-border-radius)var(--form-control-border-radius)0');
  });

  it('should lift the focused child while the group owns the focus ring', async () => {
    const fixture = await createFixture(StateHostComponent);
    const inputHost = fixture.nativeElement.querySelector('app-input');
    const input = inputHost.querySelector('input') as HTMLInputElement;

    input.focus();

    expect(document.activeElement).toBe(input);
    expect(getComputedStyle(inputHost).zIndex).toBe('1');
    expect(getComputedStyle(inputHost).getPropertyValue('--input-focus-shadow').trim()).toBe(
      'none',
    );
  });

  it('should expose disabled state on the group, addon and button', async () => {
    const fixture = await createFixture(StateHostComponent);
    fixture.componentInstance.disabled.set(true);
    fixture.detectChanges();

    const group = fixture.nativeElement.querySelector('.input-group');
    const addon = fixture.nativeElement.querySelector('[inputGroupAddon]');
    const button = fixture.nativeElement.querySelector('button');

    expect(group.classList.contains('input-group--disabled')).toBe(true);
    expect(getComputedStyle(addon).opacity).toBe('var(--form-disabled-opacity)');
    expect(button.disabled).toBe(true);
  });
});

async function createFixture<T>(component: new () => T): Promise<ComponentFixture<T>> {
  await TestBed.configureTestingModule({ imports: [component] }).compileComponents();
  const fixture = TestBed.createComponent(component);
  fixture.detectChanges();
  return fixture;
}
