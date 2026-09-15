/**
 * SPDX-License-Identifier: Apache-2.0
 */

import { computed, Injectable, inject } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { combineLatest, Observable, of, ReplaySubject } from 'rxjs';
import { catchError, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';

import { AuthService } from '../../auth/auth.service';
import { JANUS_API_CLIENT_ID, JANUS_CLIENT_ROLES } from '../../auth/auth.models';
import { UserProfileApiService } from './user-profile-api.service';
import { User } from '../models/user';
import { UserPreferences } from '../models/user-preferences';

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
 * - Provide signals representing the current user state
 * - Derive high-level flags (e.g., roles)
 * - Orchestrate loading of user-related data (e.g., preferences)
 * - Expose a single source of truth (`currentUser`) for the UI
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
  private readonly authentication$ = toObservable(this.authService.isAuthenticated);

  /**
   * Trigger used to force a reload of the current user preferences.
   *
   * A value is emitted:
   * - After application start (via startWith)
   * - After a successful preference update
   */
  private readonly preferencesReload$ = new ReplaySubject<void>(1);

  /**
   * Whether the user is authenticated.
   */
  readonly isAuthenticated = this.authService.isAuthenticated;

  /**
   * The user's email extracted from claims.
   */
  readonly email = computed(() => this.authService.claims()?.email ?? null);

  /**
   * The user's full name derived from claims.
   */
  readonly fullName = computed(() => {
    const claims = this.authService.claims();
    return `${claims?.given_name ?? ''} ${claims?.family_name ?? ''}`.trim();
  });

  /**
   * The raw permissions object.
   */
  readonly permissions = this.authService.permissions;

  /**
   * The resolved username of the current user.
   */
  readonly username = this.authService.username;

  /**
   * Emits the user preferences for the current authenticated user.
   *
   * Preferences are loaded reactively when:
   * - The authentication state changes
   * - A manual reload is requested after a successful save
   *
   * If the user is not authenticated, null is emitted.
   *
   * Errors during loading are swallowed and mapped to null to avoid
   * breaking the user stream.
   */
  readonly preferences$ = combineLatest([
    this.authentication$,
    this.preferencesReload$.pipe(startWith(void 0)),
  ]).pipe(
    switchMap(([isAuthenticated]) => {
      if (!isAuthenticated) {
        return of(null);
      }

      return this.userProfileApi.getPreferences().pipe(catchError(() => of(null)));
    }),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  /** The latest remotely loaded preferences, adapted once for local signal consumers. */
  readonly preferences = toSignal(this.preferences$, { initialValue: null });

  /**
   * Whether the current user has the ADMIN role.
   */
  readonly isAdmin = computed(() => this.hasClientRole(JANUS_CLIENT_ROLES.ADMIN));

  /**
   * Whether the current user has the USER role.
   */
  readonly isUser = computed(() => this.hasClientRole(JANUS_CLIENT_ROLES.USER));

  /**
   * Whether the current user has the EMPLOYEE role.
   */
  readonly isEmployee = computed(() => this.hasClientRole(JANUS_CLIENT_ROLES.EMPLOYEE));

  /**
   * A fully composed User model representing the current user.
   *
   * This is the main entry point for UI consumption.
   * It combines authentication state, identity, roles and preferences
   * into a single immutable object.
   */
  readonly currentUser = computed<User>(() => ({
    username: this.username(),
    email: this.email(),
    fullName: this.fullName(),
    isAuthenticated: this.isAuthenticated(),
    isAdmin: this.isAdmin(),
    isUser: this.isUser(),
    isEmployee: this.isEmployee(),
    preferences: this.preferences(),
  }));

  /**
   * Updates preferences for the current authenticated user.
   *
   * After a successful update, a reload event is emitted so every consumer
   * of `preferences$` and `currentUser` receives the persisted values.
   *
   * @param payload - New preferences to persist
   * @returns Observable emitting updated preferences
   */
  updatePreferences(payload: UserPreferences): Observable<UserPreferences> {
    return this.userProfileApi.updatePreferences(payload).pipe(
      tap(() => {
        this.preferencesReload$.next();
      }),
    );
  }

  private hasClientRole(role: string): boolean {
    return this.permissions().clientRoles[JANUS_API_CLIENT_ID]?.includes(role) ?? false;
  }
}
