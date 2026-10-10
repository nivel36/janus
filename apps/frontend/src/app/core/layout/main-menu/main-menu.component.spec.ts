/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideRouter, Router } from '@angular/router';
import { By } from '@angular/platform-browser';
import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { AuthService } from '../../auth/auth.service';
import { CurrentUserFacade } from '../../user/services/current-user.facade';
import { MainMenuComponent } from './main-menu.component';
import { ButtonComponent } from '../../../shared/ui/button/button.component';

describe('MainMenuComponent', () => {
  const logout = vi.fn<() => Promise<void>>();
  const currentUser = signal<{ fullName: string; isAdmin: boolean; isUser: boolean } | null>(null);

  beforeEach(async () => {
    logout.mockReset().mockResolvedValue(undefined);
    currentUser.set(null);

    await TestBed.configureTestingModule({
      imports: [MainMenuComponent],
      providers: [
        { provide: AuthService, useValue: { logout } },
        {
          provide: CurrentUserFacade,
          useValue: {
            currentUser,
          },
        },
        provideRouter([]),
        provideTranslateService(),
      ],
    }).compileComponents();
  });

  it.each([[false, false], [false, true], [true, false]])('uses shared buttons (admin: %s, user: %s)', (isAdmin, isUser) => {
    currentUser.set({ fullName: 'Test User', isAdmin, isUser });
    const fixture = TestBed.createComponent(MainMenuComponent);
    fixture.detectChanges();

    const buttons = fixture.debugElement
      .queryAll(By.directive(ButtonComponent))
      .map((element) => element.componentInstance as ButtonComponent);

    expect(buttons.map((button) => button.routerLink())).toEqual([
      '/user-preferences',
      '/clock',
      '/timelogs',
      '/worksites',
      ...(isAdmin || isUser ? ['/employees'] : []),
      '/schedules',
      ...(isAdmin ? ['/application-settings'] : []),
      undefined,
    ]);
    const employeeLink = fixture.nativeElement.querySelector('a[href="/employees"]');
    if (isAdmin || isUser) {
      expect(employeeLink.querySelector('svg').getAttribute('data-icon')).toBe('users-between-lines');
    } else {
      expect(employeeLink).toBeNull();
    }
    expect(buttons.every((button) => button.variant() === 'text')).toBe(true);
    expect(buttons.slice(1).every((button) => button.icon())).toBe(true);
    expect(buttons.every((button) => !!button.ariaLabel() && !!button.title())).toBe(true);
    expect(fixture.nativeElement.querySelector('a').getAttribute('href')).toBe(
      '/user-preferences',
    );

    fixture.nativeElement.querySelector('button').click();
    expect(logout).toHaveBeenCalledOnce();
  });

  it('delegates logout navigation exclusively to AuthService', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate');
    const component = TestBed.createComponent(MainMenuComponent).componentInstance;

    await component.logout();

    expect(logout).toHaveBeenCalledOnce();
    expect(navigate).not.toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalledWith(['/login']);
  });
});
