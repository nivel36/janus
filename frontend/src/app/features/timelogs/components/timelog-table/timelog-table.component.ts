import { CommonModule, DatePipe } from '@angular/common';
import { Component, DestroyRef, Input, OnChanges, OnInit, SimpleChanges, inject } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { TimeLogService } from '../../services/timelog-api.service';
import { TimeLog } from '../../models/timelog';

@Component({
	selector: 'app-timelog-table',
	standalone: true,
	imports: [CommonModule, TranslatePipe, DatePipe],
	templateUrl: './timelog-table.component.html',
	styleUrl: './timelog-table.component.css'
})
export class TimelogTableComponent implements OnInit, OnChanges {
	@Input() employeeEmail?: string;
	@Input() refreshToken = 0;
	protected timelogs: TimeLog[] = [];
	protected isLoading = false;
	protected error?: string;

	private readonly destroyRef = inject(DestroyRef);

	constructor(private readonly timeLogService: TimeLogService) { }

	ngOnInit(): void {
		this.loadTimeLogs();
	}

	ngOnChanges(changes: SimpleChanges): void {
		if (changes['employeeEmail'] || changes['refreshToken']) {
			this.loadTimeLogs();
		}
	}

	private loadTimeLogs(): void {
		if (!this.employeeEmail) {
			return;
		}
		this.isLoading = true;
		this.error = undefined;

		this.timeLogService
			.searchByEmployee(this.employeeEmail)
			.pipe(takeUntilDestroyed(this.destroyRef))
			.subscribe({
				next: (response) => {
					this.timelogs = response;
				},
				error: () => {
					this.error = 'Unable to load time logs at this time.';
					this.isLoading = false;
				},
				complete: () => {
					this.isLoading = false;
				}
			});
	}
}
