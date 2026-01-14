import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { TimeLog } from '../models/timelog';

@Injectable({ providedIn: 'root' })
export class TimeLogService {
	private readonly baseUrl = '/api/v1/employees';

	constructor(private readonly http: HttpClient) { }

	searchByEmployee(email: string, page?: number, size?: number): Observable<TimeLog[]> {
	  let params = new HttpParams();
	  if (page != null) params = params.set('page', String(page));
	  if (size != null) params = params.set('size', String(size));

	  return this.http
	    .get<Page<TimeLog>>(`${this.baseUrl}/${encodeURIComponent(email)}/timelogs/`, { params })
	    .pipe(map(r => r.content ?? []));
	}

	clockIn(email: string, worksiteCode: string): Observable<TimeLog> {
		const encodedEmail = encodeURIComponent(email);
		const url = `${this.baseUrl}/${encodedEmail}/timelogs/clock-in`;
		return this.http.post<TimeLog>(url, null, {
			params: { worksiteCode }
		});
	}

	clockOut(email: string, worksiteCode: string): Observable<TimeLog> {
		const encodedEmail = encodeURIComponent(email);
		const url = `${this.baseUrl}/${encodedEmail}/timelogs/clock-out`;
		return this.http.post<TimeLog>(url, null, {
			params: { worksiteCode }
		});
	}
}
