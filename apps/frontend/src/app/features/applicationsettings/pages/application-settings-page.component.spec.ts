/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { HttpErrorResponse } from '@angular/common/http';
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
  let find: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    settingsLoad = new Subject<typeof settings>();
    find = vi.fn().mockReturnValue(settingsLoad);

    await TestBed.configureTestingModule({
      imports: [ApplicationSettingsPageComponent],
      providers: [
        provideTranslateService(),
        provideRouter([]),
        { provide: ApplicationSettingsApiService, useValue: { find, update: vi.fn() } },
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
    expect(find).toHaveBeenCalledOnce();
    expect(component.loading()).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('applicationSettings.messages.loading');

    settingsLoad.next(settings);
    settingsLoad.complete();
    await fixture.whenStable();

    expect(component.loading()).toBe(false);
    expect(component.form.getRawValue()).toEqual(settings);
  });


  it('exposes a resource load error', async () => {
    settingsLoad.error(new Error('request failed'));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(component.errorMessage()).toBe('applicationSettings.errors.load');
    expect(fixture.nativeElement.textContent).toContain('applicationSettings.errors.load');
  });

  it.each([404, 500])('handles load error %s and recovers only when retry is available', async (status) => {
    settingsLoad.error(new HttpErrorResponse({ status }));
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
    expect(fixture.nativeElement.querySelector('a[href="/"]')).not.toBeNull();
    const retry = fixture.nativeElement.querySelector('app-async-state button');
    if (status === 404) {
      expect(retry).toBeNull();
      expect(find).toHaveBeenCalledOnce();
      return;
    }
    const response = new Subject<typeof settings>();
    find.mockReturnValue(response);
    retry.click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-async-state button')).toBeNull();
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
    response.next(settings);
    response.complete();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('form')).not.toBeNull();
    expect(component.form.getRawValue()).toEqual(settings);
    expect(find).toHaveBeenCalledTimes(2);
  });

  it('cancels a pending load when the component is destroyed', () => {
    expect(settingsLoad.observed).toBe(true);

    fixture.destroy();

    expect(settingsLoad.observed).toBe(false);
  });
});

const settings = {
  daysUntilLocked: 12,
  employeeWorksiteCreationAllowed: true,
  worksiteChangeDuringShiftAllowed: true,
  employeeManualTimeLogEntryAllowed: false,
  defaultTimezone: 'Europe/Paris',
};
