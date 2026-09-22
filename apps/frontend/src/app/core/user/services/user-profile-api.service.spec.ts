/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { AppUsersService } from '../../../api/generated/api/appUsers.service';
import {
  ACTIVE_SCREEN_HTTP_RETRY_POLICY,
  HTTP_RETRY_POLICY,
} from '../../http/http-retry.interceptor';
import { UserPreferences } from '../models/user-preferences';
import { UserProfileApiService } from './user-profile-api.service';

describe('UserProfileApiService', () => {
  let service: UserProfileApiService;
  let transport: {
    findCurrentAppUser: ReturnType<typeof vi.fn>;
    updateAppUser: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    transport = {
      findCurrentAppUser: vi
        .fn()
        .mockReturnValue(of({ id: USER_ID, email: 'person@example.test', ...PREFERENCES })),
      updateAppUser: vi
        .fn()
        .mockReturnValue(of({ id: USER_ID, email: 'person@example.test', ...PREFERENCES })),
    };
    TestBed.configureTestingModule({
      providers: [UserProfileApiService, { provide: AppUsersService, useValue: transport }],
    });
    service = TestBed.inject(UserProfileApiService);
  });

  it('loads preferences through the generated transport with its retry policy', () => {
    let result: UserPreferences | undefined;
    service.getPreferences().subscribe((preferences) => (result = preferences));

    expect(result).toEqual(PREFERENCES);
    const [, , options] = transport.findCurrentAppUser.mock.calls[0];
    expect(options.context.get(HTTP_RETRY_POLICY)).toEqual({
      retries: 10,
      baseDelayMs: 1_000,
    });
  });

  it('reuses the UUID cached while loading preferences when saving', () => {
    let result: UserPreferences | undefined;
    service.getPreferences().subscribe();
    service.updatePreferences(PREFERENCES).subscribe((preferences) => (result = preferences));

    expect(transport.findCurrentAppUser).toHaveBeenCalledTimes(1);
    expect(transport.updateAppUser).toHaveBeenCalledWith(USER_ID, PREFERENCES);
    expect(result).toEqual(PREFERENCES);
  });

  it('uses the bounded active-screen retry policy when saving before a profile load', () => {
    service.updatePreferences(PREFERENCES).subscribe();

    const [, , options] = transport.findCurrentAppUser.mock.calls[0];
    expect(options.context.get(HTTP_RETRY_POLICY)).toEqual(ACTIVE_SCREEN_HTTP_RETRY_POLICY);
    expect(transport.updateAppUser).toHaveBeenCalledWith(USER_ID, PREFERENCES);
  });
});

const PREFERENCES: UserPreferences = {
  locale: 'es-ES',
  timeFormat: 'H24',
  defaultTimezone: 'Europe/Madrid',
};

const USER_ID = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa';
