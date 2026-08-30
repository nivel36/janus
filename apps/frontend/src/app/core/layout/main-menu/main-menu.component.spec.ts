/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { AuthService } from '../../auth/auth.service';
import { CurrentUserFacade } from '../../user/services/current-user.facade';
import { MainMenuComponent } from './main-menu.component';

describe('MainMenuComponent', () => {
  const logout = vi.fn<() => Promise<void>>();
  const navigate = vi.fn();

  beforeEach(async () => {
    logout.mockReset().mockResolvedValue(undefined);
    navigate.mockReset();

    await TestBed.configureTestingModule({
      imports: [MainMenuComponent],
      providers: [
        { provide: AuthService, useValue: { logout } },
        {
          provide: CurrentUserFacade,
          useValue: {
            currentUser$: of(null),
            fullName$: of(null),
            isAdmin$: of(false),
          },
        },
        { provide: Router, useValue: { navigate } },
      ],
    }).compileComponents();
  });

  it('delegates logout navigation exclusively to AuthService', async () => {
    const component = TestBed.createComponent(MainMenuComponent).componentInstance;

    await component.logout();

    expect(logout).toHaveBeenCalledOnce();
    expect(navigate).not.toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalledWith(['/login']);
  });
});
