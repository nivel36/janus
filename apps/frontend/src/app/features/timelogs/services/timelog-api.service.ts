import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { TimeLog } from '../models/timelog';
import { ClockActionAvailability } from '../models/clock-action-availability';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class TimeLogService {
  private readonly baseUrl = `${environment.apiUrl}/employees`;

  constructor(private readonly http: HttpClient) {}

  /**
   * The `page` parameter follows Spring Data pagination (0-based index).
   */
  searchByEmployee(email: string, page?: number, size?: number): Observable<TimeLog[]> {
    let params = new HttpParams();
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

  clockActionAvailability(email: string): Observable<ClockActionAvailability> {
    const encodedEmail = encodeURIComponent(email);
    const url = `${this.baseUrl}/${encodedEmail}/timelogs/clock-action-availability`;
    return this.http.get<ClockActionAvailability>(url);
  }

  streamClockActionAvailability(
    email: string,
    token: string,
    onAvailability: (availability: ClockActionAvailability) => void,
    onError?: () => void,
  ): () => void {
    const encodedEmail = encodeURIComponent(email);
    const url = `${this.baseUrl}/${encodedEmail}/timelogs/clock-action-availability/stream`;
    const abortController = new AbortController();

    void fetch(url, {
      method: 'GET',
      headers: {
        Accept: 'text/event-stream',
        Authorization: `Bearer ${token}`,
      },
      signal: abortController.signal,
    })
      .then(async (response) => {
        if (!response.ok || !response.body) {
          throw new Error('Unable to open clock action availability stream');
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        while (true) {
          const { value, done } = await reader.read();
          if (done) {
            break;
          }

          buffer += decoder.decode(value, { stream: true });
          const events = buffer.split('\n\n');
          buffer = events.pop() ?? '';

          events.forEach((eventChunk) => {
            const dataLine = eventChunk
              .split('\n')
              .find((line) => line.startsWith('data:'));
            if (!dataLine) {
              return;
            }

            const data = dataLine.replace(/^data:\s*/, '');
            try {
              onAvailability(JSON.parse(data) as ClockActionAvailability);
            } catch {
              // Ignore malformed event payloads.
            }
          });
        }
      })
      .catch(() => {
        if (!abortController.signal.aborted) {
          onError?.();
        }
      });

    return () => abortController.abort();
  }

}
