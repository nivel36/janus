/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { OverlayContainer } from '@angular/cdk/overlay';
import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { By } from '@angular/platform-browser';
import { FormComponent } from '../../../../shared/ui/form/form.component';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { Subject, of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { AuthService } from '../../../../core/auth/auth.service';
import { CurrentUserFacade } from '../../../../core/user/services/current-user.facade';
import { ScheduleApiService } from '../../../schedules/services/schedule-api.service';
import { EmployeeFormComponent } from '../../components/employee-form/employee-form.component';
import { EmployeeApiService } from '../../services/employee-api.service';
import { EmployeeCreatePageComponent } from './employee-create-page.component';

const schedule = { code: 'DAY', name: 'Day shift', entryTolerance: 'PT0S', exitTolerance: 'PT0S' };
const validForm = {
  employeeNumber: 'EMP-1', name: ' Abel ', surname: ' Ferrer ', email: 'abel@example.com', schedule,
};

describe('EmployeeCreatePageComponent', () => {
  let fixture: ComponentFixture<EmployeeCreatePageComponent>;
  const employeeApi = { findByEmployeeNumber: vi.fn(), create: vi.fn() };
  const scheduleApi = { search: vi.fn() };

  beforeEach(async () => {
    employeeApi.findByEmployeeNumber.mockReset().mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 404 })),
    );
    employeeApi.create.mockReset().mockReturnValue(of({}));
    scheduleApi.search.mockReset().mockReturnValue(of({ items: [schedule] }));
    await TestBed.configureTestingModule({
      imports: [EmployeeCreatePageComponent],
      providers: [
        provideRouter([]),
        provideTranslateService(),
        { provide: EmployeeApiService, useValue: employeeApi },
        { provide: ScheduleApiService, useValue: scheduleApi },
        { provide: CurrentUserFacade, useValue: { currentUser: signal(null) } },
        { provide: AuthService, useValue: { logout: vi.fn() } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(EmployeeCreatePageComponent);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
  });

  function employeeForm(): EmployeeFormComponent {
    return fixture.debugElement.query(By.directive(EmployeeFormComponent)).componentInstance;
  }

  it('checks duplicate numbers on blur and blocks saving', async () => {
    employeeApi.findByEmployeeNumber.mockReturnValue(of({ employeeNumber: 'EMP-1' }));
    const input = fixture.nativeElement.querySelector('#employee-number') as HTMLInputElement;
    input.value = 'EMP-1';
    input.dispatchEvent(new Event('input'));
    expect(employeeApi.findByEmployeeNumber).not.toHaveBeenCalled();
    input.dispatchEvent(new Event('blur'));
    await fixture.whenStable();
    expect(employeeApi.findByEmployeeNumber).toHaveBeenCalledWith('EMP-1');
    expect(fixture.nativeElement.textContent).toContain('employee.errors.duplicatedNumber');
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    expect(employeeApi.create).not.toHaveBeenCalled();
  });

  it('validates required fields, identifier format, names and email before saving', () => {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    expect(employeeForm().form.controls.schedule.touched).toBe(true);
    employeeForm().form.patchValue({ employeeNumber: 'BAD NUMBER', name: '   ', surname: '123', email: 'bad-email' });
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    expect(employeeApi.findByEmployeeNumber).not.toHaveBeenCalled();
    for (const control of Object.values(employeeForm().form.controls)) {
      expect(control.invalid).toBe(true);
    }
    expect(employeeApi.create).not.toHaveBeenCalled();
  });

  it('blocks saving while number validation is pending or fails', async () => {
    const lookup = new Subject<never>();
    employeeApi.findByEmployeeNumber.mockReturnValue(lookup);
    employeeForm().form.setValue(validForm);
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    expect(employeeForm().form.pending).toBe(true);
    expect(employeeApi.create).not.toHaveBeenCalled();
    lookup.error(new HttpErrorResponse({ status: 500 }));
    await fixture.whenStable();
    expect(employeeForm().form.controls.employeeNumber.hasError('numberLookupFailed')).toBe(true);
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    expect(employeeApi.create).not.toHaveBeenCalled();
  });

  it('searches schedules, selects one and submits its code with trimmed names', async () => {
    employeeForm().form.setValue({ ...validForm, schedule: null });
    const input = fixture.nativeElement.querySelector('#employee-schedule') as HTMLInputElement;
    input.value = 'Day';
    input.dispatchEvent(new Event('input'));
    await vi.waitFor(() => expect(scheduleApi.search).toHaveBeenCalledWith(0, 50, 'Day'));
    await fixture.whenStable();
    const option = TestBed.inject(OverlayContainer).getContainerElement().querySelector('[role="option"]') as HTMLElement;
    expect(option.textContent).toContain('Day shift (DAY)');
    option.click();
    await fixture.whenStable();
    expect(employeeForm().form.controls.schedule.value).toEqual(schedule);
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    expect(employeeApi.create).toHaveBeenCalledWith({
      employeeNumber: 'EMP-1', name: 'Abel', surname: 'Ferrer', email: 'abel@example.com', scheduleCode: 'DAY',
    });
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/employees']);
  });

  it.each([500, 409])('shows save errors (%s), preserves data and allows retry', async (status) => {
    const response = new Subject<never>();
    employeeApi.create.mockReturnValue(response);
    employeeForm().form.setValue(validForm);
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    expect(employeeApi.create).toHaveBeenCalledTimes(1);
    expect(fixture.debugElement.query(By.directive(FormComponent)).componentInstance.saving()).toBe(true);
    response.error(new HttpErrorResponse({ status }));
    await fixture.whenStable();
    expect(fixture.debugElement.query(By.directive(FormComponent)).componentInstance.saving()).toBe(false);
    expect(employeeForm().form.getRawValue()).toEqual(validForm);
    expect(fixture.nativeElement.textContent).toContain(
      status === 409 ? 'employee.errors.conflict' : 'employee.errors.create',
    );
    expect(TestBed.inject(Router).navigate).not.toHaveBeenCalled();
    employeeApi.create.mockReturnValue(of({}));
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/employees']);
  });

  it('links cancellation back to the employee list', () => {
    expect(fixture.nativeElement.querySelector('a[href="/employees"]')).not.toBeNull();
  });
});
