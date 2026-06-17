/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { MockTranslatePipe } from '../../../../testing/mock-translate.pipe';
import { ButtonComponent } from '../button/button.component';
import { InputComponent } from '../input/input.component';
import { InputGroupComponent } from '../input-group/input-group.component';
import { SearchBarComponent } from './search-bar.component';

describe('SearchBarComponent', () => {
  let fixture: ComponentFixture<SearchBarComponent>;
  let component: SearchBarComponent;

  async function createComponent(
    inputs?: Partial<{
      debounceMs: number;
      minChars: number;
    }>,
  ): Promise<ComponentFixture<SearchBarComponent>> {
    const createdFixture = TestBed.createComponent(SearchBarComponent);

    if (inputs?.debounceMs !== undefined) {
      createdFixture.componentRef.setInput('debounceMs', inputs.debounceMs);
    }

    if (inputs?.minChars !== undefined) {
      createdFixture.componentRef.setInput('minChars', inputs.minChars);
    }

    createdFixture.detectChanges();
    await createdFixture.whenStable();

    return createdFixture;
  }

  function getInput(currentFixture: ComponentFixture<SearchBarComponent>): HTMLInputElement {
    const inputDe = currentFixture.debugElement.query(By.css('input'));

    if (!inputDe) {
      throw new Error('SearchBarComponent test: no input element was found.');
    }

    return inputDe.nativeElement as HTMLInputElement;
  }

  function setInputValue(
    currentFixture: ComponentFixture<SearchBarComponent>,
    value: string,
  ): void {
    const input = getInput(currentFixture);
    input.value = value;
    input.dispatchEvent(new Event('input', { bubbles: true }));
    currentFixture.detectChanges();
  }

  function submitFromDom(currentFixture: ComponentFixture<SearchBarComponent>): void {
    const submitButtonDe = currentFixture.debugElement.query(By.css('button[type="submit"]'));

    if (submitButtonDe) {
      (submitButtonDe.nativeElement as HTMLButtonElement).click();
      currentFixture.detectChanges();
      return;
    }

    const formDe = currentFixture.debugElement.query(By.css('form'));

    if (formDe) {
      formDe.nativeElement.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
      currentFixture.detectChanges();
      return;
    }

    throw new Error('SearchBarComponent test: no submit button or form element was found.');
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SearchBarComponent],
    })
      .overrideComponent(SearchBarComponent, {
        remove: {
          imports: [],
        },
        add: {
          imports: [MockTranslatePipe],
        },
      })
      .compileComponents();

    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.clearAllMocks();
    vi.clearAllTimers();
    vi.useRealTimers();
  });

  it('should create', async () => {
    fixture = await createComponent();
    component = fixture.componentInstance;

    expect(component).toBeTruthy();
  });

  it('should render the shared button component as a submit button', async () => {
    fixture = await createComponent();

    const buttonComponentDe = fixture.debugElement.query(By.directive(ButtonComponent));
    const submitButtonDe = fixture.debugElement.query(By.css('button[type="submit"]'));

    expect(buttonComponentDe).toBeTruthy();
    expect(submitButtonDe).toBeTruthy();
  });

  it('should compose the search input with the shared input group', async () => {
    fixture = await createComponent();

    const inputGroupDe = fixture.debugElement.query(By.directive(InputGroupComponent));
    const inputComponentDe = fixture.debugElement.query(By.directive(InputComponent));
    const input = getInput(fixture);

    expect(inputGroupDe).toBeTruthy();
    expect(inputComponentDe).toBeTruthy();
    expect(input.type).toBe('search');
  });

  it('should throw when debounceMs is negative', async () => {
    await expect(async () => {
      await createComponent({ debounceMs: -1 });
    }).rejects.toThrow('SearchBarComponent: debounceMs cannot be negative.');
  });

  it('should throw when minChars is negative', async () => {
    await expect(async () => {
      await createComponent({ minChars: -1 });
    }).rejects.toThrow('SearchBarComponent: minChars cannot be negative.');
  });

  it('should trim the query before emitting it', async () => {
    fixture = await createComponent({ debounceMs: 300, minChars: 3 });
    component = fixture.componentInstance;

    const emitSpy = vi.spyOn(component.queryChange, 'emit');

    setInputValue(fixture, '   abc   ');
    vi.advanceTimersByTime(300);

    expect(emitSpy).toHaveBeenCalledTimes(1);
    expect(emitSpy).toHaveBeenCalledWith('abc');
  });

  it('should debounce value changes', async () => {
    fixture = await createComponent({ debounceMs: 300, minChars: 1 });
    component = fixture.componentInstance;

    const emitSpy = vi.spyOn(component.queryChange, 'emit');

    setInputValue(fixture, 'a');

    vi.advanceTimersByTime(299);
    expect(emitSpy).not.toHaveBeenCalled();

    vi.advanceTimersByTime(1);
    expect(emitSpy).toHaveBeenCalledTimes(1);
    expect(emitSpy).toHaveBeenCalledWith('a');
  });

  it('should ignore consecutive identical trimmed queries', async () => {
    fixture = await createComponent({ debounceMs: 300, minChars: 1 });
    component = fixture.componentInstance;

    const emitSpy = vi.spyOn(component.queryChange, 'emit');

    setInputValue(fixture, 'abc');
    vi.advanceTimersByTime(300);

    setInputValue(fixture, '   abc   ');
    vi.advanceTimersByTime(300);

    expect(emitSpy).toHaveBeenCalledTimes(1);
    expect(emitSpy).toHaveBeenCalledWith('abc');
  });

  it('should not emit when query length is below minChars', async () => {
    fixture = await createComponent({ debounceMs: 300, minChars: 3 });
    component = fixture.componentInstance;

    const emitSpy = vi.spyOn(component.queryChange, 'emit');

    setInputValue(fixture, 'a');
    vi.advanceTimersByTime(300);

    setInputValue(fixture, 'ab');
    vi.advanceTimersByTime(300);

    expect(emitSpy).not.toHaveBeenCalled();
  });

  it('should emit when query length reaches minChars', async () => {
    fixture = await createComponent({ debounceMs: 300, minChars: 3 });
    component = fixture.componentInstance;

    const emitSpy = vi.spyOn(component.queryChange, 'emit');

    setInputValue(fixture, 'abc');
    vi.advanceTimersByTime(300);

    expect(emitSpy).toHaveBeenCalledTimes(1);
    expect(emitSpy).toHaveBeenCalledWith('abc');
  });

  it('should emit an empty query when the trimmed value is empty', async () => {
    fixture = await createComponent({ debounceMs: 300, minChars: 3 });
    component = fixture.componentInstance;

    const emitSpy = vi.spyOn(component.queryChange, 'emit');

    setInputValue(fixture, '   ');
    vi.advanceTimersByTime(300);

    expect(emitSpy).toHaveBeenCalledTimes(1);
    expect(emitSpy).toHaveBeenCalledWith('');
  });

  it('should emit the current trimmed query on explicit submit', async () => {
    fixture = await createComponent({ debounceMs: 300, minChars: 3 });
    component = fixture.componentInstance;

    const emitSpy = vi.spyOn(component.queryChange, 'emit');

    setInputValue(fixture, '   abcd   ');
    submitFromDom(fixture);

    expect(emitSpy).toHaveBeenCalledTimes(1);
    expect(emitSpy).toHaveBeenCalledWith('abcd');
  });

  it('should not emit on explicit submit when query length is below minChars', async () => {
    fixture = await createComponent({ debounceMs: 300, minChars: 3 });
    component = fixture.componentInstance;

    const emitSpy = vi.spyOn(component.queryChange, 'emit');

    setInputValue(fixture, 'ab');
    submitFromDom(fixture);

    expect(emitSpy).not.toHaveBeenCalled();
  });

  it('should emit an empty query on explicit submit when trimmed value is empty', async () => {
    fixture = await createComponent({ debounceMs: 300, minChars: 3 });
    component = fixture.componentInstance;

    const emitSpy = vi.spyOn(component.queryChange, 'emit');

    setInputValue(fixture, '   ');
    submitFromDom(fixture);

    expect(emitSpy).toHaveBeenCalledTimes(1);
    expect(emitSpy).toHaveBeenCalledWith('');
  });
});
