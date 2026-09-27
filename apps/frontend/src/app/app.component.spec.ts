/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';

import { AppComponent } from './app.component';
import { UserPreferences } from './core/user/models/user-preferences';
import { CurrentUserFacade } from './core/user/services/current-user.facade';

describe('AppComponent', () => {
  it('should create the app', async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        {
          provide: CurrentUserFacade,
          useValue: { preferences: signal(null) },
        },
        {
          provide: TranslateService,
          useValue: {
            currentLang: 'es-ES',
            getCurrentLang: () => 'es-ES',
            use: vi.fn(),
          },
        },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;

    expect(app).toBeTruthy();
  });

  it('should apply app language from user locale preferences', async () => {
    const useSpy = vi.fn();
    const preferences = signal<UserPreferences | null>(null);
    const onLangChange = new Subject<{ lang: string; translations: object }>();

    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        {
          provide: CurrentUserFacade,
          useValue: { preferences },
        },
        {
          provide: TranslateService,
          useValue: {
            currentLang: 'es-ES',
            getCurrentLang: () => 'es-ES',
            use: useSpy,
            onLangChange,
          },
        },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    preferences.set({
      locale: 'ca-ES',
      timeFormat: 'H24',
      theme: 'DARK',
      defaultTimezone: 'Europe/Madrid',
    });
    fixture.detectChanges();

    expect(useSpy).toHaveBeenCalledWith('ca-ES');
    expect(document.documentElement.lang).toBe('es-ES');

    onLangChange.next({ lang: 'ca-ES', translations: {} });

    expect(document.documentElement.lang).toBe('ca-ES');

    preferences.set({
      locale: 'en-GB',
      timeFormat: 'H24',
      theme: 'DARK',
      defaultTimezone: 'Europe/London',
    });
    fixture.detectChanges();

    expect(useSpy).toHaveBeenLastCalledWith('en-GB');
    expect(document.documentElement.lang).toBe('ca-ES');

    onLangChange.next({ lang: 'en-GB', translations: {} });

    expect(document.documentElement.lang).toBe('en-GB');
  });
  it('applies persisted themes and resets to dark without browser storage', async () => {
    const preferences = signal<UserPreferences | null>(null);
    const storageRead = vi.spyOn(Storage.prototype, 'getItem');
    const storageWrite = vi.spyOn(Storage.prototype, 'setItem');
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        { provide: CurrentUserFacade, useValue: { preferences } },
        {
          provide: TranslateService,
          useValue: {
            getCurrentLang: () => 'es-ES',
            use: vi.fn(),
            onLangChange: new Subject(),
          },
        },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    expect(document.documentElement.getAttribute('data-theme')).toBe('DARK');

    preferences.set({
      locale: 'es-ES',
      timeFormat: 'H24',
      defaultTimezone: 'UTC',
      theme: 'LIGHT',
    });
    fixture.detectChanges();
    expect(document.documentElement.getAttribute('data-theme')).toBe('LIGHT');

    preferences.set({ ...preferences()!, theme: 'DARK' });
    fixture.detectChanges();
    expect(document.documentElement.getAttribute('data-theme')).toBe('DARK');

    preferences.set({ ...preferences()!, theme: 'LIGHT' });
    fixture.detectChanges();
    preferences.set(null);
    fixture.detectChanges();
    expect(document.documentElement.getAttribute('data-theme')).toBe('DARK');
    expect(storageRead).not.toHaveBeenCalled();
    expect(storageWrite).not.toHaveBeenCalled();
    vi.restoreAllMocks();
  });

});
