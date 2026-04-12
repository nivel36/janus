import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { TimeLog } from '../models/timelog';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class TimeLogService {
  private readonly baseUrl = `${environment.apiBaseUrl}/employees`;

  constructor(private readonly http: HttpClient) {}

  /**
   * The `page` parameter follows Spring Data pagination (0-based index).
   */
  searchByEmployee(email: string, page?: number, size?: number): Observable<TimeLog[]> {
    let params = new HttpParams().set('sort', 'entryTime,desc');
    if (page != null) {
      params = params.set('page', String(page));
    }
    if (size != null) {
      params = params.set('size', String(size));
    }

    return this.http
      .get<Page<TimeLog>>(`${this.baseUrl}/${encodeURIComponent(email)}/timelogs/`, { params })
      .pipe(map((r) => r.content ?? []));
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
