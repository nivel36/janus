import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { TimeLog } from '../models/timelog';
import { environment } from '../../../../environments/environment';
import { Page } from '../../../shared/models/page.model';

@Injectable({ providedIn: 'root' })
export class TimeLogService {
  private readonly baseUrl = `${environment.apiBaseUrl}/employees`;

  constructor(private readonly http: HttpClient) {}

  /**
   * The `page` parameter follows Spring Data pagination (0-based index).
   */
  searchByEmployee(email: string, page = 0, size = 5): Observable<Page<TimeLog>> {
    let params = new HttpParams().set('sort', 'entryTime,desc');
    params = params.set('page', String(page));
    params = params.set('size', String(size));

    return this.http.get<Page<TimeLog>>(
      `${this.baseUrl}/${encodeURIComponent(email)}/timelogs/`,
      { params },
    );
  }

  searchLatestByEmployee(email: string): Observable<TimeLog | undefined> {
    const params = new HttpParams()
      .set('page', '0')
      .set('size', '1')
      .set('sort', 'entryTime,desc');

    return this.http
      .get<Page<TimeLog>>(`${this.baseUrl}/${encodeURIComponent(email)}/timelogs/`, { params })
      .pipe(map((r) => r.content?.[0]));
  }

  clockIn(email: string, worksiteCode: string): Observable<TimeLog> {
    const encodedEmail = encodeURIComponent(email);
    const url = `${this.baseUrl}/${encodedEmail}/timelogs/clock-in`;
    return this.http.post<TimeLog>(url, null, {
      params: { worksiteCode },
    });
  }

  clockOut(email: string, worksiteCode: string): Observable<TimeLog> {
    const encodedEmail = encodeURIComponent(email);
    const url = `${this.baseUrl}/${encodedEmail}/timelogs/clock-out`;
    return this.http.post<TimeLog>(url, null, {
      params: { worksiteCode },
    });
  }
}
