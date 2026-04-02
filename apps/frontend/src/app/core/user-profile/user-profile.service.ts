/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, finalize, map, shareReplay, tap, switchMap } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';

export type TimeFormat = 'H12' | 'H24';

export interface UserPreferences {
  locale: string;
  timeFormat: TimeFormat;
  defaultTimezone: string;
}

export interface AppUserProfile extends UserPreferences {
  username: string;
}

@Injectable({ providedIn: 'root' })
export class UserProfileService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly baseUrl = `${environment.apiBaseUrl}/appusers`;

  readonly profile = signal<AppUserProfile | null>(null);
  readonly preferences = signal<UserPreferences | null>(null);
  private inflight?: Observable<UserPreferences>;

  constructor() {
    this.authService.isAuthenticated$
      .pipe(
        switchMap((authenticated) => {
          if (!authenticated) {
            this.clear();
            return of(null);
          }

          return this.loadPreferences(false).pipe(
            catchError(() => {
              this.clear();
              return of(null);
            }),
          );
        }),
      )
      .subscribe();
  }

  loadPreferences(force = false): Observable<UserPreferences> {
    const current = this.preferences();
    if (!force && current) {
      return of(current);
    }
    if (!force && this.inflight) {
      return this.inflight;
    }

    const username = this.resolveUsername();
    const request$ = this.http
      .get<AppUserProfile>(`${this.baseUrl}/${encodeURIComponent(username)}`)
      .pipe(
        tap((response) => {
          if (this.isActiveUser(username)) {
            this.profile.set(response);
          }
        }),
        map((response) => this.toPreferences(response)),
        tap((preferences) => {
          if (this.isActiveUser(username)) {
            this.preferences.set(preferences);
          }
        }),
        catchError((err) => {
          if (this.isActiveUser(username)) {
            this.profile.set(null);
            this.preferences.set(null);
          }
          return throwError(() => err);
        }),
        finalize(() => {
          if (this.inflight === request$) {
            this.inflight = undefined;
          }
        }),
        shareReplay({ bufferSize: 1, refCount: true }),
      );

    this.inflight = request$;
    return request$;
  }

  updatePreferences(payload: UserPreferences): Observable<UserPreferences> {
    const username = this.resolveUsername();

    return this.http
      .put<AppUserProfile>(`${this.baseUrl}/${encodeURIComponent(username)}`, payload)
      .pipe(
        tap((response) => this.profile.set(response)),
        map((response) => this.toPreferences(response)),
        tap((preferences) => this.preferences.set(preferences)),
      );
  }

  clear(): void {
    this.profile.set(null);
    this.preferences.set(null);
    this.inflight = undefined;
  }

  private resolveUsername(): string {
    const claims = this.authService.getClaims();
    const username = claims?.email ?? claims?.preferred_username;

    if (!username) {
      throw new Error('Cannot resolve username for profile');
    }

    return username;
  }

  private toPreferences(response: AppUserProfile): UserPreferences {
    return {
      locale: response.locale,
      timeFormat: response.timeFormat,
      defaultTimezone: response.defaultTimezone,
    };
  }

  private isActiveUser(username: string): boolean {
    const claims = this.authService.getClaims();
    const activeUsername = claims?.email ?? claims?.preferred_username;
    return Boolean(activeUsername) && activeUsername === username;
  }
}
