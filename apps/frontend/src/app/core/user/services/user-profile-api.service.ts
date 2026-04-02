/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { environment } from '../../../../environments/environment';
import { UserPreferences } from '../models/user-preferences';

/**
 * Supported time formats for user preferences.
 */
export type TimeFormat = 'H12' | 'H24';

/**
 * Internal DTO representing the user profile as returned by the backend API.
 *
 * This type is intentionally kept private to this file to avoid leaking
 * backend-specific structures into the rest of the application.
 *
 * The service is responsible for mapping this DTO into domain-level models
 * (e.g., UserPreferences) consumed by the UI and other layers.
 */
interface AppUserProfile {
  username: string;
  locale: string;
  timeFormat: TimeFormat;
  defaultTimezone: string;
}

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
 * - Cache or store data
 * - React to authentication or session changes
 *
 * Those responsibilities belong to higher-level abstractions such as
 * a facade or store (e.g., CurrentUserFacade).
 */
@Injectable({ providedIn: 'root' })
export class UserProfileApiService {
  private readonly http = inject(HttpClient);

  /**
   * Base endpoint for user profile resources.
   */
  private readonly baseUrl = `${environment.apiBaseUrl}/appusers`;

  /**
   * Retrieves the full user profile from the backend.
   *
   * This method is intentionally private to prevent exposing backend DTOs
   * outside of this service.
   *
   * @param username - Unique identifier of the user
   * @returns Observable emitting the raw AppUserProfile DTO
   */
  private getProfile(username: string): Observable<AppUserProfile> {
    return this.http.get<AppUserProfile>(`${this.baseUrl}/${encodeURIComponent(username)}`);
  }

  /**
   * Retrieves user preferences for a given user.
   *
   * The backend profile is fetched and transformed into a UserPreferences
   * domain model, hiding any backend-specific structure.
   *
   * @param username - Unique identifier of the user
   * @returns Observable emitting the user's preferences
   */
  getPreferences(username: string): Observable<UserPreferences> {
    return this.getProfile(username).pipe(map((response) => this.toPreferences(response)));
  }

  /**
   * Updates user preferences for a given user.
   *
   * The backend response is mapped back into a UserPreferences model,
   * ensuring consistency with the rest of the application.
   *
   * @param username - Unique identifier of the user
   * @param payload - Preferences to update
   * @returns Observable emitting the updated preferences
   */
  updatePreferences(username: string, payload: UserPreferences): Observable<UserPreferences> {
    return this.http
      .put<AppUserProfile>(`${this.baseUrl}/${encodeURIComponent(username)}`, payload)
      .pipe(map((response) => this.toPreferences(response)));
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
  private toPreferences(response: AppUserProfile): UserPreferences {
    return {
      locale: response.locale,
      timeFormat: response.timeFormat,
      defaultTimezone: response.defaultTimezone,
    };
  }
}
