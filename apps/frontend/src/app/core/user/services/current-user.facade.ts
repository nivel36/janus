/**
 * SPDX-License-Identifier: Apache-2.0
 */

import { Injectable, inject } from '@angular/core';
import { combineLatest, Observable, of, ReplaySubject } from 'rxjs';
import {
  map,
  distinctUntilChanged,
  shareReplay,
  switchMap,
  catchError,
  startWith,
  tap,
} from 'rxjs/operators';

import { AuthService } from '../../auth/auth.service';
import { UserProfileApiService } from './user-profile-api.service';
import { User } from '../models/user';
import { UserPreferences } from '../models/user-preferences';
import { retryTransientHttpErrors } from '../../../shared/utils/http-retry.util';

/**
 * Facade responsible for exposing the current authenticated user as a
 * fully composed, UI-ready model.
 *
 * This class aggregates multiple sources:
 * - Authentication state (AuthService)
 * - Identity claims (email, name, username)
 * - Permissions / roles
 * - User preferences (via UserProfileApiService)
 *
 * Responsibilities:
 * - Provide reactive streams representing the current user state
 * - Derive high-level flags (e.g., roles)
 * - Orchestrate loading of user-related data (e.g., preferences)
 * - Expose a single source of truth (`currentUser$`) for the UI
 *
 * This class does NOT:
 * - Perform direct HTTP mapping (delegated to API services)
 * - Expose backend DTOs
 * - Maintain imperative mutable state (state is derived reactively)
 */
@Injectable({ providedIn: 'root' })
export class CurrentUserFacade {
  private readonly authService = inject(AuthService);
  private readonly userProfileApi = inject(UserProfileApiService);

  /**
   * Trigger used to force a reload of the current user preferences.
   *
   * A value is emitted:
   * - After application start (via startWith)
   * - After a successful preference update
   */
  private readonly preferencesReload$ = new ReplaySubject<void>(1);

  /**
   * Emits whether the user is authenticated.
   */
  readonly isAuthenticated$ = this.authService.isAuthenticated$;

  /**
   * Emits the user's email extracted from claims.
   */
  readonly email$ = this.authService.claims$.pipe(
    map((claims) => claims?.email ?? null),
    distinctUntilChanged(),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  /**
   * Emits the user's full name derived from claims.
   */
  readonly fullName$ = this.authService.claims$.pipe(
    map((claims) => `${claims?.given_name ?? ''} ${claims?.family_name ?? ''}`.trim()),
    distinctUntilChanged(),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  /**
   * Emits the raw permissions object.
   */
  readonly permissions$ = this.authService.permissions$.pipe(
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  /**
   * Emits the resolved username of the current user.
   */
  readonly username$ = this.authService.username$.pipe(
    distinctUntilChanged(),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  /**
   * Emits the user preferences for the current authenticated user.
   *
   * Preferences are loaded reactively when:
   * - The authentication state changes
   * - The username changes
   * - A manual reload is requested after a successful save
   *
   * If the user is not authenticated or no username is available,
   * null is emitted.
   *
   * Errors during loading are swallowed and mapped to null to avoid
   * breaking the user stream.
   */
  readonly preferences$ = combineLatest([
    this.isAuthenticated$,
    this.username$,
    this.preferencesReload$.pipe(startWith(void 0)),
  ]).pipe(
    switchMap(([isAuthenticated, username]) => {
      if (!isAuthenticated || !username) {
        return of(null);
      }

      return this.userProfileApi.getPreferences(username).pipe(
        retryTransientHttpErrors(),
        catchError(() => of(null)),
      );
    }),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  /**
   * Emits whether the current user has the ADMIN role.
   */
  readonly isAdmin$ = this.permissions$.pipe(
    map((p) => p.realmRoles.includes('JANUS_ADMIN')),
    distinctUntilChanged(),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  /**
   * Emits whether the current user has the USER role.
   */
  readonly isUser$ = this.permissions$.pipe(
    map((p) => p.realmRoles.includes('JANUS_USER')),
    distinctUntilChanged(),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  /**
   * Emits whether the current user has the EMPLOYEE role.
   */
  readonly isEmployee$ = this.permissions$.pipe(
    map((p) => p.realmRoles.includes('JANUS_EMPLOYEE')),
    distinctUntilChanged(),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  /**
   * Emits a fully composed User model representing the current user.
   *
   * This is the main entry point for UI consumption.
   * It combines authentication state, identity, roles and preferences
   * into a single immutable object.
   */
  readonly currentUser$: Observable<User> = combineLatest([
    this.isAuthenticated$,
    this.username$,
    this.email$,
    this.fullName$,
    this.isAdmin$,
    this.isUser$,
    this.isEmployee$,
    this.preferences$,
  ]).pipe(
    map(
      ([isAuthenticated, username, email, fullName, isAdmin, isUser, isEmployee, preferences]) => ({
        username,
        email,
        fullName,
        isAuthenticated,
        isAdmin,
        isUser,
        isEmployee,
        preferences,
      }),
    ),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  /**
   * Updates preferences for the current authenticated user.
   *
   * After a successful update, a reload event is emitted so every consumer
   * of `preferences$` and `currentUser$` receives the persisted values.
   *
   * @param payload - New preferences to persist
   * @returns Observable emitting updated preferences
   */
  updatePreferences(payload: UserPreferences): Observable<UserPreferences> {
    const username = this.resolveUsername();

    return this.userProfileApi.updatePreferences(username, payload).pipe(
      tap(() => {
        this.preferencesReload$.next();
      }),
    );
  }

  /**
   * Resolves the username of the currently authenticated user
   * from the authentication claims.
   *
   * @throws Error if no username can be resolved
   */
  private resolveUsername(): string {
    const claims = this.authService.getClaims();
    const username = claims?.email ?? claims?.preferred_username;

    if (!username) {
      throw new Error('Cannot resolve username for profile');
    }

    return username;
  }

  /**
   * Returns whether the current user has the ADMIN role.
   *
   * This synchronous helper is useful in imperative code paths
   * where a reactive stream is not convenient.
   *
   * @returns True when the current user has the ADMIN role
   */
  isAdmin(): boolean {
    return this.authService.hasRealmRole('JANUS_ADMIN');
  }

  /**
   * Returns whether the current user has the USER role.
   *
   * @returns True when the current user has the USER role
   */
  isUser(): boolean {
    return this.authService.hasRealmRole('JANUS_USER');
  }

  /**
   * Returns whether the current user has the EMPLOYEE role.
   *
   * @returns True when the current user has the EMPLOYEE role
   */
  isEmployee(): boolean {
    return this.authService.hasRealmRole('JANUS_EMPLOYEE');
  }
}
