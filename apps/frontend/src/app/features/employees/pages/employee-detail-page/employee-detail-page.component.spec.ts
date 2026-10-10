/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter, Router, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { Subject, of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { EmployeeResponse } from '../../../../client/model/employeeResponse';
import { AuthService } from '../../../../core/auth/auth.service';
import { CurrentUserFacade } from '../../../../core/user/services/current-user.facade';
import { EmployeeApiService } from '../../services/employee-api.service';
import { EmployeeDetailPageComponent } from './employee-detail-page.component';
import { WorksiteApiService, WorksitePage } from '../../../worksites/services/worksite-api.service';
import { WorksiteTableComponent } from '../../../worksites/components/worksite-table/worksite-table.component';
import { PaginatorComponent } from '../../../../shared/ui/paginator/paginator.component';
import { TimeLogPage, TimeLogService } from '../../../timelogs/services/timelog-api.service';

describe('EmployeeDetailPageComponent', () => {
  let fixture: ComponentFixture<EmployeeDetailPageComponent>;
  let response: Subject<EmployeeResponse>;
  const findByEmployeeNumber = vi.fn();
  const searchWorksites = vi.fn();
  const searchTimelogs = vi.fn();
  const emptyWorksites: WorksitePage = { items: [], totalItems: 0, page: 0, pageSize: 5, totalPages: 0 };
  const emptyTimelogs: TimeLogPage = { items: [], totalItems: 0, page: 0, pageSize: 5, totalPages: 0 };
  const employee: EmployeeResponse = {
    employeeNumber: 'EMP-1',
    name: 'Abel',
    surname: 'Ferrer',
    email: 'abel@example.com',
    schedule: { code: 'DAY', name: 'Day shift' },
  };

  beforeEach(async () => {
    searchWorksites.mockReset().mockReturnValue(of(emptyWorksites));
    searchTimelogs.mockReset().mockReturnValue(of(emptyTimelogs));
    findByEmployeeNumber.mockReset().mockImplementation(() => {
      response = new Subject<EmployeeResponse>();
      return response;
    });
    await TestBed.configureTestingModule({
      imports: [EmployeeDetailPageComponent],
      providers: [
        provideRouter([
          { path: 'employees/:employeeNumber', component: EmployeeDetailPageComponent },
        ], withComponentInputBinding()),
        provideTranslateService(),
        { provide: EmployeeApiService, useValue: { findByEmployeeNumber } },
        { provide: WorksiteApiService, useValue: { search: searchWorksites } },
        { provide: TimeLogService, useValue: { search: searchTimelogs } },
        { provide: CurrentUserFacade, useValue: { currentUser: signal(null), preferences: signal({ locale: 'en-US', defaultTimezone: 'UTC' }) } },
        { provide: AuthService, useValue: { logout: vi.fn() } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(EmployeeDetailPageComponent);
    fixture.componentRef.setInput('employeeNumber', 'EMP-1');
  });

  async function settle() {
    fixture.detectChanges();
    await Promise.resolve();
    TestBed.tick();
    fixture.detectChanges();
  }

  it('shows employee identity and email in the header and provides a link back to the list', async () => {
    await settle();
    expect(findByEmployeeNumber).toHaveBeenCalledWith('EMP-1');
    expect(fixture.nativeElement.textContent).toContain('employee.detailLoading');
    expect(fixture.nativeElement.querySelector('a[href="/employees/EMP-1/edit"]')).toBeNull();
    response.next(employee);
    response.complete();
    await settle();

    expect(fixture.nativeElement.querySelector('h2').textContent).toContain('Abel Ferrer');
    const header = fixture.nativeElement.querySelector('.app-header-detail');
    expect(header.textContent).toContain('EMP-1');
    expect(header.querySelector(':scope > a[href="mailto:abel@example.com"]').textContent.trim()).toBe('abel@example.com');
    expect(fixture.nativeElement.querySelector('app-button a').getAttribute('href')).toBe('/employees');
    const editLink = fixture.nativeElement.querySelector('a[href="/employees/EMP-1/edit"]') as HTMLAnchorElement;
    expect(editLink.textContent).toContain('actions.edit');
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    editLink.click();
    expect(navigate).toHaveBeenCalled();
    expect(navigate.mock.calls[0][0].toString()).toBe('/employees/EMP-1/edit');
  });

  it('shows an error, keeps back navigation available and recovers after retry', async () => {
    await settle();
    response.error(new Error('Employee unavailable'));
    await settle();
    expect(fixture.nativeElement.textContent).toContain('employee.detailLoadError');
    expect(fixture.nativeElement.querySelector('a[href="/employees/EMP-1/edit"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('app-card')).toBeNull();
    expect(fixture.nativeElement.querySelector('app-button a').getAttribute('href')).toBe('/employees');

    fixture.nativeElement.querySelector('app-button button').click();
    await settle();
    expect(findByEmployeeNumber).toHaveBeenCalledTimes(2);
    response.next(employee);
    response.complete();
    await settle();
    expect(fixture.nativeElement.textContent).toContain('Abel Ferrer');
    expect(fixture.nativeElement.textContent).not.toContain('employee.detailLoadError');
  });

  it('cancels stale requests when the route employee number changes', async () => {
    await settle();
    const firstResponse = response;
    fixture.componentRef.setInput('employeeNumber', 'EMP-2');
    await settle();
    expect(firstResponse.observed).toBe(false);
    expect(findByEmployeeNumber).toHaveBeenLastCalledWith('EMP-2');
    firstResponse.next(employee);
    response.next({ ...employee, employeeNumber: 'EMP-2', name: 'Marta' });
    response.complete();
    await settle();
    expect(fixture.nativeElement.textContent).toContain('Marta Ferrer');
    expect(fixture.nativeElement.textContent).not.toContain('EMP-1');
  });

  it('loads the employee number from a direct detail URL', async () => {
    fixture.destroy();
    findByEmployeeNumber.mockReturnValue(of(employee));
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/employees/EMP-1', EmployeeDetailPageComponent);
    await harness.fixture.whenStable();
    expect(findByEmployeeNumber).toHaveBeenCalledWith('EMP-1');
    expect(harness.routeNativeElement!.textContent).toContain('Abel Ferrer');
  });

  it.each([['GLOBAL', true], ['ASSIGNED', false]] as const)('shows %s worksites and recent timelogs scoped to the employee', async (scope, active) => {
    searchWorksites.mockReturnValue(of({ ...emptyWorksites, totalItems: 1, totalPages: 1, items: [{
      code: 'MAD', name: 'Madrid office', scope, active,
    }] }));
    searchTimelogs.mockReturnValue(of({
      ...emptyTimelogs,
      totalItems: 1,
      totalPages: 1,
      items: [{
        employeeNumber: 'EMP-1', worksiteCode: 'MAD', worksiteName: 'Madrid office',
        worksiteZoneId: 'UTC', entryTime: '2026-10-10T08:00:00Z', exitTime: null, workTime: null,
      }],
    }));
    await settle();
    expect(searchWorksites).not.toHaveBeenCalled();
    expect(searchTimelogs).not.toHaveBeenCalled();
    response.next(employee);
    response.complete();
    await settle();
    await settle();
    expect(searchWorksites).toHaveBeenCalledWith(0, 5, '', 'EMP-1');
    expect(searchTimelogs).toHaveBeenCalledWith(0, 5, { employeeNumber: 'EMP-1' });
    const worksiteTable = fixture.debugElement.query(By.directive(WorksiteTableComponent));
    expect(worksiteTable).toBeTruthy();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    worksiteTable.nativeElement.querySelector('tbody tr').click();
    expect(navigate).toHaveBeenCalledWith(['/worksites', 'MAD']);
    expect(worksiteTable.nativeElement.textContent).toContain(active ? 'worksite.status.active' : 'worksite.status.inactive');
    expect(fixture.nativeElement.textContent).toContain(`worksite.scopes.${scope}`);
    expect(fixture.nativeElement.textContent).not.toContain('employee.noWorksites');
    const timelogs = fixture.nativeElement.querySelector('app-timelog-table');
    expect(timelogs.textContent).toContain('employee.sections.latestTimelogs');
    expect(timelogs.querySelector('tbody').textContent).toContain('Madrid office');
    expect(timelogs.querySelector('tbody').textContent).toContain('08:00');

    fixture.componentRef.setInput('employeeNumber', 'EMP-2');
    await settle();
    response.next({ ...employee, employeeNumber: 'EMP-2' });
    response.complete();
    await settle();
    await settle();
    expect(searchWorksites).toHaveBeenLastCalledWith(0, 5, '', 'EMP-2');
    expect(searchTimelogs).toHaveBeenLastCalledWith(0, 5, { employeeNumber: 'EMP-2' });
  });

  it('paginates employee worksites and resets the page when switching employees', async () => {
    searchWorksites.mockReturnValue(of({ ...emptyWorksites, totalItems: 6, totalPages: 2 }));
    await settle();
    response.next(employee);
    response.complete();
    await settle();
    await settle();
    const table = fixture.debugElement.query(By.directive(WorksiteTableComponent));
    const paginator = table.query(By.directive(PaginatorComponent)).componentInstance as PaginatorComponent;
    paginator.pageRequested.emit(2);
    await settle();
    expect(searchWorksites).toHaveBeenLastCalledWith(1, 5, '', 'EMP-1');
    fixture.componentRef.setInput('employeeNumber', 'EMP-2');
    await settle();
    response.next({ ...employee, employeeNumber: 'EMP-2' });
    response.complete();
    await settle();
    await settle();
    expect(searchWorksites).toHaveBeenLastCalledWith(0, 5, '', 'EMP-2');
  });

  it('retries related data errors independently and shows empty states', async () => {
    const assignments = new Subject<WorksitePage>();
    const timelogs = new Subject<TimeLogPage>();
    searchWorksites.mockReturnValueOnce(assignments);
    searchTimelogs.mockReturnValueOnce(timelogs);
    await settle();
    response.next(employee);
    response.complete();
    await settle();
    await settle();
    assignments.error(new Error('Assignment request failed'));
    timelogs.error(new Error('Timelog request failed'));
    await settle();
    expect(fixture.nativeElement.textContent).toContain('worksite.loadError');
    expect(fixture.nativeElement.textContent).toContain('timelog.loadError');
    expect(fixture.nativeElement.textContent).toContain('Abel Ferrer');
    const retryButtons = fixture.nativeElement.querySelectorAll('app-async-state app-button button');
    expect(retryButtons).toHaveLength(2);
    retryButtons.forEach((button: HTMLButtonElement) => button.click());
    await settle();
    await settle();
    expect(searchWorksites).toHaveBeenCalledTimes(2);
    expect(searchTimelogs).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('employee.noWorksites');
    expect(fixture.nativeElement.textContent).toContain('timelog.empty');
    expect(fixture.nativeElement.textContent).not.toContain('worksite.loadError');
    expect(fixture.nativeElement.textContent).not.toContain('timelog.loadError');
  });
});
