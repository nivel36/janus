/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { EmployeeApiService, EmployeePage } from '../../services/employee-api.service';
import { EmployeeTableComponent } from './employee-table.component';

describe('EmployeeTableComponent', () => {
  let fixture: ComponentFixture<EmployeeTableComponent>;
  let response: Subject<EmployeePage>;
  const search = vi.fn();
  const navigate = vi.fn();
  beforeEach(async () => {
    navigate.mockReset();
    search.mockReset().mockImplementation(() => {
      response = new Subject<EmployeePage>();
      return response;
    });
    await TestBed.configureTestingModule({
      imports: [EmployeeTableComponent],
      providers: [
        provideTranslateService(),
        { provide: EmployeeApiService, useValue: { search } },
        { provide: Router, useValue: { navigate } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(EmployeeTableComponent);
  });
  async function settle() {
    fixture.detectChanges();
    await Promise.resolve();
    TestBed.tick();
    fixture.detectChanges();
  }
  function emptyPage(): EmployeePage {
    return { items: [], totalItems: 0, page: 0, pageSize: 5, totalPages: 0 };
  }
  it('shows loading then renders employee fields and opens details by mouse or keyboard', async () => {
    await settle();
    expect(fixture.nativeElement.textContent).toContain('employee.loading');
    response.next({ ...emptyPage(), totalItems: 1, totalPages: 1, items: [{ employeeNumber: 'EMP-1', name: 'Abel', surname: 'Ferrer', email: 'abel@example.com', schedule: { code: 'DAY', name: 'Day shift' } }] });
    response.complete();
    await settle();
    const row = fixture.nativeElement.querySelector('tbody tr');
    expect(Array.from(row.querySelectorAll('td'), (td: unknown) => (td as HTMLElement).textContent?.trim())).toEqual(['EMP-1', 'Abel', 'Ferrer', 'abel@example.com', 'Day shift']);
    expect(row.getAttribute('tabindex')).toBe('0');
    expect(row.getAttribute('aria-label')).toBeTruthy();
    row.click();
    expect(navigate).toHaveBeenLastCalledWith(['/employees', 'EMP-1']);
    row.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));
    expect(navigate).toHaveBeenCalledTimes(2);
    const space = new KeyboardEvent('keydown', { key: ' ', cancelable: true });
    row.dispatchEvent(space);
    expect(space.defaultPrevented).toBe(true);
    expect(navigate).toHaveBeenCalledTimes(3);
    expect(navigate).toHaveBeenLastCalledWith(['/employees', 'EMP-1']);
  });
  it('distinguishes an empty list from an empty search', async () => {
    await settle();
    response.next(emptyPage());
    response.complete();
    await settle();
    expect(fixture.nativeElement.textContent).toContain('employee.empty');
    fixture.componentRef.setInput('query', 'nobody');
    await settle();
    expect(search).toHaveBeenLastCalledWith(0, 5, 'nobody');
    response.next(emptyPage());
    response.complete();
    await settle();
    expect(fixture.nativeElement.textContent).toContain('employee.searchEmpty');
  });
  it('offers retry after errors and recovers', async () => {
    await settle();
    response.error(new Error('Network error'));
    await settle();
    expect(fixture.nativeElement.textContent).toContain('employee.loadError');
    fixture.nativeElement.querySelector('app-button button').click();
    await settle();
    expect(search).toHaveBeenCalledTimes(2);
    response.next(emptyPage());
    response.complete();
    await settle();
    expect(fixture.nativeElement.textContent).toContain('employee.empty');
    expect(fixture.nativeElement.textContent).not.toContain('employee.loadError');
  });
});
