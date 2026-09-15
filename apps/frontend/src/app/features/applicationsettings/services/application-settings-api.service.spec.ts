/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ApplicationSettingsService as ApplicationSettingsTransportService } from '../../../api/generated/api/applicationSettings.service';
import {
  ACTIVE_SCREEN_HTTP_RETRY_POLICY,
  HTTP_RETRY_POLICY,
} from '../../../core/http/http-retry.interceptor';
import { ApplicationSettingsApiService } from './application-settings-api.service';

describe('ApplicationSettingsApiService', () => {
  let service: ApplicationSettingsApiService;
  let transport: {
    findApplicationSettings: ReturnType<typeof vi.fn>;
    updateApplicationSettings: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    transport = {
      findApplicationSettings: vi.fn().mockReturnValue(of(settings)),
      updateApplicationSettings: vi.fn().mockReturnValue(of(settings)),
    };

    TestBed.configureTestingModule({
      providers: [
        ApplicationSettingsApiService,
        { provide: ApplicationSettingsTransportService, useValue: transport },
      ],
    });
    service = TestBed.inject(ApplicationSettingsApiService);
  });

  it('loads settings through the generated transport with the active-screen retry policy', () => {
    service.find().subscribe();

    expect(transport.findApplicationSettings).toHaveBeenCalledOnce();
    const [, , options] = transport.findApplicationSettings.mock.calls[0];
    expect(options.context.get(HTTP_RETRY_POLICY)).toBe(ACTIVE_SCREEN_HTTP_RETRY_POLICY);
  });

  it('sends updates through the generated transport', () => {
    service.update(settings).subscribe();

    expect(transport.updateApplicationSettings).toHaveBeenCalledWith(settings);
  });
});

const settings = {
  daysUntilLocked: 7,
  employeeWorkplaceCreationAllowed: true,
  worksiteChangeDuringShiftAllowed: false,
  employeeManualTimelogEntryAllowed: false,
  defaultTimezone: 'Europe/Madrid',
};
