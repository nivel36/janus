/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { AsyncPipe } from '@angular/common';
import { Component, DestroyRef, inject, input, output } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import {
  Observable,
  Subject,
  catchError,
  combineLatest,
  concat,
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
 * Requested clock action mode.
 */
type ClockActionMode = 'auto' | 'force-opposite';

/**
 * Result of a clocking action.
 */
type ClockActionResult =
  | { type: 'success'; employeeEmail: string; timeLog: TimeLog }
  | { type: 'permissionDenied'; feedbackKey: string }
  | { type: 'networkError'; feedbackKey: string };

/**
 * Internal state of the clocking action execution.
 */
type ClockActionState =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'result'; result: ClockActionResult };

/**
 * View model consumed by the template.
 */
type TimelogClockCardViewModel = {
  hasClockInOutPermission: boolean;
  isClockActionLoading: boolean;
  clockActionFeedbackKey?: string;
  clockActionTitleKey: string;
  clockActionLabelKey: string;
  oppositeClockActionLabelKey: string;
};

/**
 * Resolved data required to execute a clock action.
 */
type ResolvedClockAction = {
  shouldClockOut: boolean;
  worksiteCode: string;
};

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
   * Requests to execute a clocking action.
   */
  private readonly clockActionRequests$ = new Subject<ClockActionMode>();

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
  readonly hasClockInOutPermission$ = this.authService.permissions$.pipe(
    map((permissions) => permissions.realmRoles.includes('JANUS_EMPLOYEE')),
    distinctUntilChanged(),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  /**
   * Employee's latest known time log.
   *
   * It is updated:
   * - when employeeEmail changes
   * - when a clocking action completes successfully, reusing the API response directly
   */
  readonly latestTimeLog$: Observable<TimeLog | undefined> = this.employeeEmail$.pipe(
    switchMap((employeeEmail) =>
      merge(
        this.searchLatestTimeLog(employeeEmail),
        this.successfulClockActionResult$.pipe(
          filter((result) => result.employeeEmail === employeeEmail),
          map((result) => result.timeLog),
        ),
      ).pipe(startWith(undefined)),
    ),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  /**
   * Main execution state flow for the clocking action.
   *
   * exhaustMap is used to ignore repeated clicks while an action is already in progress.
   */
  readonly clockActionState$: Observable<ClockActionState> = this.clockActionRequests$.pipe(
    withLatestFrom(this.employeeEmail$, this.latestTimeLog$, this.hasClockInOutPermission$),
    exhaustMap(([mode, employeeEmail, latestTimeLog, canClockInOut]) => {
      if (!canClockInOut) {
        return of<ClockActionState>({
          status: 'result',
          result: {
            type: 'permissionDenied',
            feedbackKey: 'timelog.clockActionPermissionDenied',
          },
        });
      }

      const { shouldClockOut, worksiteCode } = this.resolveClockAction(mode, latestTimeLog);

      const action$ = shouldClockOut
        ? this.timeLogService.clockOut(employeeEmail, worksiteCode)
        : this.timeLogService.clockIn(employeeEmail, worksiteCode);

      return concat(
        of<ClockActionState>({ status: 'loading' }),
        action$.pipe(
          map(
            (timeLog): ClockActionState => ({
              status: 'result',
              result: { type: 'success', employeeEmail, timeLog },
            }),
          ),
          catchError(() =>
            of<ClockActionState>({
              status: 'result',
              result: {
                type: 'networkError',
                feedbackKey: 'timelog.clockActionNetworkError',
              },
            }),
          ),
        ),
      );
    }),
    startWith<ClockActionState>({ status: 'idle' }),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  /**
   * Result emitted when a clocking action finishes.
   */
  readonly clockActionResult$: Observable<ClockActionResult> = this.clockActionState$.pipe(
    filter(
      (state): state is Extract<ClockActionState, { status: 'result' }> =>
        state.status === 'result',
    ),
    map((state) => state.result),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  /**
   * Time log returned by a successful clocking action.
   */
  readonly successfulClockActionResult$: Observable<
    Extract<ClockActionResult, { type: 'success' }>
  > = this.clockActionResult$.pipe(
    filter(
      (result): result is Extract<ClockActionResult, { type: 'success' }> =>
        result.type === 'success',
    ),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  /**
   * Loading state of the button.
   */
  readonly isClockActionLoading$: Observable<boolean> = this.clockActionState$.pipe(
    map((state) => state.status === 'loading'),
    distinctUntilChanged(),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  /**
   * i18n key for the feedback shown to the user.
   *
   * In case of success, the message is cleared.
   */
  readonly clockActionFeedbackKey$: Observable<string | undefined> = this.clockActionResult$.pipe(
    map((result) => (result.type === 'success' ? undefined : result.feedbackKey)),
    startWith(undefined),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  /**
   * View model consumed by the template.
   */
  readonly vm$: Observable<TimelogClockCardViewModel> = combineLatest([
    this.latestTimeLog$,
    this.hasClockInOutPermission$,
    this.isClockActionLoading$,
    this.clockActionFeedbackKey$,
  ]).pipe(
    map(
      ([latestTimeLog, hasClockInOutPermission, isClockActionLoading, clockActionFeedbackKey]) => {
        const hasOpenTimeLog = this.isOpenTimeLog(latestTimeLog);

        return {
          hasClockInOutPermission,
          isClockActionLoading,
          clockActionFeedbackKey,
          clockActionTitleKey: hasOpenTimeLog
            ? 'timelog.activeWorkday'
            : 'timelog.workdayNotStarted',
          clockActionLabelKey: hasOpenTimeLog ? 'timelog.clockout' : 'timelog.clockin',
          oppositeClockActionLabelKey: hasOpenTimeLog ? 'timelog.clockin' : 'timelog.clockout',
        };
      },
    ),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  constructor() {
    this.successfulClockActionResult$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.clockActionDone.emit();
    });
  }

  /**
   * Handles the main clock action request.
   *
   * The actual logic lives in the clockActionState$ stream.
   */
  onClockAction(): void {
    this.clockActionRequests$.next('auto');
  }

  /**
   * Executes the opposite clock action explicitly,
   * bypassing the automatic decision.
   */
  onOppositeClockAction(): void {
    this.clockActionRequests$.next('force-opposite');
  }

  /**
   * Loads the latest timelog for the given employee.
   *
   * @param employeeEmail Employee email used to search timelogs.
   * @returns An observable containing the most recent timelog,
   * or undefined if the request fails or no timelog is found.
   */
  private searchLatestTimeLog(employeeEmail: string): Observable<TimeLog | undefined> {
    return this.timeLogService
      .searchLatestByEmployee(employeeEmail)
      .pipe(catchError(() => of(undefined)));
  }

  /**
   * Resolves the effective action to execute from the current mode and timelog state.
   *
   * @param mode Requested action mode.
   * @param latestTimeLog Latest known timelog for the employee.
   * @returns The action to execute and the worksite code to use.
   */
  private resolveClockAction(
    mode: ClockActionMode,
    latestTimeLog: TimeLog | undefined,
  ): ResolvedClockAction {
    const hasOpenTimeLog = this.isOpenTimeLog(latestTimeLog);

    return {
      shouldClockOut: mode === 'force-opposite' ? !hasOpenTimeLog : hasOpenTimeLog,
      worksiteCode: latestTimeLog?.worksiteCode ?? this.defaultWorksiteCode,
    };
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
}
