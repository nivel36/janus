/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { CurrentUserFacade } from '../../../core/user/services/current-user.facade';
import { AuthService } from '../../../core/auth/auth.service';
import { TimezoneCatalog } from '../../../shared/services/timezone-catalog.service';
import { ApplicationSettingsApiService } from '../services/application-settings-api.service';
import { ApplicationSettingsPageComponent } from './application-settings-page.component';

describe('ApplicationSettingsPageComponent', () => {
  let component: ApplicationSettingsPageComponent;
  let fixture: ComponentFixture<ApplicationSettingsPageComponent>;
  let settingsLoad: Subject<typeof settings>;

  beforeEach(async () => {
    settingsLoad = new Subject<typeof settings>();

    await TestBed.configureTestingModule({
      imports: [ApplicationSettingsPageComponent],
      providers: [
        provideTranslateService(),
        provideRouter([]),
        { provide: ApplicationSettingsApiService, useValue: { find: () => settingsLoad } },
        {
          provide: CurrentUserFacade,
          useValue: {
            isAdmin: signal(true),
            currentUser: signal({ isAuthenticated: false, fullName: '', isAdmin: true }),
          },
        },
        { provide: AuthService, useValue: { logout: vi.fn() } },
        {
          provide: TimezoneCatalog,
          useValue: {
            createSearchState: () => ({
              items: signal([]),
              loading: signal(false),
              error: signal(null),
              panelOpen: signal(false),
              search: vi.fn(),
            }),
            displayWith: (value: { literal: string }) => value.literal,
            valueWith: (value: { zoneId: string }) => value.zoneId,
            resolve: () => null,
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApplicationSettingsPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('shows a loading state, then fills the form with the API response', async () => {
    expect(component.loading()).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('applicationSettings.messages.loading');

    settingsLoad.next(settings);
    settingsLoad.complete();
    await fixture.whenStable();

    expect(component.loading()).toBe(false);
    expect(component.form.getRawValue()).toEqual(settings);
  });
});

const settings = {
  daysUntilLocked: 12,
  employeeWorkplaceCreationAllowed: true,
  worksiteChangeDuringShiftAllowed: true,
  employeeManualTimelogEntryAllowed: false,
  defaultTimezone: 'Europe/Paris',
};
