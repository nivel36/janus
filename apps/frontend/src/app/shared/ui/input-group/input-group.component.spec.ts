/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component } from '@angular/core';
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
});

async function createFixture<T>(component: new () => T): Promise<ComponentFixture<T>> {
  await TestBed.configureTestingModule({ imports: [component] }).compileComponents();
  const fixture = TestBed.createComponent(component);
  fixture.detectChanges();
  return fixture;
}
