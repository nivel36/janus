/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';

import { AppComponent } from './app.component';
import { CurrentUserFacade } from './core/user/services/current-user.facade';

describe('AppComponent', () => {
  it('should create the app', async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        {
          provide: CurrentUserFacade,
          useValue: { preferences$: of(null) },
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

    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        {
          provide: CurrentUserFacade,
          useValue: {
            preferences$: of({
              locale: 'ca-ES',
              timeFormat: 'H24',
              defaultTimezone: 'Europe/Madrid',
            }),
          },
        },
        {
          provide: TranslateService,
          useValue: {
            currentLang: 'es-ES',
            getCurrentLang: () => 'es-ES',
            use: useSpy,
          },
        },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    expect(useSpy).toHaveBeenCalledWith('ca-ES');
  });
});
