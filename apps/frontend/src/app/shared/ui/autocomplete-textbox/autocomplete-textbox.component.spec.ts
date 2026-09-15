/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { OverlayContainer } from '@angular/cdk/overlay';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { TranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { MockTranslateService } from '../../../../testing/mock-translate.service';
import { AutocompleteTextboxComponent } from './autocomplete-textbox.component';
import { AutocompleteValueAccessorDirective } from './autocomplete-value-accessor.directive';

interface Option {
  code: string;
  label: string;
}

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, AutocompleteTextboxComponent, AutocompleteValueAccessorDirective],
  template: `
    <app-autocomplete-textbox
      appAutocompleteValueAccessor
      [formControl]="control"
      [items]="items"
      [loading]="loading"
      [displayWith]="displayWith"
      [valueWith]="valueWith"
      [resolveByValue]="resolveByValue"
      (queryChange)="queries.push($event)"
      ariaLabel="Country"
    />
  `,
})
class HostComponent {
  readonly control = new FormControl<string | null>('es');
  readonly items: Option[] = [
    { code: 'es', label: 'España' },
    { code: 'fr', label: 'Francia' },
  ];
  loading = false;
  readonly queries: string[] = [];
  readonly displayWith = (option: Option): string => option.label;
  readonly valueWith = (option: Option): string => option.code;
  readonly resolveByValue = (value: string): Option | null =>
    this.items.find((option) => option.code === value) ?? null;
}

describe('AutocompleteTextboxComponent', () => {
  let fixture: ComponentFixture<HostComponent>;
  let overlay: HTMLElement;

  beforeEach(async () => {
    Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', {
      configurable: true,
      value: vi.fn(),
    });
    await TestBed.configureTestingModule({
      imports: [HostComponent],
      providers: [{ provide: TranslateService, useClass: MockTranslateService }],
    }).compileComponents();
    fixture = TestBed.createComponent(HostComponent);
    overlay = TestBed.inject(OverlayContainer).getContainerElement();
    fixture.detectChanges();
  });

  it('resolves a generic form value through the forms adapter', () => {
    const input: HTMLInputElement = fixture.debugElement.query(By.css('input')).nativeElement;
    expect(input.value).toBe('España');
  });

  it('emits queries without owning debounce or data fetching', () => {
    const input: HTMLInputElement = fixture.debugElement.query(By.css('input')).nativeElement;
    fixture.componentInstance.control.setValue(null);
    input.value = 'fra';
    input.dispatchEvent(new Event('input', { bubbles: true }));
    fixture.detectChanges();
    expect(fixture.componentInstance.queries).toContain('fra');
    expect(overlay.querySelectorAll('[role="option"]')).toHaveLength(2);
  });

  it('serializes selection to the generic form value', () => {
    fixture.componentInstance.control.setValue(null);
    const input: HTMLInputElement = fixture.debugElement.query(By.css('input')).nativeElement;
    input.value = 'fra';
    input.dispatchEvent(new Event('input', { bubbles: true }));
    fixture.detectChanges();
    (overlay.querySelectorAll('[role="option"]')[1] as HTMLElement).click();
    fixture.detectChanges();
    expect(fixture.componentInstance.control.value).toBe('fr');
    expect(input.value).toBe('Francia');
  });

  it('renders externally supplied loading state', () => {
    fixture.componentInstance.control.setValue(null);
    fixture.componentInstance.loading = true;
    const input: HTMLInputElement = fixture.debugElement.query(By.css('input')).nativeElement;
    input.value = 'fra';
    input.dispatchEvent(new Event('input', { bubbles: true }));
    fixture.detectChanges();
    expect(overlay.textContent).toContain('autocomplete.loadingResults');
    expect(input.getAttribute('aria-busy')).toBe('true');
  });
});
