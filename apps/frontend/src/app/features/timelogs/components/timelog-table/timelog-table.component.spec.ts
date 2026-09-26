/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { CurrentUserFacade } from '../../../../core/user/services/current-user.facade';
import { DEFAULT_LIST_PAGE_SIZE } from '../../../../shared/utils/list-query-params.util';
import { TimeLog } from '../../models/timelog';
import { TimeLogPage, TimeLogService } from '../../services/timelog-api.service';
import { TimelogTableComponent } from './timelog-table.component';

interface TimelogTablePageControls {
  currentPage: () => number;
  onPageChange: (page: number) => void;
  timelogs: () => TimeLog[];
}

describe('TimelogTableComponent', () => {
  let fixture: ComponentFixture<TimelogTableComponent>;
  let component: TimelogTableComponent;
  let pages: Subject<TimeLogPage>[];
  let search: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    pages = Array.from({ length: 3 }, () => new Subject<TimeLogPage>());
    search = vi.fn((page: number) => pages[page].asObservable());

    await TestBed.configureTestingModule({
      imports: [TimelogTableComponent],
      providers: [
        provideTranslateService(),
        { provide: TimeLogService, useValue: { search } },
        {
          provide: CurrentUserFacade,
          useValue: {
            preferences: signal({
              locale: 'en-US',
              timeFormat: 'H24',
              defaultTimezone: 'UTC',
            }),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TimelogTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    TestBed.tick();
  });

  it('returns from an empty page to the last valid page when the total shrinks', async () => {
    pages[0].next(pageWith([timelog('2026-09-01T08:00:00Z')], 11, 0));
    pages[0].complete();
    await settleEffects();

    pageControls().onPageChange(3);
    await settleEffects();
    expect(search).toHaveBeenLastCalledWith(2, DEFAULT_LIST_PAGE_SIZE);

    pages[2].next(pageWith([], 6, 2));
    pages[2].complete();
    await settleEffects();

    expect(pageControls().currentPage()).toBe(2);
    expect(search).toHaveBeenLastCalledWith(1, DEFAULT_LIST_PAGE_SIZE);

    const lastValidTimelog = timelog('2026-09-02T08:00:00Z');
    pages[1].next(pageWith([lastValidTimelog], 6, 1));
    pages[1].complete();
    await settleEffects();

    expect(pageControls().currentPage()).toBe(2);
    expect(pageControls().timelogs()).toEqual([lastValidTimelog]);
  });

  function pageControls(): TimelogTablePageControls {
    return component as unknown as TimelogTablePageControls;
  }

  async function settleEffects(): Promise<void> {
    await Promise.resolve();
    TestBed.tick();
  }

  function pageWith(items: TimeLog[], totalItems: number, page: number): TimeLogPage {
    return {
      items,
      totalItems,
      page,
      pageSize: DEFAULT_LIST_PAGE_SIZE,
      totalPages: Math.ceil(totalItems / DEFAULT_LIST_PAGE_SIZE),
    };
  }

  function timelog(entryTime: string): TimeLog {
    return {
      employeeEmail: 'employee@example.com',
      worksiteCode: 'BCN',
      worksiteZoneId: 'Europe/Madrid',
      entryTime,
      exitTime: null,
      workTime: null,
    };
  }
});
