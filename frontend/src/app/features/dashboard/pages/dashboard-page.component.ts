import { AsyncPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { filter, firstValueFrom } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AuthService } from '../../../core/auth/auth.service';
import { ClockComponent } from '../../../shared/ui/clock/clock.component';
import { CardComponent } from '../../../shared/ui/card/card.component';
import { TimelogTableComponent } from '../../timelogs/components/timelog-table/timelog-table.component';
import { ButtonComponent } from '../../../shared/ui/button/button.component';
import { TimeLogService } from '../../timelogs/services/timelog-api.service';
import { TimeLog } from '../../timelogs/models/timelog';

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
		ButtonComponent
	],
	templateUrl: './dashboard-page.component.html',
	styleUrl: './dashboard-page.component.css'
})
export class DashboardPageComponent implements OnInit {
	private readonly authService = inject(AuthService);
	private readonly timeLogService = inject(TimeLogService);
	private readonly destroyRef = inject(DestroyRef);
	private readonly defaultWorksiteCode = 'BCN-HQ';

	clockActionLabelKey = 'timelog.clockin';
	isClockActionLoading = false;

	readonly isAuthenticated$ = this.authService.isAuthenticated$;
	readonly username$ = this.authService.username$;

	private latestTimeLog?: TimeLog;

	tableRefreshToken = 0;

	employeeEmail!: string;

	ngOnInit(): void {
	  this.username$
	    .pipe(
	      filter((username): username is string => !!username),
	      takeUntilDestroyed(this.destroyRef)
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

		this.isClockActionLoading = true;

		try {
			const email = await firstValueFrom(
				this.username$.pipe(filter((u): u is string => !!u))
			);

			const worksiteCode = this.latestTimeLog?.worksiteCode ?? this.defaultWorksiteCode;

			const action$ = this.shouldClockOut()
				? this.timeLogService.clockOut(email, worksiteCode)
				: this.timeLogService.clockIn(email, worksiteCode);

			action$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
				next: (timeLog) => {
					this.latestTimeLog = timeLog;
					this.clockActionLabelKey = this.getClockActionLabelKey(timeLog);
					this.tableRefreshToken += 1;
				},
				error: () => {
					this.isClockActionLoading = false;
				},
				complete: () => {
					this.isClockActionLoading = false;
				}
			});
		} catch {
			this.isClockActionLoading = false;
		}
	}

	private refreshLatestTimeLog(username: string): void {
		this.timeLogService
			.searchByEmployee(username, 1, 5)
			.pipe(takeUntilDestroyed(this.destroyRef))
			.subscribe({
				next: (response) => {
					this.latestTimeLog = this.getLatestTimeLog(response);
					this.clockActionLabelKey = this.getClockActionLabelKey(this.latestTimeLog);
				},
				error: () => {
					this.latestTimeLog = undefined;
					this.clockActionLabelKey = 'timelog.clockin';
				}
			});
	}

	private shouldClockOut(): boolean {
		return !!this.latestTimeLog && !this.extractExitTime(this.latestTimeLog.exitTime);
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
