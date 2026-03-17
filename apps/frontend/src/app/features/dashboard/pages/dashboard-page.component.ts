/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { AsyncPipe, NgIf } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import {
  BehaviorSubject,
  distinctUntilChanged,
  filter,
  firstValueFrom,
  map,
  shareReplay,
} from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AuthService } from '../../../core/auth/auth.service';
import { ClockComponent } from '../../../shared/ui/clock/clock.component';
import { CardComponent } from '../../../shared/ui/card/card.component';
import { TimelogTableComponent } from '../../timelogs/components/timelog-table/timelog-table.component';
import { ButtonComponent } from '../../../shared/ui/button/button.component';
import { TimeLogService } from '../../timelogs/services/timelog-api.service';
import { TimeLog } from '../../timelogs/models/timelog';
import { KEYCLOAK_CLIENT_ID } from '../../../core/auth/keycloak.constants';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [
    AsyncPipe,
    FormsModule,
    TranslatePipe,
    ClockComponent,
    CardComponent,
    TimelogTableComponent,
    ButtonComponent,
  ],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.css',
})
export class DashboardPageComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly timeLogService = inject(TimeLogService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly defaultWorksiteCode = 'BCN-HQ';

  private readonly latestTimeLogSubject = new BehaviorSubject<TimeLog | undefined>(undefined);
  readonly latestTimeLog$ = this.latestTimeLogSubject.asObservable();

  clockActionFeedbackKey?: string;
  isClockActionLoading = false;
  tableRefreshToken = 0;
  employeeEmail!: string;

  readonly isAuthenticated$ = this.authService.isAuthenticated$;
  readonly username$ = this.authService.username$;

  readonly canClockInOut$ = this.authService.permissions$.pipe(
    map((permissions) => permissions.realmRoles.includes('JANUS_USER')),
  );

  readonly fullName$ = this.authService.claims$.pipe(
    map((c) => `${c?.given_name ?? ''} ${c?.family_name ?? ''}`.trim()),
    distinctUntilChanged(),
  );

  readonly clockActionTitleKey$ = this.latestTimeLog$.pipe(
    map((timeLog) => this.getClockActionTitleKey(timeLog)),
    distinctUntilChanged(),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  readonly clockActionLabelKey$ = this.latestTimeLog$.pipe(
    map((timeLog) => this.getClockActionLabelKey(timeLog)),
    distinctUntilChanged(),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  ngOnInit(): void {
    this.username$
      .pipe(
        filter((username): username is string => !!username),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((username) => {
        this.employeeEmail = username;
        this.refreshLatestTimeLog(username);
      });
  }

  async onClockAction(): Promise<void> {
    if (this.isClockActionLoading) {
      return;
    }

    if (!this.authService.hasRealmRole('JANUS_USER')) {
      this.clockActionFeedbackKey = 'timelog.clockActionPermissionDenied';
      return;
    }

    this.clockActionFeedbackKey = undefined;
    this.isClockActionLoading = true;

    try {
      const email = await firstValueFrom(this.username$.pipe(filter((u): u is string => !!u)));
      const latestTimeLog = this.latestTimeLogSubject.getValue();
      const worksiteCode = latestTimeLog?.worksiteCode ?? this.defaultWorksiteCode;

      const action$ = this.shouldClockOut(latestTimeLog)
        ? this.timeLogService.clockOut(email, worksiteCode)
        : this.timeLogService.clockIn(email, worksiteCode);

      action$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: (timeLog) => {
          this.latestTimeLogSubject.next(timeLog);
          this.clockActionFeedbackKey = undefined;
          this.tableRefreshToken += 1;
        },
        error: () => {
          this.clockActionFeedbackKey = 'timelog.clockActionNetworkError';
          this.isClockActionLoading = false;
        },
        complete: () => {
          this.isClockActionLoading = false;
        },
      });
    } catch {
      this.isClockActionLoading = false;
    }
  }

  private refreshLatestTimeLog(username: string): void {
    this.timeLogService
      .searchByEmployee(username, 0, 5)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.latestTimeLogSubject.next(this.getLatestTimeLog(response));
        },
        error: () => {
          this.latestTimeLogSubject.next(undefined);
        },
      });
  }

  private shouldClockOut(timeLog?: TimeLog): boolean {
    return !!timeLog && !this.extractExitTime(timeLog.exitTime);
  }

  private getClockActionTitleKey(timeLog?: TimeLog): string {
    if (!timeLog) {
      return 'timelog.workdayNotStarted';
    }
    return this.extractExitTime(timeLog.exitTime)
      ? 'timelog.workdayNotStarted'
      : 'timelog.activeWorkday';
  }

  private getClockActionLabelKey(timeLog?: TimeLog): string {
    if (!timeLog) {
      return 'timelog.clockin';
    }
    return this.extractExitTime(timeLog.exitTime) ? 'timelog.clockin' : 'timelog.clockout';
  }

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

  private extractExitTime(exitTime: TimeLog['exitTime']): string | null {
    if (!exitTime) {
      return null;
    }
    if (typeof exitTime === 'string') {
      return exitTime;
    }
    return exitTime ?? null;
  }
}
