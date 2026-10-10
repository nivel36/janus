/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Location } from '@angular/common';
import { provideLocationMocks } from '@angular/common/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NavigationEnd, provideRouter, Router, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { filter, firstValueFrom, of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../../../core/auth/auth.service';
import { CurrentUserFacade } from '../../../../core/user/services/current-user.facade';
import { SearchBarComponent } from '../../../../shared/ui/search-bar/search-bar.component';
import { EmployeeTableComponent } from '../../components/employee-table/employee-table.component';
import { EmployeeApiService } from '../../services/employee-api.service';
import { EmployeesPageComponent } from './employees-page.component';

describe('EmployeesPageComponent', () => {
  const search = vi.fn();
  let harness: RouterTestingHarness;
  let router: Router;
  beforeEach(async () => {
    search.mockReset().mockImplementation((page: number, pageSize: number) => of({
      items: [], totalItems: 21, page, pageSize, totalPages: Math.ceil(21 / pageSize),
    }));
    await TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'employees', component: EmployeesPageComponent }], withComponentInputBinding()),
        provideTranslateService(),
        provideLocationMocks(),
        { provide: EmployeeApiService, useValue: { search } },
        { provide: CurrentUserFacade, useValue: { currentUser: signal(null) } },
        { provide: AuthService, useValue: { logout: vi.fn() } },
      ],
    }).compileComponents();
    router = TestBed.inject(Router);
    router.setUpLocationChangeListener();
    harness = await RouterTestingHarness.create();
  });
  function searchBar(): SearchBarComponent {
    return harness.routeDebugElement!.query(By.directive(SearchBarComponent)).componentInstance;
  }
  async function navigate(action: () => void) {
    const finished = firstValueFrom(router.events.pipe(filter((event) => event instanceof NavigationEnd)));
    action();
    await finished;
    harness.detectChanges();
    await harness.fixture.whenStable();
  }

  it('offers a link to create a new employee', async () => {
    await harness.navigateByUrl('/employees', EmployeesPageComponent);
    const link = harness.routeNativeElement!.querySelector('a[href="/employees/new"]');
    expect(link).not.toBeNull();
    expect(link!.textContent).toContain('actions.new');
  });

  it('restores query and page from the URL and requests the backend page', async () => {
    await harness.navigateByUrl('/employees?query=Abel&page=2', EmployeesPageComponent);
    await harness.fixture.whenStable();
    expect(search).toHaveBeenLastCalledWith(1, 5, 'Abel');
    expect(searchBar().query()).toBe('Abel');
    expect(searchBar().placeholderKey()).toBe('employee.search.placeholder');
    expect(harness.routeNativeElement!.querySelector('input')!.value).toBe('Abel');
  });

  it('resets pages on search, supports back navigation, and removes cleared query parameters', async () => {
    await harness.navigateByUrl('/employees?query=Abel&page=2', EmployeesPageComponent);
    await navigate(() => searchBar().queryChange.emit('Ferrer'));
    expect(router.url).toBe('/employees?query=Ferrer');
    expect(search).toHaveBeenLastCalledWith(0, 5, 'Ferrer');

    await navigate(() => TestBed.inject(Location).back());
    expect(router.url).toBe('/employees?query=Abel&page=2');
    expect(harness.routeNativeElement!.querySelector('input')!.value).toBe('Abel');

    await navigate(() => searchBar().queryChange.emit(''));
    expect(router.url).toBe('/employees');
    expect(search).toHaveBeenLastCalledWith(0, 5, '');
  });

  it('keeps the query when changing pages and normalizes invalid URL pages', async () => {
    await harness.navigateByUrl('/employees?query=Abel&page=invalid', EmployeesPageComponent);
    await harness.fixture.whenStable();
    expect(search).toHaveBeenLastCalledWith(0, 5, 'Abel');
    const table = harness.routeDebugElement!.query(By.directive(EmployeeTableComponent)).componentInstance as EmployeeTableComponent;
    await navigate(() => table.pageChange.emit(3));
    expect(router.url).toBe('/employees?query=Abel&page=3');
    expect(search).toHaveBeenLastCalledWith(2, 5, 'Abel');
  });
});
