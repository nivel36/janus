import { CommonModule, DatePipe } from '@angular/common';
import { Component, DestroyRef, Input, OnChanges, OnInit, SimpleChanges, inject } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject } from 'rxjs';
import { finalize, switchMap } from 'rxjs/operators';

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
	@Input({ required: true }) employeeEmail!: string;
	@Input() refreshToken = 0;
	protected timelogs: TimeLog[] = [];
	protected isLoading = false;
	protected error?: string;

	private readonly destroyRef = inject(DestroyRef);
	private readonly reload$ = new Subject<void>();

	constructor(private readonly timeLogService: TimeLogService) { }

	ngOnInit(): void {
		this.reload$
			.pipe(
				switchMap(() => this.fetchTimeLogs()),
				takeUntilDestroyed(this.destroyRef)
			)
			.subscribe({
				next: (response) => {
					this.timelogs = response;
					this.isLoading = false;
				},
				error: () => {
					this.error = 'Unable to load time logs at this time.';
					this.isLoading = false;
				},
			});
		this.loadTimeLogs();
	}

	ngOnChanges(changes: SimpleChanges): void {
		if (changes['employeeEmail'] || changes['refreshToken']) {
			this.loadTimeLogs();
		}
	}

	private loadTimeLogs(): void {
		this.isLoading = true;
		this.error = undefined;
		this.reload$.next();
	}

	private fetchTimeLogs() {
		return this.timeLogService.searchByEmployee(this.employeeEmail).pipe(
			finalize(() => this.isLoading = false)
		);
	}
}
