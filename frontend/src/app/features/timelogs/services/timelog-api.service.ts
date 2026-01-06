import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { TimeLog } from '../models/timelog';

@Injectable({ providedIn: 'root' })
export class TimeLogService {
	private readonly baseUrl = '/api/v1/employees';

	constructor(private readonly http: HttpClient) { }

	searchByEmployee(email: string): Observable<TimeLog[]> {
		const encodedEmail = encodeURIComponent(email);
		const url = `${this.baseUrl}/${encodedEmail}/timelogs/`;
		return this.http
			.get<Page<TimeLog>>(url)
			.pipe(map((response) => response.content ?? []));
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
