import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';

export interface TimeLogResponse {
	employeeEmail: string;
	worksiteCode: string;
	worksiteZoneId: string;
	entryTime: string;
	exitTime?: string | { present: boolean; value?: string | null } | null;
}

interface PageResponse<T> {
	content: T[];
}

@Injectable({ providedIn: 'root' })
export class TimeLogService {
	private readonly baseUrl = '/api/v1/employees';

	constructor(private readonly http: HttpClient) { }

	searchByEmployee(email: string): Observable<TimeLogResponse[]> {
		const encodedEmail = encodeURIComponent(email);
		const url = `${this.baseUrl}/${encodedEmail}/timelogs/`;

		return this.http
			.get<PageResponse<TimeLogResponse>>(url)
			.pipe(map((response) => response.content ?? []));
	}

	clockIn(email: string, worksiteCode: string): Observable<TimeLogResponse> {
		const encodedEmail = encodeURIComponent(email);
		const url = `${this.baseUrl}/${encodedEmail}/timelogs/clock-in`;

		return this.http.post<TimeLogResponse>(url, null, {
			params: { worksiteCode }
		});
	}

	clockOut(email: string, worksiteCode: string): Observable<TimeLogResponse> {
		const encodedEmail = encodeURIComponent(email);
		const url = `${this.baseUrl}/${encodedEmail}/timelogs/clock-out`;

		return this.http.post<TimeLogResponse>(url, null, {
			params: { worksiteCode }
		});
	}
}
