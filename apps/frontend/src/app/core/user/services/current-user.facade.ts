/**
 * SPDX-License-Identifier: Apache-2.0
 */

import { computed, Injectable, inject } from '@angular/core';
import { rxResource, toObservable } from '@angular/core/rxjs-interop';
import { filter, mergeMap, Observable, of, throwError } from 'rxjs';
import { tap } from 'rxjs/operators';

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
   * Remote preferences query. An undefined request keeps the resource idle while
   * there is no authenticated user; authentication changes cancel stale loads.
   * Resource errors remain available to consumers instead of being confused with
   * the valid "no value" state.
   */
  readonly preferencesResource = rxResource<UserPreferences, true | undefined>({
    params: () => (this.authService.isAuthenticated() ? true : undefined),
    stream: () => this.userProfileApi.getPreferences(),
  });

  /** The latest successfully loaded preferences, or null before a value exists. */
  readonly preferences = computed(() =>
    this.preferencesResource.hasValue() ? this.preferencesResource.value() : null,
  );

  private readonly preferencesQuery = computed(() => ({
    authenticated: this.isAuthenticated(),
    error: this.preferencesResource.error(),
    hasValue: this.preferencesResource.hasValue(),
    value: this.preferences(),
  }));

  /**
   * Observable compatibility adapter. It waits for the resource to settle and
   * preserves query failures on the error channel.
   */
  readonly preferences$ = toObservable(this.preferencesQuery).pipe(
    filter((query) => !query.authenticated || query.hasValue || query.error !== undefined),
    mergeMap((query) =>
      query.error === undefined ? of(query.value) : throwError(() => query.error),
    ),
  );

  /** Explicit query state exposed to screens that need loading and error feedback. */
  readonly preferencesLoading = computed(() => this.preferencesResource.isLoading());
  readonly preferencesError = computed(() => this.preferencesResource.error());

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
        this.preferencesResource.reload();
      }),
    );
  }

  reloadPreferences(): boolean {
    return this.preferencesResource.reload();
  }

  private hasClientRole(role: string): boolean {
    return this.permissions().clientRoles[JANUS_API_CLIENT_ID]?.includes(role) ?? false;
  }
}
