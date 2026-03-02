/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, finalize, shareReplay, tap } from 'rxjs/operators';

export interface AppPreferences {
  locale: string;
  timeFormat: '12h' | '24h';
  dateFormat: string;
  timezone: string;
  theme?: 'light' | 'dark';
}

@Injectable({ providedIn: 'root' })
export class PreferencesService {
  readonly prefs = signal<AppPreferences | null>(null);
  private inflight?: Observable<AppPreferences>;

  constructor(private http: HttpClient) {}

  load(force = false): Observable<AppPreferences> {
    const current = this.prefs();
    if (!force && current) return of(current);
    if (!force && this.inflight) return this.inflight;

    this.inflight = this.http.get<AppPreferences>('/api/preferences').pipe(
      tap((v) => this.prefs.set(v)),
      catchError((err) => {
        this.prefs.set(null);
        return throwError(() => err);
      }),
      finalize(() => (this.inflight = undefined)),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    return this.inflight;
  }

  clear(): void {
    this.prefs.set(null);
    this.inflight = undefined;
  }
}
