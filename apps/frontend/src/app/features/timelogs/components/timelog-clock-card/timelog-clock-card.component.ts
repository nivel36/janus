/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { AsyncPipe } from '@angular/common';
import { Component, DestroyRef, inject, input, output } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import {
  Subject,
  catchError,
  combineLatest,
  distinctUntilChanged,
  exhaustMap,
  filter,
  map,
  merge,
  of,
  shareReplay,
  startWith,
  switchMap,
  withLatestFrom,
} from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { ButtonComponent } from '../../../../shared/ui/button/button.component';
import { CardComponent } from '../../../../shared/ui/card/card.component';
import { ClockComponent } from '../../../../shared/ui/clock/clock.component';
import { TimeLog } from '../../models/timelog';
import { TimeLogService } from '../../services/timelog-api.service';

/**
 * Result of a clocking action.
 */
type ClockActionResult =
  | { type: 'success'; timeLog: TimeLog }
  | { type: 'permissionDenied'; feedbackKey: string }
  | { type: 'networkError'; feedbackKey: string };

/**
 * Self-contained card responsible for displaying and executing the clock in / clock out action.
 *
 * Responsibilities:
 * - Retrieve the employee's latest time log.
 * - Determine whether the next action is clock in or clock out.
 * - Execute the corresponding action.
 * - Display loading state and error feedback.
 * - Notify the parent component when the action completes successfully.
 */
@Component({
  selector: 'app-timelog-clock-card',
  standalone: true,
  imports: [AsyncPipe, TranslatePipe, ClockComponent, CardComponent, ButtonComponent],
  templateUrl: './timelog-clock-card.component.html',
  styleUrl: './timelog-clock-card.component.css',
})
export class TimelogClockCardComponent {
  private readonly authService = inject(AuthService);
  private readonly timeLogService = inject(TimeLogService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly defaultWorksiteCode = 'BCN-HQ';

  /**
   * Email of the employee for whom the card is displayed.
   */
  readonly employeeEmail = input.required<string>();

  /**
   * Event emitted when the clocking action completes successfully.
   */
  readonly clockActionDone = output<void>();

  /**
   * Clicks on the clocking button.
   */
  private readonly clockActionClickSubject = new Subject<void>();

  /**
   * Manual trigger to reload the latest time log.
   */
  private readonly reloadLatestTimeLogSubject = new Subject<void>();

  /**
   * Reactive observable of the employee email.
   */
  private readonly employeeEmail$ = toObservable(this.employeeEmail).pipe(
    filter((email): email is string => !!email),
    distinctUntilChanged(),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  /**
   * Indicates whether the user has permission to clock in/out.
   */
  readonly canClockInOut$ = this.authService.permissions$.pipe(
    map((permissions) => permissions.realmRoles.includes('JANUS_USER')),
    distinctUntilChanged(),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  readonly latestTimeLogReloadTrigger$ = merge(of(undefined), this.reloadLatestTimeLogSubject);

  /**
   * Employee's latest known time log.
   *
   * It is reloaded:
   * - when employeeEmail changes
   * - when a clocking action completes successfully
   */
  readonly latestTimeLog$ = this.employeeEmail$.pipe(
    switchMap((employeeEmail) =>
      this.latestTimeLogReloadTrigger$.pipe(
        switchMap(() => this.searchLatestTimeLog(employeeEmail)),
      ),
    ),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  /**
   * Loads the latest timelog for the given employee.
   *
   * @param employeeEmail Employee email used to search timelogs.
   * @returns An observable containing the most recent timelog,
   * or undefined if the request fails or no timelog is found.
   */
  private searchLatestTimeLog(employeeEmail: string) {
    return this.timeLogService.searchByEmployee(employeeEmail, 0, 5).pipe(
      map((timeLogs) => this.getLatestTimeLog(timeLogs)),
      catchError(() => of(undefined)),
    );
  }

  /**
   * i18n key for the card title.
   */
  readonly clockActionTitleKey$ = this.latestTimeLog$.pipe(
    map((timeLog) =>
      this.isOpenTimeLog(timeLog) ? 'timelog.activeWorkday' : 'timelog.workdayNotStarted',
    ),
    distinctUntilChanged(),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  /**
   * i18n key for the button label.
   */
  readonly clockActionLabelKey$ = this.latestTimeLog$.pipe(
    map((timeLog) => (this.isOpenTimeLog(timeLog) ? 'timelog.clockout' : 'timelog.clockin')),
    distinctUntilChanged(),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  /**
   * Main execution flow for the clocking action.
   *
   * exhaustMap is used to ignore repeated clicks while an action is already in progress.
   */
  readonly clockActionResult$ = this.clockActionClickSubject.pipe(
    withLatestFrom(this.employeeEmail$, this.latestTimeLog$, this.canClockInOut$),
    exhaustMap(([, employeeEmail, latestTimeLog, canClockInOut]) => {
      if (!canClockInOut) {
        return of<ClockActionResult>({
          type: 'permissionDenied',
          feedbackKey: 'timelog.clockActionPermissionDenied',
        });
      }

      const worksiteCode = latestTimeLog?.worksiteCode ?? this.defaultWorksiteCode;
      const action$ = this.isOpenTimeLog(latestTimeLog)
        ? this.timeLogService.clockOut(employeeEmail, worksiteCode)
        : this.timeLogService.clockIn(employeeEmail, worksiteCode);

      return action$.pipe(
        map((timeLog) => ({ type: 'success', timeLog }) as ClockActionResult),
        catchError(() =>
          of<ClockActionResult>({
            type: 'networkError',
            feedbackKey: 'timelog.clockActionNetworkError',
          }),
        ),
      );
    }),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  /**
   * Loading state of the button.
   */
  readonly isClockActionLoading$ = merge(
    this.clockActionClickSubject.pipe(map(() => true)),
    this.clockActionResult$.pipe(map(() => false)),
  ).pipe(startWith(false), distinctUntilChanged(), shareReplay({ bufferSize: 1, refCount: true }));

  /**
   * i18n key for the feedback shown to the user.
   *
   * In case of success, the message is cleared.
   */
  readonly clockActionFeedbackKey$ = this.clockActionResult$.pipe(
    map((result) => (result.type === 'success' ? undefined : result.feedbackKey)),
    startWith(undefined),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  constructor() {
    this.clockActionResult$
      .pipe(
        filter((result) => result.type === 'success'),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.reloadLatestTimeLogSubject.next();
        this.clockActionDone.emit();
      });
  }

  /**
   * Handles the button click.
   *
   * The actual logic lives in the clockActionResult$ stream.
   */
  onClockAction(): void {
    this.clockActionClickSubject.next();
  }

  /**
   * Indicates whether there is an open workday,
   * that is, a clock-in record without an exit time.
   *
   * @param timeLog Time log to evaluate.
   * @returns true if the workday is open.
   */
  private isOpenTimeLog(timeLog?: TimeLog): boolean {
    return !!timeLog && !timeLog.exitTime;
  }

  /**
   * Returns the most recent time log from a collection.
   *
   * @param timeLogs List of time logs.
   * @returns The time log with the most recent entryTime, or undefined if the list is empty.
   */
  private getLatestTimeLog(timeLogs: TimeLog[]): TimeLog | undefined {
    return timeLogs.reduce<TimeLog | undefined>((latest, current) => {
      if (!latest) {
        return current;
      }

      return new Date(current.entryTime).getTime() > new Date(latest.entryTime).getTime()
        ? current
        : latest;
    }, undefined);
  }
}
