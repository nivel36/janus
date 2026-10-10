/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Router, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { FormComponent } from '../../../shared/ui/form/form.component';
import { AuthService } from '../../auth/auth.service';
import { Subject } from 'rxjs';
import { beforeEach, describe, expect, it, type Mock, vi } from 'vitest';

import { UserPreferences } from '../models/user-preferences';
import { CurrentUserFacade } from '../services/current-user.facade';
import { UserPreferencesPageComponent } from './user-preferences-page.component';

describe('UserPreferencesPageComponent', () => {
  const stored: UserPreferences = {
    locale: 'es-ES',
    timeFormat: 'H24',
    defaultTimezone: 'Europe/Madrid',
    theme: 'LIGHT',
  };
  let response: Subject<UserPreferences>;
  let updatePreferences: ReturnType<typeof vi.fn>;
  let navigate: Mock<Router['navigate']>;

  beforeEach(() => {
    response = new Subject<UserPreferences>();
    updatePreferences = vi.fn(() => response);
    navigate = vi.fn<Router['navigate']>().mockResolvedValue(true);
    TestBed.configureTestingModule({
      imports: [UserPreferencesPageComponent],
      providers: [
        provideRouter([]),
        provideTranslateService(),
        { provide: AuthService, useValue: { logout: vi.fn() } },
        {
          provide: CurrentUserFacade,
          useValue: {
            currentUser: signal(null),
            preferences: signal(stored),
            preferencesLoading: signal(false),
            preferencesError: signal(undefined),
            updatePreferences,
          },
        },
      ],
    });
    vi.spyOn(TestBed.inject(Router), 'navigate').mockImplementation(navigate);
  });

  it('loads the stored theme and submits changes with the other preferences', () => {
    const fixture = TestBed.createComponent(UserPreferencesPageComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    expect(component.form.controls.theme.value).toBe('LIGHT');

    component.form.controls.theme.setValue('DARK');
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    expect(updatePreferences).toHaveBeenCalledWith({ ...stored, theme: 'DARK' });
    expect(navigate).not.toHaveBeenCalled();

    response.next({ ...stored, theme: 'DARK' });
    response.complete();
    expect(component.form.controls.theme.value).toBe('DARK');
    expect(navigate).toHaveBeenCalledWith(['/']);
    expect(fixture.debugElement.query(By.directive(FormComponent)).componentInstance.saving()).toBe(false);
  });

  it('reports a failed save and stays on the preferences page', () => {
    const fixture = TestBed.createComponent(UserPreferencesPageComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.form.controls.theme.setValue('DARK');
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    response.error(new Error('Save failed'));

    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('userPreferences.errors.update');
    expect(fixture.debugElement.query(By.directive(FormComponent)).componentInstance.saving()).toBe(false);
    expect(navigate).not.toHaveBeenCalled();
  });
});
