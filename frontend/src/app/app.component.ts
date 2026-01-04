import { AsyncPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from './auth/auth.service';
import { ClockComponent } from './shared/ui/clock/clock.component';
import { CardComponent } from './shared/ui/card/card.component';
import { TimelogTableComponent } from './timelog/timelog-table.component';
import { ButtonComponent } from './shared/ui/button/button.component';
import { TimeLogResponse, TimeLogService } from './timelog/time-log.service';
import { filter } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
	selector: 'app-root',
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
	templateUrl: './app.component.html',
	styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
	title = 'frontend';
	private readonly authService = inject(AuthService);
	private readonly timeLogService = inject(TimeLogService);
	private readonly destroyRef = inject(DestroyRef);
	private readonly defaultWorksiteCode = 'BCN-HQ';
	credentials = {
		username: '',
		password: ''
	};
	errorMessage = '';
	isLoading = false;
	clockActionLabelKey = 'timelog.clockin';
	isClockActionLoading = false;
	readonly isAuthenticated$ = this.authService.isAuthenticated$;
	readonly username$ = this.authService.username$;
	private latestTimeLog?: TimeLogResponse;
	private employeeEmail?: string;

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

	onLogin(): void {
		this.errorMessage = '';
		this.isLoading = true;
		this.authService.login(this.credentials.username, this.credentials.password).subscribe({
			next: () => {
				this.isLoading = false;
				this.credentials.password = '';
			},
			error: (error) => {
				this.isLoading = false;
				this.errorMessage = error?.error?.detail ?? 'Unable to sign in. Please try again.';
			}
		});
	}

	onClockAction(): void {
		if (!this.employeeEmail || this.isClockActionLoading) {
			return;
		}

		this.isClockActionLoading = true;
		const worksiteCode = this.latestTimeLog?.worksiteCode ?? this.defaultWorksiteCode;
		const action$ = this.shouldClockOut()
			? this.timeLogService.clockOut(this.employeeEmail, worksiteCode)
			: this.timeLogService.clockIn(this.employeeEmail, worksiteCode);

		action$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
			next: (timeLog) => {
				this.latestTimeLog = timeLog;
				this.clockActionLabelKey = this.getClockActionLabelKey(timeLog);
			},
			error: () => {
				this.isClockActionLoading = false;
			},
			complete: () => {
				this.isClockActionLoading = false;
			}
		});
	}

	private refreshLatestTimeLog(username: string): void {
		this.timeLogService
			.searchByEmployee(username)
			.pipe(takeUntilDestroyed(this.destroyRef))
			.subscribe({
				next: (response) => {
					this.latestTimeLog = this.getLatestTimeLog(response);
					this.clockActionLabelKey = this.getClockActionLabelKey(this.latestTimeLog);
				}
			});
	}

	private shouldClockOut(): boolean {
		return !!this.latestTimeLog && !this.extractExitTime(this.latestTimeLog.exitTime);
	}

	private getClockActionLabelKey(timeLog?: TimeLogResponse): string {
		if (!timeLog) {
			return 'timelog.clockin';
		}
		return this.extractExitTime(timeLog.exitTime) ? 'timelog.clockin' : 'timelog.clockout';
	}

	private getLatestTimeLog(timeLogs: TimeLogResponse[]): TimeLogResponse | undefined {
		return timeLogs.reduce<TimeLogResponse | undefined>((latest, current) => {
			if (!latest) {
				return current;
			}
			return new Date(current.entryTime).getTime() > new Date(latest.entryTime).getTime()
				? current
				: latest;
		}, undefined);
	}

	private extractExitTime(exitTime: TimeLogResponse['exitTime']): string | null {
		if (!exitTime) {
			return null;
		}
		if (typeof exitTime === 'string') {
			return exitTime;
		}
		if ('present' in exitTime) {
			return exitTime.present ? exitTime.value ?? null : null;
		}
		return null;
	}
}
