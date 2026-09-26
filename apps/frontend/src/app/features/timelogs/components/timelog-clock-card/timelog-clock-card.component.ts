/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, DestroyRef, computed, inject, input, output, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faAngleRight } from '@fortawesome/free-solid-svg-icons';
import { TranslatePipe } from '@ngx-translate/core';
import { catchError, finalize, map, of } from 'rxjs';

import { CurrentUserFacade } from '../../../../core/user/services/current-user.facade';
import { ButtonComponent } from '../../../../shared/ui/button/button.component';
import { ClockComponent } from '../../../../shared/ui/clock/clock.component';
import { createUuid } from '../../../../shared/utils/uuid.utils';
import { WorksiteApiService } from '../../../worksites/services/worksite-api.service';
import { TimeLog } from '../../models/timelog';
import { TimeLogService } from '../../services/timelog-api.service';

type ClockActionMode = 'auto' | 'force-opposite';

interface ResolvedClockAction {
  shouldClockOut: boolean;
  worksiteCode: string | undefined;
}

/**
 * Self-contained card responsible for displaying and executing the clock in / clock out action.
 */
@Component({
  selector: 'app-timelog-clock-card',
  standalone: true,
  imports: [TranslatePipe, ClockComponent, ButtonComponent, FontAwesomeModule],
  templateUrl: './timelog-clock-card.component.html',
  styleUrl: './timelog-clock-card.component.css',
})
export class TimelogClockCardComponent {
  private readonly currentUser = inject(CurrentUserFacade);
  private readonly timeLogService = inject(TimeLogService);
  private readonly worksiteApiService = inject(WorksiteApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly faAngleRight = faAngleRight;
  readonly titleElementId = `clock-in-card-${createUuid()}-title`;

  /** Email of the employee for whom the card is displayed. */
  readonly employeeEmail = input.required<string>();

  /** Event emitted when the clocking action completes successfully. */
  readonly clockActionDone = output<void>();

  private readonly latestTimeLogResource = rxResource<TimeLog | undefined, string>({
    params: () => this.employeeEmail(),
    stream: ({ params: employeeEmail }) =>
      this.timeLogService
        .searchLatestByEmployee(employeeEmail)
        .pipe(catchError(() => of(undefined))),
    defaultValue: undefined,
  });

  private readonly assignedWorksiteCodeResource = rxResource<string | undefined, string>({
    params: () => this.employeeEmail(),
    stream: ({ params: employeeEmail }) =>
      this.worksiteApiService.searchAssignedToEmployee(employeeEmail).pipe(
        map((worksites) => (worksites.length === 1 ? worksites[0].code : undefined)),
        catchError(() => of(undefined)),
      ),
    defaultValue: undefined,
  });

  protected readonly latestTimeLog = computed(() => this.latestTimeLogResource.value());
  protected readonly assignedWorksiteCode = computed(() =>
    this.assignedWorksiteCodeResource.value(),
  );
  protected readonly hasClockInOutPermission = computed(() => this.currentUser.isEmployee());
  protected readonly isClockActionLoading = signal(false);
  protected readonly clockActionFeedbackKey = signal<string | undefined>(undefined);

  private readonly hasOpenTimeLog = computed(() => this.isOpenTimeLog(this.latestTimeLog()));

  protected readonly clockActionTitleKey = computed(() =>
    this.hasOpenTimeLog() ? 'timelog.activeWorkday' : 'timelog.workdayNotStarted',
  );
  protected readonly clockActionLabelKey = computed(() =>
    this.hasOpenTimeLog() ? 'timelog.clockout' : 'timelog.clockin',
  );
  protected readonly oppositeClockActionLabelKey = computed(() =>
    this.hasOpenTimeLog() ? 'timelog.clockin' : 'timelog.clockout',
  );
  protected readonly locale = computed(() => this.currentUser.preferences()?.locale);
  protected readonly use12Hour = computed(
    () => this.currentUser.preferences()?.timeFormat === 'H12',
  );

  protected onClockAction(): void {
    this.executeClockAction('auto');
  }

  protected onOppositeClockAction(): void {
    this.executeClockAction('force-opposite');
  }

  /**
   * Executes one clock mutation at a time and updates the query state from its response.
   */
  private executeClockAction(mode: ClockActionMode): void {
    if (this.isClockActionLoading()) {
      return;
    }

    if (!this.hasClockInOutPermission()) {
      this.clockActionFeedbackKey.set('timelog.clockActionPermissionDenied');
      return;
    }

    const employeeEmail = this.employeeEmail();
    const { shouldClockOut, worksiteCode } = this.resolveClockAction(
      mode,
      this.latestTimeLog(),
      this.assignedWorksiteCode(),
    );

    if (!worksiteCode) {
      this.clockActionFeedbackKey.set('timelog.clockActionWorksiteUnavailable');
      return;
    }

    this.isClockActionLoading.set(true);
    this.clockActionFeedbackKey.set(undefined);

    const action$ = shouldClockOut
      ? this.timeLogService.clockOut(employeeEmail, worksiteCode)
      : this.timeLogService.clockIn(employeeEmail, worksiteCode);

    action$
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isClockActionLoading.set(false)),
      )
      .subscribe({
        next: (timeLog) => {
          this.latestTimeLogResource.set(timeLog);
          this.clockActionDone.emit();
        },
        error: () => {
          this.clockActionFeedbackKey.set('timelog.clockActionNetworkError');
        },
      });
  }

  private resolveClockAction(
    mode: ClockActionMode,
    latestTimeLog: TimeLog | undefined,
    assignedWorksiteCode: string | undefined,
  ): ResolvedClockAction {
    const hasOpenTimeLog = this.isOpenTimeLog(latestTimeLog);

    return {
      shouldClockOut: mode === 'force-opposite' ? !hasOpenTimeLog : hasOpenTimeLog,
      worksiteCode: latestTimeLog?.worksiteCode ?? assignedWorksiteCode,
    };
  }

  private isOpenTimeLog(timeLog?: TimeLog): boolean {
    return !!timeLog && !timeLog.exitTime;
  }
}
