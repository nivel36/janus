/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, expect, it } from 'vitest';

import { authChildGuard } from './core/auth/auth.guard';
import { JANUS_CLIENT_ROLES } from './core/auth/auth.models';
import { appRoutes } from './app.routes';

describe('application routes', () => {
  it('keeps forbidden public and protects all application routes through their parent', () => {
    const forbidden = appRoutes.find((route) => route.path === 'forbidden');
    const protectedParent = appRoutes.find((route) => route.path === '');

    expect(forbidden?.canActivate).toBeUndefined();
    expect(forbidden?.canActivateChild).toBeUndefined();
    expect(protectedParent).toMatchObject({
      canActivateChild: [authChildGuard],
      data: {
        clientRole: [JANUS_CLIENT_ROLES.EMPLOYEE, JANUS_CLIENT_ROLES.USER, JANUS_CLIENT_ROLES.ADMIN],
      },
    });
    expect(protectedParent?.component).toBeUndefined();
    expect(protectedParent?.loadComponent).toBeUndefined();
    expect(protectedParent?.children?.every((route) => route.data === undefined)).toBe(true);
  });
});
