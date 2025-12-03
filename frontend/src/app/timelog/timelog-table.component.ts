import { CommonModule, DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { DateTime } from 'luxon';
import { TimeLogResponse, TimeLogService } from './time-log.service';

interface TimeLogView {
	entryTime: Date;
	exitTime?: Date;
	duration?: string;
}

@Component({
	selector: 'app-timelog-table',
	standalone: true,
	imports: [CommonModule, TranslatePipe, DatePipe],
	templateUrl: './timelog-table.component.html',
	// Si tu Angular no soporta styleUrl, cambia a:
	// styleUrls: ['./timelog-table.component.css']
	styleUrl: './timelog-table.component.css'
})
export class TimelogTableComponent implements OnInit {
	protected readonly employeeEmail = 'aferrer@nivel36.es';
	protected timelogs: TimeLogView[] = [];
	protected isLoading = false;
	protected error?: string;

	private readonly destroyRef = inject(DestroyRef);

	constructor(private readonly timeLogService: TimeLogService) { }

	ngOnInit(): void {
		this.loadTimeLogs();
	}

	private loadTimeLogs(): void {
		this.isLoading = true;
		this.error = undefined;

		this.timeLogService
			.searchByEmployee(this.employeeEmail)
			.pipe(takeUntilDestroyed(this.destroyRef))
			.subscribe({
				next: (response) => {
					this.timelogs = response.map((timelog) => this.mapToView(timelog));
				},
				error: () => {
					this.error = 'Unable to load time logs at this time.';
					this.isLoading = false; // importante para que deje de mostrar el loading en error
				},
				complete: () => {
					this.isLoading = false;
				}
			});
	}

	private mapToView(timeLog: TimeLogResponse): TimeLogView {
		const zone = timeLog.worksiteZoneId;
		const entryTime = this.convertInstant(timeLog.entryTime, zone);

		const exitTimeInstant = this.extractExitTime(timeLog.exitTime);
		const exitTime = exitTimeInstant ? this.convertInstant(exitTimeInstant, zone) : undefined;

		return {
			entryTime,
			exitTime,
			duration: exitTime ? this.calculateDuration(entryTime, exitTime) : undefined
		};
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

	private convertInstant(instant: string, zone: string): Date {
		// Interpretamos el instant como UTC y lo convertimos a la zona del worksite
		return DateTime.fromISO(instant, { zone: 'utc' })
			.setZone(zone)
			.toJSDate();
	}

	private calculateDuration(entryTime: Date, exitTime: Date): string {
		const milliseconds = Math.max(exitTime.getTime() - entryTime.getTime(), 0);
		const totalMinutes = Math.floor(milliseconds / 60000);
		const hours = Math.floor(totalMinutes / 60);
		const minutes = totalMinutes % 60;

		return `${hours}h ${minutes.toString().padStart(2, '0')}m`;
	}
}
