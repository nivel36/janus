/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { OverlayContainer } from '@angular/cdk/overlay';
import { By } from '@angular/platform-browser';
import { Observable, of } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';

import { AutocompleteTextboxComponent } from './autocomplete-textbox.component';

class MockTranslateService {
  instant(key: string, params?: Record<string, unknown>): string {
    if (key === 'autocomplete.manyResultsAvailable' && params?.['count'] !== undefined) {
      return `autocomplete.manyResultsAvailable:${params['count']}`;
    }

    return key;
  }
}

describe('AutocompleteTextboxComponent', () => {
  let fixture: ComponentFixture<AutocompleteTextboxComponent<string>>;
  let component: AutocompleteTextboxComponent<string>;
  let searchMethodSpy: jasmine.Spy<(query: string) => Observable<string[]>>;
  let overlayContainer: OverlayContainer;
  let overlayContainerElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AutocompleteTextboxComponent],
      providers: [
        {
          provide: TranslateService,
          useClass: MockTranslateService,
        },
      ],
    }).compileComponents();

    overlayContainer = TestBed.inject(OverlayContainer);
    overlayContainerElement = overlayContainer.getContainerElement();

    fixture = TestBed.createComponent(AutocompleteTextboxComponent<string>);
    component = fixture.componentInstance;

    searchMethodSpy = jasmine
      .createSpy('searchMethod')
      .and.callFake((query: string) => of([`${query}-1`, `${query}-2`]));

    component.searchMethod = searchMethodSpy;
    component.debounceMs = 300;
    component.minChars = 3;

    fixture.detectChanges();
  });

  afterEach(() => {
    overlayContainer.ngOnDestroy();
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
      overlayContainerElement.querySelectorAll('.results li[role="option"]'),
    ) as HTMLLIElement[];
  }

  function getOverlayMessage(): HTMLElement | null {
    return overlayContainerElement.querySelector('.results .results-message');
  }

  function selectOverlayOption(index: number): void {
    const option = getOverlayOptions()[index];
    expect(option).toBeTruthy();

    option.dispatchEvent(new PointerEvent('pointerdown', { bubbles: true }));
    option.click();
    fixture.detectChanges();
  }

  it('should call search method after debounce when text has at least minChars characters', fakeAsync(() => {
    setInputValue('mad');

    tick(299);
    expect(searchMethodSpy).not.toHaveBeenCalled();

    tick(1);
    fixture.detectChanges();

    expect(searchMethodSpy).toHaveBeenCalledOnceWith('mad');
    expect(component.results).toEqual(['mad-1', 'mad-2']);
    expect(getOverlayOptions().length).toBe(2);
    expect(component.panel.kind).toBe('results');
  }));

  it('should cancel queued search when user backspaces to fewer than minChars before debounce', fakeAsync(() => {
    setInputValue('madr');

    tick(150);

    setInputValue('ma');

    tick(300);
    fixture.detectChanges();

    expect(searchMethodSpy).not.toHaveBeenCalled();
    expect(component.results).toEqual([]);
    expect(getOverlayOptions().length).toBe(0);
    expect(component.isOverlayOpen).toBeFalse();
  }));

  it('should not call search method when text has fewer than minChars characters', fakeAsync(() => {
    setInputValue('ma');

    tick(400);
    fixture.detectChanges();

    expect(searchMethodSpy).not.toHaveBeenCalled();
    expect(component.results).toEqual([]);
    expect(getOverlayOptions().length).toBe(0);
    expect(component.isOverlayOpen).toBeFalse();
  }));

  it('should show results in the overlay after a successful search', fakeAsync(() => {
    setInputValue('madr');

    tick(300);
    fixture.detectChanges();

    const options = getOverlayOptions();

    expect(component.isOverlayOpen).toBeTrue();
    expect(options.length).toBe(2);
    expect(options[0].textContent?.trim()).toBe('madr-1');
    expect(options[1].textContent?.trim()).toBe('madr-2');
  }));

  it('should lock input, emit selection and show clear button after selecting a result', fakeAsync(() => {
    const onChangeSpy = jasmine.createSpy('onChange');
    const selectedChangeSpy = jasmine.createSpy('selectedChange');

    component.registerOnChange(onChangeSpy);
    component.selectedChange.subscribe(selectedChangeSpy);

    setInputValue('madr');
    tick(300);
    fixture.detectChanges();

    selectOverlayOption(0);

    const input = getInput();

    expect(component.selectedValue).toBe('madr-1');
    expect(input.readOnly).toBeTrue();
    expect(input.value).toBe('madr-1');
    expect(component.isOverlayOpen).toBeFalse();
    expect(fixture.debugElement.query(By.css('.clear-button'))).not.toBeNull();
    expect(onChangeSpy).toHaveBeenCalledOnceWith('madr-1');
    expect(selectedChangeSpy).toHaveBeenCalledOnceWith('madr-1');
  }));

  it('should not select an option when component is disabled', () => {
    component.setDisabledState(true);

    component.onSelect('manual-option');
    fixture.detectChanges();

    expect(component.selectedValue).toBeNull();
    expect(component.textControl.value).toBe('');
    expect(component.isOverlayOpen).toBeFalse();
  });

  it('should close overlay and disable input control when disabled', fakeAsync(() => {
    setInputValue('madr');
    tick(300);
    fixture.detectChanges();

    expect(component.isOverlayOpen).toBeTrue();
    expect(getOverlayOptions().length).toBe(2);

    component.setDisabledState(true);
    fixture.detectChanges();

    expect(component.disabled).toBeTrue();
    expect(component.textControl.disabled).toBeTrue();
    expect(component.isOverlayOpen).toBeFalse();
    expect(getOverlayOptions().length).toBe(0);
  }));

  it('should clear visible results when writeValue(null) is called', fakeAsync(() => {
    setInputValue('madr');
    tick(300);
    fixture.detectChanges();

    expect(component.results.length).toBeGreaterThan(0);
    expect(getOverlayOptions().length).toBeGreaterThan(0);

    component.writeValue(null);
    fixture.detectChanges();

    expect(component.results).toEqual([]);
    expect(component.textControl.value).toBe('');
    expect(component.selectedValue).toBeNull();
    expect(component.isOverlayOpen).toBeFalse();
    expect(overlayContainerElement.querySelector('.results')).toBeNull();
  }));

  it('should resolve external value into a selected option when writeValue is used', fakeAsync(() => {
    component.resolveByValue = (value: string) => value;
    fixture.detectChanges();

    component.writeValue('valor fijo');
    tick();
    fixture.detectChanges();

    const input = getInput();

    expect(component.selectedValue).toBe('valor fijo');
    expect(input.value).toBe('valor fijo');
    expect(input.readOnly).toBeTrue();
    expect(fixture.debugElement.query(By.css('.clear-button'))).not.toBeNull();
  }));

  it('should keep fallback text without selection when resolveByValue returns null', fakeAsync(() => {
    component.resolveByValue = () => null;
    fixture.detectChanges();

    component.writeValue('texto externo');
    tick();
    fixture.detectChanges();

    const input = getInput();

    expect(component.selectedValue).toBeNull();
    expect(input.value).toBe('texto externo');
    expect(input.readOnly).toBeFalse();
    expect(fixture.debugElement.query(By.css('.clear-button'))).toBeNull();
  }));

  it('should clear selection, hide clear button and make input editable again', fakeAsync(() => {
    component.resolveByValue = (value: string) => value;
    fixture.detectChanges();

    component.writeValue('valor fijo');
    tick();
    fixture.detectChanges();

    const clearButton: HTMLButtonElement = fixture.debugElement.query(
      By.css('.clear-button'),
    ).nativeElement;

    clearButton.click();
    fixture.detectChanges();

    const input = getInput();

    expect(component.selectedValue).toBeNull();
    expect(input.readOnly).toBeFalse();
    expect(input.value).toBe('');
    expect(component.textControl.value).toBe('');
    expect(fixture.debugElement.query(By.css('.clear-button'))).toBeNull();
  }));

  it('should show empty hint when no value is selected', () => {
    component.emptyHint = 'Search timezone';
    fixture.detectChanges();

    const input = getInput();
    expect(input.placeholder).toBe('Search timezone');
  });

  it('should prefer placeholder when emptyHint is empty', () => {
    component.placeholder = 'Type here';
    component.emptyHint = '';
    fixture.detectChanges();

    const input = getInput();
    expect(input.placeholder).toBe('Type here');
  });

  it('should render empty state message when search returns no results', fakeAsync(() => {
    searchMethodSpy.and.returnValue(of([]));

    setInputValue('madr');
    tick(300);
    fixture.detectChanges();

    expect(component.panel.kind).toBe('empty');
    expect(component.results).toEqual([]);
    expect(getOverlayOptions().length).toBe(0);
    expect(getOverlayMessage()?.textContent?.trim()).toBe('autocomplete.noResultsFound');
  }));

  it('should move active option with keyboard navigation', fakeAsync(() => {
    setInputValue('madr');
    tick(300);
    fixture.detectChanges();

    const input = getInput();

    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowDown', bubbles: true }));
    fixture.detectChanges();

    expect(component.activeIndex).toBe(0);
    expect(component.isActive(0)).toBeTrue();

    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowDown', bubbles: true }));
    fixture.detectChanges();

    expect(component.activeIndex).toBe(1);
    expect(component.isActive(1)).toBeTrue();

    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowUp', bubbles: true }));
    fixture.detectChanges();

    expect(component.activeIndex).toBe(0);
    expect(component.isActive(0)).toBeTrue();
  }));

  it('should select the active option when pressing Enter', fakeAsync(() => {
    const onChangeSpy = jasmine.createSpy('onChange');
    component.registerOnChange(onChangeSpy);

    setInputValue('madr');
    tick(300);
    fixture.detectChanges();

    const input = getInput();

    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowDown', bubbles: true }));
    fixture.detectChanges();

    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
    fixture.detectChanges();

    expect(component.selectedValue).toBe('madr-1');
    expect(component.textControl.value).toBe('madr-1');
    expect(onChangeSpy).toHaveBeenCalledOnceWith('madr-1');
    expect(component.isOverlayOpen).toBeFalse();
  }));

  it('should close overlay when pressing Escape', fakeAsync(() => {
    setInputValue('madr');
    tick(300);
    fixture.detectChanges();

    expect(component.isOverlayOpen).toBeTrue();

    const input = getInput();
    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    fixture.detectChanges();

    expect(component.isOverlayOpen).toBeFalse();
    expect(component.panel.kind).toBe('closed');
  }));

  it('should mark control as touched on blur', () => {
    const onTouchedSpy = jasmine.createSpy('onTouched');
    component.registerOnTouched(onTouchedSpy);

    component.handleBlur();

    expect(onTouchedSpy).toHaveBeenCalled();
  });

  it('should expose loading state while search is in progress', fakeAsync(() => {
    searchMethodSpy.and.callFake(() => {
      return new Observable<string[]>((subscriber) => {
        setTimeout(() => {
          subscriber.next(['late-1', 'late-2']);
          subscriber.complete();
        }, 100);
      });
    });

    setInputValue('madr');
    tick(300);
    fixture.detectChanges();

    expect(component.panel.kind).toBe('loading');
    expect(component.isLoading).toBeTrue();
    expect(getOverlayMessage()?.textContent?.trim()).toBe('autocomplete.loadingResults');

    tick(100);
    fixture.detectChanges();

    expect(component.panel.kind).toBe('results');
    expect(component.isLoading).toBeFalse();
    expect(component.results).toEqual(['late-1', 'late-2']);
  }));

  it('should return stable option ids', () => {
    expect(component.getOptionId(0)).toMatch(/^autocomplete-option-\d+-0$/);
    expect(component.getOptionId(3)).toMatch(/^autocomplete-option-\d+-3$/);
  });
});
