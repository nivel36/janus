/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, finalize, map, shareReplay, tap } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/auth/auth.service';

export type TimeFormat = 'H12' | 'H24';

export interface AppPreferences {
  locale: string;
  timeFormat: TimeFormat;
  defaultTimezone: string;
}

interface AppUserPreferencesResponse extends AppPreferences {
  username: string;
}

@Injectable({ providedIn: 'root' })
export class PreferencesService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly baseUrl = `${environment.apiBaseUrl}/appusers`;

  readonly prefs = signal<AppPreferences | null>(null);
  private inflight?: Observable<AppPreferences>;

  load(force = false): Observable<AppPreferences> {
    const current = this.prefs();
    if (!force && current) {
      return of(current);
    }
    if (!force && this.inflight) {
      return this.inflight;
    }

    const username = this.resolveUsername();
    this.inflight = this.http
      .get<AppUserPreferencesResponse>(`${this.baseUrl}/${encodeURIComponent(username)}`)
      .pipe(
        map((response) => this.toPreferences(response)),
        tap((preferences) => this.prefs.set(preferences)),
        catchError((err) => {
          this.prefs.set(null);
          return throwError(() => err);
        }),
        finalize(() => (this.inflight = undefined)),
        shareReplay({ bufferSize: 1, refCount: false }),
      );

    return this.inflight;
  }

  update(payload: AppPreferences): Observable<AppPreferences> {
    const username = this.resolveUsername();

    return this.http
      .put<AppUserPreferencesResponse>(`${this.baseUrl}/${encodeURIComponent(username)}`, payload)
      .pipe(
        map((response) => this.toPreferences(response)),
        tap((preferences) => this.prefs.set(preferences)),
      );
  }

  clear(): void {
    this.prefs.set(null);
    this.inflight = undefined;
  }

  private resolveUsername(): string {
    const claims = this.authService.getClaims();
    const username = claims?.email ?? claims?.preferred_username;

    if (!username) {
      throw new Error('Cannot resolve username for preferences');
    }

    return username;
  }

  private toPreferences(response: AppUserPreferencesResponse): AppPreferences {
    return {
      locale: response.locale,
      timeFormat: response.timeFormat,
      defaultTimezone: response.defaultTimezone,
    };
  }
}
