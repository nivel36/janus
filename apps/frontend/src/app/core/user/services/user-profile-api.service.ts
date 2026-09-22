/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Injectable, inject } from '@angular/core';
import { HttpContext } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, switchMap, tap } from 'rxjs/operators';

import { TimeFormat as ApiTimeFormat } from '../../../api/generated/model/timeFormat';
import { UserPreferences, type TimeFormat } from '../models/user-preferences';
import { AppUsersService } from '../../../api/generated/api/appUsers.service';
import { AppUserResponse } from '../../../api/generated/model/appUserResponse';
import {
  ACTIVE_SCREEN_HTTP_RETRY_POLICY,
  HTTP_RETRY_POLICY,
  type HttpRetryPolicy,
} from '../../http/http-retry.interceptor';

const PROFILE_LOAD_RETRY_POLICY: HttpRetryPolicy = {
  retries: 10,
  baseDelayMs: 1_000,
};

/**
 * Service responsible exclusively for communication with the user profile API.
 *
 * This service:
 * - Encapsulates all HTTP interactions related to user profile data
 * - Hides backend DTOs from the rest of the application
 * - Maps API responses into domain models (UserPreferences)
 *
 * This service does NOT:
 * - Maintain any application state
 * - Know about the authenticated user
 * - Maintain user preferences or authentication state
 * - React to authentication or session changes
 *
 * Those responsibilities belong to higher-level abstractions such as
 * a facade or store (e.g., CurrentUserFacade).
 */
@Injectable({ providedIn: 'root' })
export class UserProfileApiService {
  private readonly api = inject(AppUsersService);
  private profileId: string | null = null;

  /**
   * Retrieves the full user profile from the backend.
   *
   * This method is intentionally private to prevent exposing backend DTOs
   * outside of this service.
   *
   * @returns Observable emitting the raw AppUserProfile DTO
   */
  private getProfile(
    retryPolicy: HttpRetryPolicy = PROFILE_LOAD_RETRY_POLICY,
  ): Observable<AppUserResponse> {
    return this.api.findCurrentAppUser('body', false, {
      context: new HttpContext().set(HTTP_RETRY_POLICY, retryPolicy),
    });
  }

  /**
   * Retrieves user preferences for a given user.
   *
   * The backend profile is fetched and transformed into a UserPreferences
   * domain model, hiding any backend-specific structure.
   *
   * @returns Observable emitting the user's preferences
   */
  getPreferences(): Observable<UserPreferences> {
    return this.getProfile().pipe(
      tap((response) => (this.profileId = response.id)),
      map((response) => this.toPreferences(response)),
    );
  }

  /**
   * Updates user preferences for a given user.
   *
   * The backend response is mapped back into a UserPreferences model,
   * ensuring consistency with the rest of the application.
   *
   * @param payload - Preferences to update
   * @returns Observable emitting the updated preferences
   */
  updatePreferences(payload: UserPreferences): Observable<UserPreferences> {
    const profileId$ = this.profileId
      ? of(this.profileId)
      : this.getProfile(ACTIVE_SCREEN_HTTP_RETRY_POLICY).pipe(map((profile) => profile.id));

    return profileId$.pipe(
      switchMap((profileId) =>
        this.api.updateAppUser(profileId, {
          ...payload,
          timeFormat: payload.timeFormat as ApiTimeFormat,
        }),
      ),
      tap((response) => (this.profileId = response.id)),
      map((response) => this.toPreferences(response)),
    );
  }

  /**
   * Maps a backend AppUserProfile DTO to a UserPreferences domain model.
   *
   * This method centralizes the transformation logic, allowing the backend
   * contract to evolve without impacting the rest of the application.
   *
   * @param response - Raw backend profile DTO
   * @returns UserPreferences domain model
   */
  private toPreferences(response: AppUserResponse): UserPreferences {
    return {
      locale: response.locale,
      timeFormat: response.timeFormat as TimeFormat,
      defaultTimezone: response.defaultTimezone,
    };
  }
}
