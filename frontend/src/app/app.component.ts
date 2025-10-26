import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { PanelModule } from 'primeng/panel';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';

type JsonNullable<T> = T | { value?: T | null } | null | undefined;

interface TimeLogApiModel {
  entryTime: string;
  exitTime?: JsonNullable<string>;
}

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  size: number;
  number: number;
}

interface TimeLogRow {
  entryTime: Date;
  exitTime: Date | null;
}

interface TimeLogViewRow {
  dateDisplay: string;
  entryDisplay: string;
  exitDisplay: string;
  durationDisplay: string;
  entryTime: Date;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, PanelModule, CardModule, ButtonModule, TableModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
  providers: [DatePipe]
})
export class AppComponent implements OnInit, OnDestroy {
  private static readonly EMPLOYEE_EMAIL = 'aferrer@nivel36.es';
  private static readonly WORKSITE_CODE = 'BCN-HQ';
  private readonly http = inject(HttpClient);
  private readonly datePipe = inject(DatePipe);

  protected readonly currentTimeText = signal('');
  protected readonly currentTimeIso = signal('');
  protected readonly logs = signal<TimeLogViewRow[]>([]);
  protected readonly loading = signal(false);
  protected readonly clockInLoading = signal(false);
  protected readonly clockInError = signal<string | null>(null);
  protected readonly tableError = signal<string | null>(null);
  protected totalRecords = signal(0);
  protected rows = signal(10);
  protected first = signal(0);

  private readonly baseUrl = `/api/v1/employee/${AppComponent.EMPLOYEE_EMAIL}/timelogs`;
  private tickId: ReturnType<typeof setInterval> | undefined;
  private rawLogs: TimeLogRow[] = [];

  ngOnInit(): void {
    this.updateCurrentTime();
    this.tickId = setInterval(() => {
      this.updateCurrentTime();
    }, 1_000);
    this.loadLogs();
  }

  ngOnDestroy(): void {
    if (this.tickId) {
      clearInterval(this.tickId);
    }
  }

  protected onClockIn(): void {
    if (this.clockInLoading()) {
      return;
    }
    this.clockInError.set(null);
    this.clockInLoading.set(true);
    const params = new HttpParams().set('worksiteCode', AppComponent.WORKSITE_CODE);
    this.http
      .post<TimeLogApiModel>(`${this.baseUrl}/clock-in`, null, { params })
      .subscribe({
        next: () => {
          this.loadLogs();
        },
        error: (error: HttpErrorResponse) => {
          this.clockInError.set(this.buildErrorMessage(error));
          this.clockInLoading.set(false);
        }
      });
  }

  protected loadLogs(event?: TableLazyLoadEvent): void {
    if (this.loading()) {
      return;
    }
    const pageSize = event?.rows ?? this.rows();
    const first = event?.first ?? this.first();
    const page = Math.floor(first / pageSize);
    this.loading.set(true);
    this.tableError.set(null);

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', pageSize.toString())
      .set('sort', 'entryTime,desc');

    this.http.get<PageResponse<TimeLogApiModel>>(`${this.baseUrl}/`, { params }).subscribe({
      next: (response) => {
        this.clockInLoading.set(false);
        this.rows.set(response.size);
        this.totalRecords.set(response.totalElements);
        this.first.set(response.number * response.size);
        this.rawLogs = response.content.map((log) => this.mapToRow(log));
        this.refreshViewRows();
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.clockInLoading.set(false);
        this.tableError.set(this.buildErrorMessage(error));
      }
    });
  }

  private refreshViewRows(): void {
    this.logs.set(this.rawLogs.map((log) => this.mapToView(log)));
  }

  private mapToRow(apiModel: TimeLogApiModel): TimeLogRow {
    return {
      entryTime: new Date(apiModel.entryTime),
      exitTime: this.extractInstant(apiModel.exitTime)
    };
  }

  private mapToView(row: TimeLogRow): TimeLogViewRow {
    const entry = row.entryTime;
    const exit = row.exitTime;
    return {
      entryTime: entry,
      dateDisplay: this.formatDate(entry, 'dd/MM/yyyy'),
      entryDisplay: this.formatDate(entry, 'HH:mm'),
      exitDisplay: exit ? this.formatDate(exit, 'HH:mm') : '—',
      durationDisplay: this.formatDuration(entry, exit)
    };
  }

  private extractInstant(value: JsonNullable<string> | undefined): Date | null {
    if (!value) {
      return null;
    }
    if (typeof value === 'string') {
      return value ? new Date(value) : null;
    }
    if (typeof value === 'object') {
      const candidate = value.value;
      return candidate ? new Date(candidate) : null;
    }
    return null;
  }

  private updateCurrentTime(): void {
    const now = new Date();
    this.currentTimeText.set(this.formatDate(now, 'HH:mm:ss'));
    this.currentTimeIso.set(now.toISOString());
    if (this.rawLogs.length > 0) {
      this.refreshViewRows();
    }
  }

  private formatDate(date: Date, format: string): string {
    return this.datePipe.transform(date, format) ?? '';
  }

  private formatDuration(entry: Date, exit: Date | null): string {
    const end = exit ?? new Date();
    const diffMillis = Math.max(end.getTime() - entry.getTime(), 0);
    const totalMinutes = Math.floor(diffMillis / 60000);
    const hours = Math.floor(totalMinutes / 60);
    const minutes = totalMinutes % 60;
    return `${hours}h ${minutes.toString().padStart(2, '0')}m`;
  }

  private buildErrorMessage(error: HttpErrorResponse): string {
    if (error.error?.message) {
      return error.error.message;
    }
    if (typeof error.error === 'string' && error.error.trim().length > 0) {
      return error.error;
    }
    return 'No hemos podido completar la operación. Inténtalo de nuevo más tarde.';
  }
}
