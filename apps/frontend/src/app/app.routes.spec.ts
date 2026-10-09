/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, expect, it } from 'vitest';

import { authChildGuard } from './core/auth/auth.guard';
import { JANUS_CLIENT_ROLES } from './core/auth/auth.models';
import { appRoutes } from './app.routes';

describe('application routes', () => {
  it('redirects the root to the time logs page', async () => {
    const children = appRoutes.find((route) => route.path === '')?.children;
    const root = children?.find((route) => route.path === '');
    const timelogs = children?.find((route) => route.path === 'timelogs');

    expect(root).toMatchObject({ pathMatch: 'full', redirectTo: 'timelogs' });
    expect(timelogs?.loadComponent).toBeDefined();
    const component = await timelogs!.loadComponent!();
    const { TimelogsPageComponent } = await import(
      './features/timelogs/pages/timelogs-page.component'
    );
    expect(component).toBe(TimelogsPageComponent);
  });

  it('keeps forbidden public and protects all application routes through their parent', () => {
    const forbidden = appRoutes.find((route) => route.path === 'forbidden');
    const protectedParent = appRoutes.find((route) => route.path === '');

    expect(forbidden?.canActivate).toBeUndefined();
    expect(forbidden?.canActivateChild).toBeUndefined();
    expect(protectedParent).toMatchObject({
      canActivateChild: [authChildGuard],
      data: {
        clientRole: [
          JANUS_CLIENT_ROLES.EMPLOYEE,
          JANUS_CLIENT_ROLES.USER,
          JANUS_CLIENT_ROLES.ADMIN,
        ],
      },
    });
    expect(protectedParent?.component).toBeUndefined();
    expect(protectedParent?.loadComponent).toBeUndefined();
    const adminOnlyPaths = ['application-settings', 'worksites/new', 'worksites/:code/edit'];
    const adminOnlyRoutes = protectedParent?.children?.filter((route) =>
      adminOnlyPaths.includes(route.path ?? ''),
    );

    expect(adminOnlyRoutes).toHaveLength(adminOnlyPaths.length);
    expect(
      adminOnlyRoutes?.every((route) => route.data?.['clientRole'] === JANUS_CLIENT_ROLES.ADMIN),
    ).toBe(true);
    expect(
      protectedParent?.children
        ?.filter((route) => !adminOnlyPaths.includes(route.path ?? ''))
        .every((route) => route.data === undefined),
    ).toBe(true);
  });
});
