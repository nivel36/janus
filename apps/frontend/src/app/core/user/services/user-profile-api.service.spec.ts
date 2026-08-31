/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { environment } from '../../../../environments/environment';
import { UserPreferences } from '../models/user-preferences';
import { UserProfileApiService } from './user-profile-api.service';

describe('UserProfileApiService', () => {
  let service: UserProfileApiService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(UserProfileApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('loads preferences through the current-user endpoint', () => {
    service.getPreferences().subscribe((preferences) => expect(preferences).toEqual(PREFERENCES));

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/appusers/me`);
    expect(request.request.method).toBe('GET');
    request.flush({ username: 'mutable-name', ...PREFERENCES });
  });

  it('updates preferences through the current-user endpoint', () => {
    service
      .updatePreferences(PREFERENCES)
      .subscribe((preferences) => expect(preferences).toEqual(PREFERENCES));

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/appusers/me`);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(PREFERENCES);
    request.flush({ username: 'mutable-name', ...PREFERENCES });
  });
});

const PREFERENCES: UserPreferences = {
  locale: 'es-ES',
  timeFormat: 'H24',
  defaultTimezone: 'Europe/Madrid',
};
