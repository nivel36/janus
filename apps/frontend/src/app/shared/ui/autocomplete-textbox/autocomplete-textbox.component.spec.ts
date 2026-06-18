/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { OverlayContainer } from '@angular/cdk/overlay';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { TranslateService } from '@ngx-translate/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { AutocompleteTextboxComponent } from './autocomplete-textbox.component';
import { MockTranslateService } from '../../../../testing/mock-translate.service';

interface TestOption {
  code: string;
  label: string;
}

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, AutocompleteTextboxComponent],
  template: `
    <app-autocomplete-textbox
      [formControl]="control"
      [searchMethod]="searchMethod"
      [resolveByValue]="resolveByValue"
      [displayWith]="displayWith"
      [valueWith]="valueWith"
      ariaLabel="Country"
    />
  `,
})
class InitialValueHostComponent {
  readonly control = new FormControl<string | null>('es');
  readonly searchMethod = vi.fn<(query: string) => Observable<TestOption[]>>(() => of([]));
  readonly resolveByValue = vi.fn<(value: string) => Observable<TestOption | null>>(
    (value: string) => of(value === 'es' ? { code: 'es', label: 'España' } : null),
  );
  readonly displayWith = (option: TestOption): string => option.label;
  readonly valueWith = (option: TestOption): string => option.code;
}

describe('AutocompleteTextboxComponent', () => {
  let fixture: ComponentFixture<AutocompleteTextboxComponent<string>>;
  let component: AutocompleteTextboxComponent<string>;
  let searchMethodSpy: ReturnType<typeof vi.fn<(query: string) => Observable<string[]>>>;
  let overlayContainer: OverlayContainer;
  let overlayContainerElement: HTMLElement;

  beforeEach(async () => {
    vi.useFakeTimers();

    Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', {
      configurable: true,
      value: vi.fn(),
    });

    await TestBed.configureTestingModule({
      imports: [AutocompleteTextboxComponent, InitialValueHostComponent],
      providers: [{ provide: TranslateService, useClass: MockTranslateService }],
    }).compileComponents();

    overlayContainer = TestBed.inject(OverlayContainer);
    overlayContainerElement = overlayContainer.getContainerElement();

    fixture = TestBed.createComponent(AutocompleteTextboxComponent<string>);
    component = fixture.componentInstance;

    searchMethodSpy = vi.fn<(query: string) => Observable<string[]>>((query: string) =>
      of([`${query}-1`, `${query}-2`]),
    );

    fixture.componentRef.setInput('searchMethod', searchMethodSpy);
    fixture.componentRef.setInput('debounceMs', 300);
    fixture.componentRef.setInput('minChars', 3);
    fixture.componentRef.setInput('ariaLabel', 'Autocomplete');

    fixture.detectChanges();
  });

  afterEach(() => {
    overlayContainerElement.innerHTML = '';
    vi.clearAllTimers();
    vi.useRealTimers();
  });

  function getInput(): HTMLInputElement {
    return fixture.debugElement.query(By.css('input')).nativeElement;
  }

  function setInputValue(value: string): void {
    const input = getInput();
    input.value = value;
    input.dispatchEvent(new Event('input', { bubbles: true }));
    fixture.detectChanges();
  }

  function getOverlayOptions(): HTMLLIElement[] {
    return Array.from(
      overlayContainerElement.querySelectorAll('.autocomplete__results li[role="option"]'),
    ) as HTMLLIElement[];
  }

  function getOverlayMessage(): HTMLElement | null {
    return overlayContainerElement.querySelector('.autocomplete__results .autocomplete__message');
  }

  function selectOverlayOption(index: number): void {
    const option = getOverlayOptions()[index];
    expect(option).toBeTruthy();

    option.dispatchEvent(new PointerEvent('pointerdown', { bubbles: true }));
    option.click();
    fixture.detectChanges();
  }

  it('should call search method after debounce when text has at least minChars characters', async () => {
    setInputValue('mad');

    await vi.advanceTimersByTimeAsync(299);
    expect(searchMethodSpy).not.toHaveBeenCalled();

    await vi.advanceTimersByTimeAsync(1);
    fixture.detectChanges();

    expect(searchMethodSpy).toHaveBeenCalledTimes(1);
    expect(searchMethodSpy).toHaveBeenCalledWith('mad');
    expect(component.results).toEqual(['mad-1', 'mad-2']);
    expect(getOverlayOptions().length).toBe(2);
    expect(component.panel.kind).toBe('results');
  });

  it('should cancel queued search when user backspaces to fewer than minChars before debounce', async () => {
    setInputValue('madr');

    await vi.advanceTimersByTimeAsync(150);

    setInputValue('ma');

    await vi.advanceTimersByTimeAsync(300);
    fixture.detectChanges();

    expect(searchMethodSpy).not.toHaveBeenCalled();
    expect(component.results).toEqual([]);
    expect(getOverlayOptions().length).toBe(0);
    expect(component.isOverlayOpen).toBe(false);
  });

  it('should not call search method when text has fewer than minChars characters', async () => {
    setInputValue('ma');

    await vi.advanceTimersByTimeAsync(400);
    fixture.detectChanges();

    expect(searchMethodSpy).not.toHaveBeenCalled();
    expect(component.results).toEqual([]);
    expect(getOverlayOptions().length).toBe(0);
    expect(component.isOverlayOpen).toBe(false);
  });

  it('should show results in the overlay after a successful search', async () => {
    setInputValue('madr');

    await vi.advanceTimersByTimeAsync(300);
    fixture.detectChanges();

    const options = getOverlayOptions();

    expect(component.isOverlayOpen).toBe(true);
    expect(options.length).toBe(2);
    expect(options[0].textContent?.trim()).toBe('madr-1');
    expect(options[1].textContent?.trim()).toBe('madr-2');
  });

  it('should lock input, emit selection and show clear button after selecting a result', async () => {
    const onChangeSpy = vi.fn();
    const selectedChangeSpy = vi.fn();

    component.registerOnChange(onChangeSpy);
    component.selectedChange.subscribe(selectedChangeSpy);

    setInputValue('madr');
    await vi.advanceTimersByTimeAsync(300);
    fixture.detectChanges();

    selectOverlayOption(0);

    const input = getInput();

    expect(component.selectedValue).toBe('madr-1');
    expect(input.readOnly).toBe(true);
    expect(input.value).toBe('madr-1');
    expect(component.isOverlayOpen).toBe(false);
    expect(fixture.debugElement.query(By.css('app-button button'))).not.toBeNull();
    expect(onChangeSpy).toHaveBeenCalledTimes(1);
    expect(onChangeSpy).toHaveBeenCalledWith('madr-1');
    expect(selectedChangeSpy).toHaveBeenCalledTimes(1);
    expect(selectedChangeSpy).toHaveBeenCalledWith('madr-1');
  });

  it('should not select an option when component is disabled', () => {
    component.setDisabledState(true);

    component.onSelect('manual-option');
    fixture.detectChanges();

    expect(component.selectedValue).toBeNull();
    expect(component.textControl.value).toBe('');
    expect(component.isOverlayOpen).toBe(false);
  });

  it('should close overlay and disable input control when disabled', async () => {
    setInputValue('madr');
    await vi.advanceTimersByTimeAsync(300);
    fixture.detectChanges();

    expect(component.isOverlayOpen).toBe(true);
    expect(getOverlayOptions().length).toBe(2);

    component.setDisabledState(true);
    fixture.detectChanges();

    expect(component.disabled).toBe(true);
    expect(component.textControl.disabled).toBe(true);
    expect(component.isOverlayOpen).toBe(false);
    expect(getOverlayOptions().length).toBe(0);
  });

  it('should clear visible results when writeValue(null) is called', async () => {
    setInputValue('madr');
    await vi.advanceTimersByTimeAsync(300);
    fixture.detectChanges();

    expect(component.results.length).toBeGreaterThan(0);
    expect(getOverlayOptions().length).toBeGreaterThan(0);

    component.writeValue(null);
    await Promise.resolve();
    fixture.detectChanges();

    expect(component.results).toEqual([]);
    expect(component.textControl.value).toBe('');
    expect(component.selectedValue).toBeNull();
    expect(component.isOverlayOpen).toBe(false);
    expect(overlayContainerElement.querySelector('.autocomplete__results')).toBeNull();
  });

  it('should rebuild visible text from a FormControl initial value before first detectChanges', () => {
    const hostFixture = TestBed.createComponent(InitialValueHostComponent);
    const hostComponent = hostFixture.componentInstance;

    expect(hostComponent.resolveByValue).not.toHaveBeenCalled();

    hostFixture.detectChanges();

    const input: HTMLInputElement = hostFixture.debugElement.query(By.css('input')).nativeElement;

    expect(hostComponent.resolveByValue).toHaveBeenCalledTimes(1);
    expect(hostComponent.resolveByValue).toHaveBeenCalledWith('es');
    expect(input.value).toBe('España');
    expect(input.readOnly).toBe(true);
  });

  it('should resolve external value into a selected option when writeValue is used', async () => {
    fixture.componentRef.setInput('resolveByValue', (value: string) => value);
    fixture.detectChanges();

    component.writeValue('valor fijo');
    await Promise.resolve();
    fixture.detectChanges();

    const input = getInput();

    expect(component.selectedValue).toBe('valor fijo');
    expect(input.value).toBe('valor fijo');
    expect(input.readOnly).toBe(true);
    expect(fixture.debugElement.query(By.css('app-button button'))).not.toBeNull();
  });

  it('should keep fallback text without selection when resolveByValue returns null', async () => {
    fixture.componentRef.setInput('resolveByValue', () => null);
    fixture.detectChanges();

    component.writeValue('texto externo');
    await Promise.resolve();
    fixture.detectChanges();

    const input = getInput();

    expect(component.selectedValue).toBeNull();
    expect(input.value).toBe('texto externo');
    expect(input.readOnly).toBe(false);
    expect(fixture.debugElement.query(By.css('app-button button'))).toBeNull();
  });

  it('should clear selection, hide clear button and make input editable again', async () => {
    fixture.componentRef.setInput('resolveByValue', (value: string) => value);
    fixture.detectChanges();

    component.writeValue('valor fijo');
    await Promise.resolve();
    fixture.detectChanges();

    const clearButton: HTMLButtonElement = fixture.debugElement.query(
      By.css('app-button button'),
    ).nativeElement;

    clearButton.click();
    fixture.detectChanges();

    const input = getInput();

    expect(component.selectedValue).toBeNull();
    expect(input.readOnly).toBe(false);
    expect(input.value).toBe('');
    expect(component.textControl.value).toBe('');
    expect(fixture.debugElement.query(By.css('app-button button'))).toBeNull();
  });

  it('should show empty hint when no value is selected', () => {
    fixture.componentRef.setInput('emptyHint', 'Search timezone');
    fixture.detectChanges();

    const input = getInput();
    expect(input.placeholder).toBe('Search timezone');
  });

  it('should prefer placeholder when emptyHint is empty', () => {
    fixture.componentRef.setInput('placeholder', 'Type here');
    fixture.componentRef.setInput('emptyHint', '');
    fixture.detectChanges();

    const input = getInput();
    expect(input.placeholder).toBe('Type here');
  });

  it('should render empty state message when search returns no results', async () => {
    searchMethodSpy.mockImplementation(() => of([]));

    setInputValue('madr');
    await vi.advanceTimersByTimeAsync(300);
    fixture.detectChanges();

    expect(component.panel.kind).toBe('empty');
    expect(component.results).toEqual([]);
    expect(getOverlayOptions().length).toBe(0);
    expect(getOverlayMessage()?.textContent?.trim()).toBe('autocomplete.noResultsFound');
  });

  it('should move active option with keyboard navigation', async () => {
    setInputValue('madr');
    await vi.advanceTimersByTimeAsync(300);
    fixture.detectChanges();

    const input = getInput();

    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowDown', bubbles: true }));
    fixture.detectChanges();

    expect(component.activeIndex).toBe(0);
    expect(component.isActive(0)).toBe(true);

    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowDown', bubbles: true }));
    fixture.detectChanges();

    expect(component.activeIndex).toBe(1);
    expect(component.isActive(1)).toBe(true);

    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowUp', bubbles: true }));
    fixture.detectChanges();

    expect(component.activeIndex).toBe(0);
    expect(component.isActive(0)).toBe(true);
  });

  it('should select the active option when pressing Enter', async () => {
    const onChangeSpy = vi.fn();
    component.registerOnChange(onChangeSpy);

    setInputValue('madr');
    await vi.advanceTimersByTimeAsync(300);
    fixture.detectChanges();

    const input = getInput();

    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowDown', bubbles: true }));
    fixture.detectChanges();

    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
    fixture.detectChanges();

    expect(component.selectedValue).toBe('madr-1');
    expect(component.textControl.value).toBe('madr-1');
    expect(onChangeSpy).toHaveBeenCalledTimes(1);
    expect(onChangeSpy).toHaveBeenCalledWith('madr-1');
    expect(component.isOverlayOpen).toBe(false);
  });

  it('should close overlay when pressing Escape', async () => {
    setInputValue('madr');
    await vi.advanceTimersByTimeAsync(300);
    fixture.detectChanges();

    expect(component.isOverlayOpen).toBe(true);

    const input = getInput();
    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    fixture.detectChanges();

    expect(component.isOverlayOpen).toBe(false);
    expect(component.panel.kind).toBe('closed');
  });

  it('should mark control as touched on blur', () => {
    const onTouchedSpy = vi.fn();
    component.registerOnTouched(onTouchedSpy);

    component.handleBlur();

    expect(onTouchedSpy).toHaveBeenCalled();
  });

  it('should expose loading state while search is in progress', async () => {
    searchMethodSpy.mockImplementation(() => {
      return new Observable<string[]>((subscriber) => {
        setTimeout(() => {
          subscriber.next(['late-1', 'late-2']);
          subscriber.complete();
        }, 100);
      });
    });

    setInputValue('madr');
    await vi.advanceTimersByTimeAsync(300);
    fixture.detectChanges();

    expect(component.panel.kind).toBe('loading');
    expect(component.isLoading).toBe(true);
    expect(getOverlayMessage()?.textContent?.trim()).toBe('autocomplete.loadingResults');

    await vi.advanceTimersByTimeAsync(100);
    fixture.detectChanges();

    expect(component.panel.kind).toBe('results');
    expect(component.isLoading).toBe(false);
    expect(component.results).toEqual(['late-1', 'late-2']);
  });

  it('should return stable option ids', () => {
    expect(component.getOptionId(0)).toMatch(/^autocomplete-option-\d+-0$/);
    expect(component.getOptionId(3)).toMatch(/^autocomplete-option-\d+-3$/);
  });
});
