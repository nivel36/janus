/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { OverlayContainer } from '@angular/cdk/overlay';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';

import { AutocompleteTextboxComponent } from './autocomplete-textbox.component';

describe('AutocompleteTextboxComponent', () => {
  let fixture: ComponentFixture<AutocompleteTextboxComponent<string>>;
  let component: AutocompleteTextboxComponent<string>;
  let searchMethodSpy: jasmine.Spy<(query: string) => ReturnType<typeof of>>;
  let overlayContainer: OverlayContainer;
  let overlayContainerElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AutocompleteTextboxComponent],
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

    fixture.detectChanges();
  });

  afterEach(() => {
    overlayContainer.ngOnDestroy();
  });

  function getInput(): HTMLInputElement {
    return fixture.debugElement.query(By.css('input')).nativeElement;
  }

  function getOverlayResultButtons(): HTMLButtonElement[] {
    return Array.from(
      overlayContainerElement.querySelectorAll('.results li button'),
    ) as HTMLButtonElement[];
  }

  it('should call search method after debounce when text has at least 3 chars', fakeAsync(() => {
    const input = getInput();
    input.value = 'mad';
    input.dispatchEvent(new Event('input'));

    tick(299);
    expect(searchMethodSpy).not.toHaveBeenCalled();

    tick(1);
    fixture.detectChanges();

    expect(searchMethodSpy).toHaveBeenCalledOnceWith('mad');
    expect(component.results).toEqual(['mad-1', 'mad-2']);
    expect(getOverlayResultButtons().length).toBe(2);
  }));

  it('should cancel queued search when user backspaces to fewer than 3 chars before debounce', fakeAsync(() => {
    const input = getInput();

    input.value = 'madr';
    input.dispatchEvent(new Event('input'));

    tick(150);

    input.value = 'ma';
    input.dispatchEvent(new Event('input'));

    tick(300);
    fixture.detectChanges();

    expect(searchMethodSpy).not.toHaveBeenCalled();
    expect(component.results).toEqual([]);
    expect(getOverlayResultButtons().length).toBe(0);
  }));

  it('should not call search method when text has fewer than 3 chars', fakeAsync(() => {
    const input = getInput();
    input.value = 'ma';
    input.dispatchEvent(new Event('input'));

    tick(400);
    fixture.detectChanges();

    expect(searchMethodSpy).not.toHaveBeenCalled();
    expect(component.results).toEqual([]);
    expect(getOverlayResultButtons().length).toBe(0);
  }));

  it('should lock input and show clear button after selecting a result', fakeAsync(() => {
    component.textControl.setValue('madr');
    tick(300);
    fixture.detectChanges();

    const firstResultButton = getOverlayResultButtons()[0];
    expect(firstResultButton).toBeTruthy();

    firstResultButton.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
    fixture.detectChanges();

    const input = getInput();
    expect(component.selectedValue).toBe('madr-1');
    expect(input.readOnly).toBeTrue();
    expect(fixture.debugElement.query(By.css('.clear-button'))).not.toBeNull();
  }));

  it('should not allow selecting results when disabled', fakeAsync(() => {
    component.textControl.setValue('madr');
    tick(300);
    fixture.detectChanges();

    component.setDisabledState(true);
    fixture.detectChanges();

    const firstResultButton = getOverlayResultButtons()[0];
    expect(firstResultButton).toBeTruthy();

    firstResultButton.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
    fixture.detectChanges();

    expect(component.selectedValue).toBeNull();
    expect(component.textControl.value).toBe('madr');
  }));

  it('should clear visible results when writeValue(null) is called', fakeAsync(() => {
    component.textControl.setValue('madr');
    tick(300);
    fixture.detectChanges();

    expect(component.results.length).toBeGreaterThan(0);
    expect(getOverlayResultButtons().length).toBeGreaterThan(0);

    component.writeValue(null);
    fixture.detectChanges();

    expect(component.results).toEqual([]);
    expect(component.textControl.value).toBe('');
    expect(overlayContainerElement.querySelector('.results')).toBeNull();
  }));

  it('should clear selection, hide clear button and make input editable again', () => {
    component.writeValue('valor fijo');
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
    expect(fixture.debugElement.query(By.css('.clear-button'))).toBeNull();
  });
});
