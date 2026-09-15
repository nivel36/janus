/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { AppUsersService } from '../../../api/generated/api/appUsers.service';
import { HTTP_RETRY_POLICY } from '../../http/http-retry.interceptor';
import { UserPreferences } from '../models/user-preferences';
import { UserProfileApiService } from './user-profile-api.service';

describe('UserProfileApiService', () => {
  let service: UserProfileApiService;
  let transport: {
    findCurrentAppUser: ReturnType<typeof vi.fn>;
    updateCurrentAppUser: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    transport = {
      findCurrentAppUser: vi.fn().mockReturnValue(of({ username: 'mutable-name', ...PREFERENCES })),
      updateCurrentAppUser: vi
        .fn()
        .mockReturnValue(of({ username: 'mutable-name', ...PREFERENCES })),
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

  it('updates preferences through the generated transport', () => {
    let result: UserPreferences | undefined;
    service.updatePreferences(PREFERENCES).subscribe((preferences) => (result = preferences));

    expect(transport.updateCurrentAppUser).toHaveBeenCalledWith(PREFERENCES);
    expect(result).toEqual(PREFERENCES);
  });
});

const PREFERENCES: UserPreferences = {
  locale: 'es-ES',
  timeFormat: 'H24',
  defaultTimezone: 'Europe/Madrid',
};
