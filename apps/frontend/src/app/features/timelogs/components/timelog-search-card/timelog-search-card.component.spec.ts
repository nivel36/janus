import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';
import { EmployeesService } from '../../../../client/api/employees.service';
import { EmployeeResponse } from '../../../../client/model/employeeResponse';
import { AutocompleteTextboxComponent } from '../../../../shared/ui/autocomplete-textbox/autocomplete-textbox.component';
import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CurrentUserFacade } from '../../../../core/user/services/current-user.facade';
import { TimelogSearchCardComponent } from './timelog-search-card.component';

describe('TimelogSearchCardComponent', () => {
  let fixture: ComponentFixture<TimelogSearchCardComponent>;
  const preferences = signal<{ defaultTimezone: string } | null>({ defaultTimezone: 'Europe/Madrid' });
  const employee: EmployeeResponse = {
    employeeNumber: 'EMP-0042', name: 'Abel', surname: 'Ferrer',
    email: 'abel@example.com', schedule: { code: 'STANDARD', name: 'Standard shift' },
  };
  const employees = { searchEmployees: vi.fn(() => of({ content: [employee] })) };
  beforeEach(async () => {
    employees.searchEmployees.mockClear();
    preferences.set({ defaultTimezone: 'Europe/Madrid' });
    await TestBed.configureTestingModule({
      imports: [TimelogSearchCardComponent],
      providers: [provideTranslateService(), { provide: CurrentUserFacade, useValue: { preferences } },
              { provide: EmployeesService, useValue: employees }],
    }).compileComponents();
    fixture = TestBed.createComponent(TimelogSearchCardComponent);
    fixture.detectChanges();
  });
  function dates(from: string, to: string): void {
    const inputs = fixture.nativeElement.querySelectorAll('input[type="date"]') as NodeListOf<HTMLInputElement>;
    [from, to].forEach((value, index) => {
      inputs[index].value = value;
      inputs[index].dispatchEvent(new Event('input'));
    });
    fixture.detectChanges();
  }
  function submit(): void {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();
  }
  it('searches only on submit and clears the interval with empty fields', () => {
    const emit = vi.spyOn(fixture.componentInstance.searchRequested, 'emit');
    dates('2026-10-01', '2026-10-01');
    expect(emit).not.toHaveBeenCalled();
    submit();
    expect(emit).toHaveBeenLastCalledWith({ start: '2026-09-30T22:00:00.000Z', end: '2026-10-01T22:00:00.000Z' });
    dates('', '');
    submit();
    expect(emit).toHaveBeenLastCalledWith(undefined);
  });
  it.each([['2026-10-01', ''], ['', '2026-10-01'], ['2026-10-02', '2026-10-01']])(
    'shows an accessible error for an invalid interval', (from, to) => {
      const emit = vi.spyOn(fixture.componentInstance.searchRequested, 'emit');
      dates(from, to);
      submit();
      expect(emit).not.toHaveBeenCalled();
      expect(fixture.nativeElement.querySelector('[role="alert"]')).not.toBeNull();
      expect(fixture.nativeElement.querySelector('input').getAttribute('aria-invalid')).toBe('true');
    },
  );
  function autocomplete(): AutocompleteTextboxComponent<EmployeeResponse> {
    return fixture.debugElement.query(By.directive(AutocompleteTextboxComponent)).componentInstance;
  }
  it('searches employees after typing and displays their name and number', async () => {
    vi.useFakeTimers();
    try {
      autocomplete().textControl.setValue('Abel');
      expect(employees.searchEmployees).not.toHaveBeenCalled();
      await vi.advanceTimersByTimeAsync(350);
      fixture.detectChanges();
      expect(employees.searchEmployees).toHaveBeenCalledWith('Abel', undefined, undefined, 0, 20);
      expect(autocomplete().items()).toEqual([employee]);
      expect(autocomplete().displayWith()(employee)).toBe('Abel Ferrer (EMP-0042)');
    } finally {
      vi.useRealTimers();
    }
  });
  it('includes the selected employee with or without dates and removes it when cleared', () => {
    const emit = vi.spyOn(fixture.componentInstance.searchRequested, 'emit');
    autocomplete().onSelect(employee);
    expect(emit).not.toHaveBeenCalled();
    submit();
    expect(emit).toHaveBeenLastCalledWith({ employeeNumber: 'EMP-0042' });
    dates('2026-10-01', '2026-10-01');
    submit();
    expect(emit).toHaveBeenLastCalledWith({
      employeeNumber: 'EMP-0042', start: '2026-09-30T22:00:00.000Z', end: '2026-10-01T22:00:00.000Z',
    });
    autocomplete().clearSelection();
    submit();
    expect(emit).toHaveBeenLastCalledWith({ start: '2026-09-30T22:00:00.000Z', end: '2026-10-01T22:00:00.000Z' });
  });
  it('does not filter by unselected text or a previous selection after editing', () => {
    const emit = vi.spyOn(fixture.componentInstance.searchRequested, 'emit');
    autocomplete().onSelect(employee);
    autocomplete().textControl.setValue('Other');
    submit();
    expect(emit).toHaveBeenLastCalledWith(undefined);
  });
  it('falls back to the browser timezone', () => {
    preferences.set(null);
    const emit = vi.spyOn(fixture.componentInstance.searchRequested, 'emit');
    dates('2026-10-01', '2026-10-01');
    submit();
    expect(emit).toHaveBeenCalledWith({ start: new Date(2026, 9, 1).toISOString(), end: new Date(2026, 9, 2).toISOString() });
  });
});
