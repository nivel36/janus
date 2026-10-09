/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { signal, type Type } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { CurrentUserFacade } from '../core/user/services/current-user.facade';
import { ScheduleTableComponent } from './schedules/components/schedule-table/schedule-table.component';
import { ScheduleApiService } from './schedules/services/schedule-api.service';
import { TimelogTableComponent } from './timelogs/components/timelog-table/timelog-table.component';
import { TimeLogPage, TimeLogService } from './timelogs/services/timelog-api.service';
import { WorksiteTableComponent } from './worksites/components/worksite-table/worksite-table.component';
import { WorksiteApiService } from './worksites/services/worksite-api.service';

type TableComponent = TimelogTableComponent | WorksiteTableComponent | ScheduleTableComponent;

const tables = [
  { component: TimelogTableComponent, service: TimeLogService, queryArgs: [] },
  { component: WorksiteTableComponent, service: WorksiteApiService, queryArgs: [''] },
  { component: ScheduleTableComponent, service: ScheduleApiService, queryArgs: [''] },
];

describe.each(tables)('$component.name pagination', ({ component, service, queryArgs }) => {
  let fixture: ComponentFixture<TableComponent>;
  let response: Subject<TimeLogPage>;
  let search: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    search = vi.fn(() => {
      response = new Subject<TimeLogPage>();
      return response.asObservable();
    });
    await TestBed.configureTestingModule({
      imports: [component],
      providers: [
        provideRouter([]),
        provideTranslateService(),
        { provide: service, useValue: { search } },
        { provide: CurrentUserFacade, useValue: { preferences: signal(undefined) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(component as Type<TableComponent>);
    if ('pageChange' in fixture.componentInstance) {
      fixture.componentInstance.pageChange.subscribe((page) => {
        fixture.componentRef.setInput('page', page);
      });
    }
  });

  it.each([undefined, 10])('uses pageSize %s for requests and navigation', async (pageSize) => {
    if (pageSize !== undefined) fixture.componentRef.setInput('pageSize', pageSize);
    const size = pageSize ?? 5;
    await settle();
    expect(search).toHaveBeenLastCalledWith(0, size, ...queryArgs);

    await respond(21, 0, size);
    expect(range()).toBe(`1-${size} paginator.of 21`);

    nextPage();
    await settle();
    expect(search).toHaveBeenLastCalledWith(1, size, ...queryArgs);
    await respond(21, 1, size);
    expect(range()).toBe(`${size + 1}-${size * 2} paginator.of 21`);
  });

  it('reloads the current page on size changes and clamps after the response', async () => {
    await settle();
    await respond(21, 0, 5);
    nextPage();
    await settle();
    await respond(21, 1, 5);
    nextPage();
    await settle();
    await respond(21, 2, 5);

    fixture.componentRef.setInput('pageSize', 10);
    await settle();
    expect(search).toHaveBeenLastCalledWith(2, 10, ...queryArgs);
    await respond(11, 2, 10);
    expect(search).toHaveBeenLastCalledWith(1, 10, ...queryArgs);
    await respond(11, 1, 10);
    expect(range()).toBe('11-11 paginator.of 11');
  });

  it.each([5, 10])('preserves size %s when refreshing results', async (size) => {
    fixture.componentRef.setInput('pageSize', size);
    await settle();
    await respond(21, 0, size);
    search.mockClear();

    fixture.componentRef.setInput('refreshToken', 1);
    await settle();
    expect(search).toHaveBeenCalledExactlyOnceWith(0, size, ...queryArgs);
    await respond(22, 0, size);
    expect(range()).toBe(`1-${size} paginator.of 22`);
  });

  async function settle(): Promise<void> {
    fixture.detectChanges();
    await Promise.resolve();
    TestBed.tick();
    fixture.detectChanges();
  }

  async function respond(totalItems: number, page: number, pageSize: number): Promise<void> {
    response.next({ items: [], totalItems, page, pageSize, totalPages: Math.ceil(totalItems / pageSize) });
    response.complete();
    await settle();
  }

  function range(): string {
    return fixture.nativeElement.querySelector('.paginator__text').textContent.trim();
  }

  function nextPage(): void {
    const buttons = fixture.nativeElement.querySelectorAll('app-paginator button');
    (buttons[1] as HTMLButtonElement).click();
  }
});
