/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, expect, it } from 'vitest';

import { authChildGuard } from './core/auth/auth.guard';
import { JANUS_CLIENT_ROLES } from './core/auth/auth.models';
import { appRoutes } from './app.routes';

describe('application routes', () => {
  it('redirects the root to the clock page', async () => {
    const children = appRoutes.find((route) => route.path === '')?.children;
    const root = children?.find((route) => route.path === '');
    const clock = children?.find((route) => route.path === 'clock');

    expect(root).toMatchObject({ pathMatch: 'full', redirectTo: 'clock' });
    expect(clock?.loadComponent).toBeDefined();
    const component = await clock!.loadComponent!();
    const { ClockPageComponent } = await import(
      './features/clock/pages/clock-page.component'
    );
    expect(component).toBe(ClockPageComponent);
  });

  it('loads the employee search for users and administrators', async () => {
    const route = appRoutes.find((route) => route.path === '')?.children?.find(
      (route) => route.path === 'employees',
    );
    expect(route?.data?.['clientRole']).toEqual([JANUS_CLIENT_ROLES.USER, JANUS_CLIENT_ROLES.ADMIN]);
    const { EmployeesPageComponent } = await import(
      './features/employees/pages/employees-page/employees-page.component'
    );
    expect(await route!.loadComponent!()).toBe(EmployeesPageComponent);
  });

  it('loads employee creation before the employee detail route for users and administrators', async () => {
    const children = appRoutes.find((route) => route.path === '')!.children!;
    const index = children.findIndex((route) => route.path === 'employees/new');
    expect(index).toBeGreaterThanOrEqual(0);
    expect(index).toBeLessThan(children.findIndex((route) => route.path === 'employees/:employeeNumber'));
    expect(children[index].data?.['clientRole']).toEqual([JANUS_CLIENT_ROLES.USER, JANUS_CLIENT_ROLES.ADMIN]);
    const { EmployeeCreatePageComponent } = await import(
      './features/employees/pages/employee-create-page/employee-create-page.component'
    );
    expect(await children[index].loadComponent!()).toBe(EmployeeCreatePageComponent);
  });

  it('loads employee editing before details for users and administrators', async () => {
    const children = appRoutes.find((route) => route.path === '')!.children!;
    const index = children.findIndex((route) => route.path === 'employees/:employeeNumber/edit');
    expect(index).toBeGreaterThanOrEqual(0);
    expect(index).toBeLessThan(children.findIndex((route) => route.path === 'employees/:employeeNumber'));
    expect(children[index].data?.['clientRole']).toEqual([JANUS_CLIENT_ROLES.USER, JANUS_CLIENT_ROLES.ADMIN]);
    const { EmployeeEditPageComponent } = await import(
      './features/employees/pages/employee-edit-page/employee-edit-page.component'
    );
    expect(await children[index].loadComponent!()).toBe(EmployeeEditPageComponent);
  });

  it('loads employee details for users and administrators', async () => {
    const route = appRoutes.find((route) => route.path === '')?.children?.find(
      (route) => route.path === 'employees/:employeeNumber',
    );
    expect(route?.data?.['clientRole']).toEqual([JANUS_CLIENT_ROLES.USER, JANUS_CLIENT_ROLES.ADMIN]);
    const { EmployeeDetailPageComponent } = await import(
      './features/employees/pages/employee-detail-page/employee-detail-page.component'
    );
    expect(await route!.loadComponent!()).toBe(EmployeeDetailPageComponent);
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
        ?.filter((route) => !adminOnlyPaths.includes(route.path ?? '') && !route.path?.startsWith('employees'))
        .every((route) => route.data === undefined),
    ).toBe(true);
  });
});
