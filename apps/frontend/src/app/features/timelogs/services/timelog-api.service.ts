import { Injectable, inject } from '@angular/core';
import { HttpContext } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { TimeLog } from '../models/timelog';
import { TimeLogsService } from '../../../api/generated/api/timeLogs.service';
import { TimeLogResponse } from '../../../api/generated/model/timeLogResponse';
import {
  ACTIVE_SCREEN_HTTP_RETRY_POLICY,
  HTTP_RETRY_POLICY,
} from '../../../core/http/http-retry.interceptor';

export interface TimeLogPage {
  items: TimeLog[];
  totalItems: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

@Injectable({ providedIn: 'root' })
export class TimeLogService {
  private readonly api = inject(TimeLogsService);

  /**
   * The `page` parameter follows Spring Data pagination (0-based index).
   */
  search(page = 0, size = 10): Observable<TimeLogPage> {
    const context = new HttpContext().set(HTTP_RETRY_POLICY, ACTIVE_SCREEN_HTTP_RETRY_POLICY);
    return this.api
      .searchTimeLogs(undefined, undefined, undefined, page, size, ['entryTime,desc'], 'body', false, { context })
      .pipe(
        map((r) => ({
          items: r.content.map((item) => this.mapTimeLog(item)),
          totalItems: r.page.totalElements,
          page: r.page.number,
          pageSize: r.page.size,
          totalPages: r.page.totalPages,
        })),
      );
  }

  searchLatestByEmployee(email: string): Observable<TimeLog | undefined> {
    const context = new HttpContext().set(HTTP_RETRY_POLICY, ACTIVE_SCREEN_HTTP_RETRY_POLICY);
    return this.api
      .searchTimeLogs(email, undefined, undefined, 0, 1, ['entryTime,desc'], 'body', false, { context })
      .pipe(map((r) => r.content[0] ? this.mapTimeLog(r.content[0]) : undefined));
  }

  clockIn(email: string, worksiteCode: string): Observable<TimeLog> {
    return this.api.clockIn(email, worksiteCode).pipe(map((item) => this.mapTimeLog(item)));
  }

  clockOut(email: string, worksiteCode: string): Observable<TimeLog> {
    return this.api.clockOut(email, worksiteCode).pipe(map((item) => this.mapTimeLog(item)));
  }

  private mapTimeLog(response: TimeLogResponse): TimeLog {
    return response as TimeLog;
  }
}
