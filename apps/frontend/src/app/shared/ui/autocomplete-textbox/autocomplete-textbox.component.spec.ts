/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';

import { AutocompleteTextboxComponent } from './autocomplete-textbox.component';

describe('AutocompleteTextboxComponent', () => {
  let fixture: ComponentFixture<AutocompleteTextboxComponent<string>>;
  let component: AutocompleteTextboxComponent<string>;
  let searchMethodSpy: jasmine.Spy<(query: string) => ReturnType<typeof of>>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AutocompleteTextboxComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(AutocompleteTextboxComponent<string>);
    component = fixture.componentInstance;
    searchMethodSpy = jasmine.createSpy('searchMethod').and.callFake((query: string) =>
      of([`${query}-1`, `${query}-2`]),
    );
    component.searchMethod = searchMethodSpy;
    component.debounceMs = 300;

    fixture.detectChanges();
  });

  function getInput(): HTMLInputElement {
    return fixture.debugElement.query(By.css('input')).nativeElement;
  }

  it('should call search method after debounce when text has more than 3 chars', fakeAsync(() => {
    const input = getInput();
    input.value = 'madr';
    input.dispatchEvent(new Event('input'));

    tick(299);
    expect(searchMethodSpy).not.toHaveBeenCalled();

    tick(1);
    fixture.detectChanges();

    expect(searchMethodSpy).toHaveBeenCalledOnceWith('madr');
    expect(component.results).toEqual(['madr-1', 'madr-2']);
  }));


  it('should cancel queued search when user backspaces to 3 chars before debounce', fakeAsync(() => {
    const input = getInput();

    input.value = 'madr';
    input.dispatchEvent(new Event('input'));

    tick(150);

    input.value = 'mad';
    input.dispatchEvent(new Event('input'));

    tick(300);
    fixture.detectChanges();

    expect(searchMethodSpy).not.toHaveBeenCalled();
    expect(component.results).toEqual([]);
  }));

  it('should not call search method when text has 3 chars or fewer', fakeAsync(() => {
    const input = getInput();
    input.value = 'mad';
    input.dispatchEvent(new Event('input'));

    tick(400);
    fixture.detectChanges();

    expect(searchMethodSpy).not.toHaveBeenCalled();
    expect(component.results).toEqual([]);
  }));

  it('should lock input and show clear button after selecting a result', fakeAsync(() => {
    component.textControl.setValue('madr');
    tick(300);
    fixture.detectChanges();

    const firstResultButton: HTMLButtonElement = fixture.debugElement.query(By.css('.results li button')).nativeElement;
    firstResultButton.click();
    fixture.detectChanges();

    const input = getInput();
    expect(component.selectedValue).toBe('madr-1');
    expect(input.readOnly).toBeTrue();
    expect(fixture.debugElement.query(By.css('.clear-button'))).not.toBeNull();
  }));

  it('should clear selection, hide clear button and make input editable again', () => {
    component.writeValue('valor fijo');
    fixture.detectChanges();

    const clearButton: HTMLButtonElement = fixture.debugElement.query(By.css('.clear-button')).nativeElement;
    clearButton.click();
    fixture.detectChanges();

    const input = getInput();
    expect(component.selectedValue).toBeNull();
    expect(input.readOnly).toBeFalse();
    expect(input.value).toBe('');
    expect(fixture.debugElement.query(By.css('.clear-button'))).toBeNull();
  });
});
