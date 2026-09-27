/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

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
  let navigate: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    response = new Subject<UserPreferences>();
    updatePreferences = vi.fn(() => response);
    navigate = vi.fn();
    TestBed.configureTestingModule({
      imports: [UserPreferencesPageComponent],
      providers: [
        {
          provide: CurrentUserFacade,
          useValue: {
            preferences: signal(stored),
            preferencesLoading: signal(false),
            preferencesError: signal(undefined),
            updatePreferences,
          },
        },
        { provide: Router, useValue: { navigate } },
      ],
    }).overrideComponent(UserPreferencesPageComponent, {
      set: { template: '', imports: [] },
    });
  });

  it('loads the stored theme and submits changes with the other preferences', () => {
    const fixture = TestBed.createComponent(UserPreferencesPageComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    expect(component.form.controls.theme.value).toBe('LIGHT');

    component.form.controls.theme.setValue('DARK');
    component.save();
    expect(updatePreferences).toHaveBeenCalledWith({ ...stored, theme: 'DARK' });
    expect(navigate).not.toHaveBeenCalled();

    response.next({ ...stored, theme: 'DARK' });
    response.complete();
    expect(component.form.controls.theme.value).toBe('DARK');
    expect(navigate).toHaveBeenCalledWith(['/']);
    expect(component.saving()).toBe(false);
  });

  it('reports a failed save and stays on the preferences page', () => {
    const fixture = TestBed.createComponent(UserPreferencesPageComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.form.controls.theme.setValue('DARK');
    component.save();
    response.error(new Error('Save failed'));

    expect(component.errorMessage()).toBe('userPreferences.errors.update');
    expect(component.saving()).toBe(false);
    expect(navigate).not.toHaveBeenCalled();
  });
});
