/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { Subject, of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { CurrentUserFacade } from '../../../../core/user/services/current-user.facade';
import { Worksite } from '../../../worksites/models/worksite';
import { WorksiteApiService } from '../../../worksites/services/worksite-api.service';
import { TimeLog } from '../../models/timelog';
import { TimeLogService } from '../../services/timelog-api.service';
import { TimelogClockCardComponent } from './timelog-clock-card.component';

interface ClockCardControls {
  latestTimeLog: () => TimeLog | undefined;
  isClockActionLoading: () => boolean;
  clockActionFeedbackKey: () => string | undefined;
  clockActionLabelKey: () => string;
  onClockAction: () => void;
  onOppositeClockAction: () => void;
}

describe('TimelogClockCardComponent', () => {
  let fixture: ComponentFixture<TimelogClockCardComponent>;
  let component: TimelogClockCardComponent;
  let latestTimeLogs: Subject<TimeLog | undefined>;
  let clockInResult: Subject<TimeLog>;
  let clockOutResult: Subject<TimeLog>;
  let clockIn: ReturnType<typeof vi.fn>;
  let clockOut: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    latestTimeLogs = new Subject<TimeLog | undefined>();
    clockInResult = new Subject<TimeLog>();
    clockOutResult = new Subject<TimeLog>();
    clockIn = vi.fn(() => clockInResult.asObservable());
    clockOut = vi.fn(() => clockOutResult.asObservable());

    await TestBed.configureTestingModule({
      imports: [TimelogClockCardComponent],
      providers: [
        provideTranslateService(),
        {
          provide: CurrentUserFacade,
          useValue: {
            isEmployee: signal(true),
            preferences: signal({ locale: 'en-GB', timeFormat: 'H24' }),
          },
        },
        {
          provide: TimeLogService,
          useValue: {
            searchLatestByEmployee: vi.fn(() => latestTimeLogs.asObservable()),
            clockIn,
            clockOut,
          },
        },
        {
          provide: WorksiteApiService,
          useValue: {
            searchAssignedToEmployee: vi.fn(() => of([{ code: 'BCN' } as Worksite])),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TimelogClockCardComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('employeeEmail', 'employee@example.com');
    fixture.detectChanges();
    await settleEffects();
  });

  it('derives the action labels from the latest timelog resource', async () => {
    expect(controls().clockActionLabelKey()).toBe('timelog.clockin');

    latestTimeLogs.next(timelog(null));
    await settleEffects();

    expect(controls().latestTimeLog()?.worksiteCode).toBe('BCN');
    expect(controls().clockActionLabelKey()).toBe('timelog.clockout');
  });

  it('prevents concurrent mutations, updates the resource, and emits completion', async () => {
    const done = vi.fn();
    component.clockActionDone.subscribe(done);

    controls().onClockAction();
    controls().onClockAction();

    expect(clockIn).toHaveBeenCalledTimes(1);
    expect(controls().isClockActionLoading()).toBe(true);

    const createdTimeLog = timelog(null);
    clockInResult.next(createdTimeLog);
    clockInResult.complete();
    await settleEffects();

    expect(controls().latestTimeLog()).toEqual(createdTimeLog);
    expect(controls().isClockActionLoading()).toBe(false);
    expect(done).toHaveBeenCalledTimes(1);
  });

  it('exposes a failed mutation as signal presentation state', async () => {
    controls().onOppositeClockAction();
    clockOutResult.error(new Error('network'));
    await settleEffects();

    expect(clockOut).toHaveBeenCalledWith('employee@example.com', 'BCN');
    expect(controls().isClockActionLoading()).toBe(false);
    expect(controls().clockActionFeedbackKey()).toBe('timelog.clockActionNetworkError');
  });

  function controls(): ClockCardControls {
    return component as unknown as ClockCardControls;
  }

  async function settleEffects(): Promise<void> {
    await Promise.resolve();
    TestBed.tick();
  }

  function timelog(exitTime: string | null): TimeLog {
    return {
      employeeEmail: 'employee@example.com',
      worksiteCode: 'BCN',
      worksiteZoneId: 'Europe/Madrid',
      entryTime: '2026-09-26T08:00:00Z',
      exitTime,
      workTime: null,
    };
  }
});
