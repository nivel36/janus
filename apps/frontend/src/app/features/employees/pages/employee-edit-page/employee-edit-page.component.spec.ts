/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { OverlayContainer } from '@angular/cdk/overlay';
import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Router, provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { Subject, of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { EmployeeResponse } from '../../../../client/model/employeeResponse';
import { AuthService } from '../../../../core/auth/auth.service';
import { CurrentUserFacade } from '../../../../core/user/services/current-user.facade';
import { FormComponent } from '../../../../shared/ui/form/form.component';
import { ScheduleApiService } from '../../../schedules/services/schedule-api.service';
import { EmployeeFormComponent } from '../../components/employee-form/employee-form.component';
import { EmployeeApiService } from '../../services/employee-api.service';
import { EmployeeEditPageComponent } from './employee-edit-page.component';

const employee: EmployeeResponse = {
  employeeNumber: 'EMP-1',
  name: 'Abel',
  surname: 'Ferrer',
  email: 'abel@example.com',
  schedule: { code: 'DAY', name: 'Day shift' },
};
const nightSchedule = {
  code: 'NIGHT', name: 'Night shift', entryTolerance: 'PT0S', exitTolerance: 'PT0S',
};

describe('EmployeeEditPageComponent', () => {
  let fixture: ComponentFixture<EmployeeEditPageComponent>;
  const employeeApi = { findByEmployeeNumber: vi.fn(), update: vi.fn() };
  const scheduleApi = { search: vi.fn() };

  beforeEach(async () => {
    employeeApi.findByEmployeeNumber.mockReset().mockReturnValue(of(employee));
    employeeApi.update.mockReset().mockReturnValue(of(employee));
    scheduleApi.search.mockReset().mockReturnValue(of({ items: [nightSchedule] }));
    await TestBed.configureTestingModule({
      imports: [EmployeeEditPageComponent],
      providers: [
        provideRouter([
          { path: 'employees/:employeeNumber/edit', component: EmployeeEditPageComponent },
        ], withComponentInputBinding()),
        provideTranslateService(),
        { provide: EmployeeApiService, useValue: employeeApi },
        { provide: ScheduleApiService, useValue: scheduleApi },
        { provide: CurrentUserFacade, useValue: { currentUser: signal(null) } },
        { provide: AuthService, useValue: { logout: vi.fn() } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(EmployeeEditPageComponent);
    fixture.componentRef.setInput('employeeNumber', 'EMP-1');
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    await settle();
  });

  function employeeForm(): EmployeeFormComponent {
    return fixture.debugElement.query(By.directive(EmployeeFormComponent)).componentInstance;
  }

  async function settle() {
    fixture.detectChanges();
    await Promise.resolve();
    TestBed.tick();
    fixture.detectChanges();
  }

  function submit() {
    fixture.nativeElement.querySelector('form').dispatchEvent(
      new Event('submit', { bubbles: true, cancelable: true }),
    );
  }

  it('prefills the employee with an immutable number and the current schedule', () => {
    expect(employeeApi.findByEmployeeNumber).toHaveBeenCalledWith('EMP-1');
    expect(employeeApi.findByEmployeeNumber).toHaveBeenCalledTimes(1);
    expect(employeeForm().form.getRawValue()).toEqual(employee);
    expect(fixture.nativeElement.querySelector('#employee-number').disabled).toBe(true);
    expect(fixture.nativeElement.querySelector('#employee-schedule').value).toBe('Day shift (DAY)');
    expect(scheduleApi.search).not.toHaveBeenCalled();
  });

  it('loads the employee number from a direct edit URL', async () => {
    fixture.destroy();
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/employees/EMP-1/edit', EmployeeEditPageComponent);
    await harness.fixture.whenStable();
    expect(employeeApi.findByEmployeeNumber).toHaveBeenCalledWith('EMP-1');
    expect(harness.routeNativeElement!.querySelector<HTMLInputElement>('#employee-name')!.value).toBe('Abel');
  });

  it('blocks invalid values and exposes accessible field errors', async () => {
    employeeForm().form.patchValue({ name: ' ', surname: '123', email: 'invalid', schedule: null });
    submit();
    await settle();
    expect(employeeApi.update).not.toHaveBeenCalled();
    for (const field of ['name', 'surname', 'email', 'schedule'] as const) {
      expect(employeeForm().form.controls[field].invalid).toBe(true);
      expect(employeeForm().form.controls[field].touched).toBe(true);
      const input = fixture.nativeElement.querySelector(`#employee-${field}`);
      expect(input.getAttribute('aria-invalid')).toBe('true');
      expect(input.getAttribute('aria-describedby')).toBe(`employee-${field}-error`);
    }
    employeeForm().form.patchValue({ name: 'A'.repeat(256), surname: 'B'.repeat(256), email: `${'a'.repeat(250)}@example.com` });
    submit();
    expect(employeeForm().form.controls.name.hasError('maxlength')).toBe(true);
    expect(employeeForm().form.controls.surname.hasError('maxlength')).toBe(true);
    expect(employeeForm().form.controls.email.hasError('maxlength')).toBe(true);
    expect(employeeApi.update).not.toHaveBeenCalled();
  });

  it('searches and selects a new schedule and saves trimmed names using the original number', async () => {
    employeeForm().form.patchValue({ employeeNumber: 'TAMPERED', name: ' Marta ', surname: ' Ferrer ' });
    const input = fixture.nativeElement.querySelector('#employee-schedule') as HTMLInputElement;
    input.value = 'Night';
    input.dispatchEvent(new Event('input'));
    expect(employeeForm().form.controls.schedule.value).toBeNull();
    await vi.waitFor(() => expect(scheduleApi.search).toHaveBeenCalledWith(0, 50, 'Night'));
    await settle();
    const option = TestBed.inject(OverlayContainer).getContainerElement().querySelector('[role="option"]') as HTMLElement;
    expect(option.textContent).toContain('Night shift (NIGHT)');
    option.click();
    await settle();
    submit();
    expect(employeeApi.update).toHaveBeenCalledWith('EMP-1', {
      name: 'Marta', surname: 'Ferrer', email: 'abel@example.com', scheduleCode: 'NIGHT',
    });
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/employees', 'EMP-1']);
  });

  it('cancels back to the employee detail without saving', () => {
    employeeForm().form.patchValue({ name: 'Unsaved' });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    fixture.nativeElement.querySelector('a[href="/employees/EMP-1"]').click();
    expect(navigate.mock.calls[0][0].toString()).toBe('/employees/EMP-1');
    expect(employeeApi.update).not.toHaveBeenCalled();
  });

  it.each([404, 500])('shows load errors (%s), supports retry and keeps cancellation available', async (status) => {
    const response = new Subject<EmployeeResponse>();
    employeeApi.findByEmployeeNumber.mockReturnValue(response);
    fixture.componentRef.setInput('employeeNumber', 'EMP-2');
    await settle();
    expect(fixture.nativeElement.textContent).toContain('employee.detailLoading');
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
    response.error(new HttpErrorResponse({ status }));
    await settle();
    expect(fixture.nativeElement.textContent).toContain('employee.detailLoadError');
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
    expect(fixture.nativeElement.querySelector('a[href="/employees/EMP-2"]')).not.toBeNull();
    if (status === 404) {
      expect(fixture.nativeElement.querySelector('app-async-state button')).toBeNull();
      return;
    }
    employeeApi.findByEmployeeNumber.mockReturnValue(of({ ...employee, employeeNumber: 'EMP-2' }));
    fixture.nativeElement.querySelector('app-async-state button').click();
    await settle();
    expect(fixture.nativeElement.querySelector('form')).not.toBeNull();
    expect(employeeForm().form.getRawValue().employeeNumber).toBe('EMP-2');
    expect(employeeApi.update).not.toHaveBeenCalled();
  });

  it.each([409, 500])('preserves input after a save error (%s), blocks duplicates and allows retry', async (status) => {
    const response = new Subject<EmployeeResponse>();
    employeeApi.update.mockReturnValue(response);
    employeeForm().form.patchValue({ name: 'Marta' });
    submit();
    submit();
    await settle();
    expect(employeeApi.update).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(true);
    response.error(new HttpErrorResponse({ status }));
    await settle();
    expect(fixture.nativeElement.textContent).toContain(
      status === 409 ? 'employee.errors.conflict' : 'employee.errors.update',
    );
    expect(employeeForm().form.controls.name.value).toBe('Marta');
    expect(TestBed.inject(Router).navigate).not.toHaveBeenCalled();
    employeeApi.update.mockReturnValue(of({ ...employee, name: 'Marta' }));
    submit();
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/employees', 'EMP-1']);
  });

  it('cancels pending saves and ignores stale responses when the employee changes', async () => {
    const saveResponse = new Subject<EmployeeResponse>();
    employeeApi.update.mockReturnValue(saveResponse);
    submit();
    expect(saveResponse.observed).toBe(true);
    const loadResponse = new Subject<EmployeeResponse>();
    employeeApi.findByEmployeeNumber.mockReturnValue(loadResponse);
    fixture.componentRef.setInput('employeeNumber', 'EMP-2');
    await settle();
    expect(saveResponse.observed).toBe(false);
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
    saveResponse.next(employee);
    expect(TestBed.inject(Router).navigate).not.toHaveBeenCalled();
    loadResponse.next({ ...employee, employeeNumber: 'EMP-2', name: 'Marta' });
    loadResponse.complete();
    await settle();
    expect(employeeForm().form.getRawValue().employeeNumber).toBe('EMP-2');
    expect(employeeForm().form.controls.name.value).toBe('Marta');
    expect(fixture.debugElement.query(By.directive(FormComponent)).componentInstance.saving()).toBe(false);
  });

  it('cancels stale loads when the employee changes again', async () => {
    const firstLoad = new Subject<EmployeeResponse>();
    employeeApi.findByEmployeeNumber.mockReturnValue(firstLoad);
    fixture.componentRef.setInput('employeeNumber', 'EMP-2');
    await settle();
    employeeApi.findByEmployeeNumber.mockReturnValue(of({ ...employee, employeeNumber: 'EMP-3', name: 'Marta' }));
    fixture.componentRef.setInput('employeeNumber', 'EMP-3');
    await settle();
    expect(firstLoad.observed).toBe(false);
    firstLoad.next({ ...employee, employeeNumber: 'EMP-2' });
    await settle();
    expect(employeeForm().form.getRawValue().employeeNumber).toBe('EMP-3');
    expect(employeeForm().form.controls.name.value).toBe('Marta');
  });
});
