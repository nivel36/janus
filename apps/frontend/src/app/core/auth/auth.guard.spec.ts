/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { TestBed } from '@angular/core/testing';
import { Router, type ActivatedRouteSnapshot, type RouterStateSnapshot } from '@angular/router';
import Keycloak from 'keycloak-js';
import type { AuthGuardData } from 'keycloak-angular';
import { describe, expect, it, vi } from 'vitest';
import { of, Subject, throwError } from 'rxjs';

import { isAccessAllowed } from './auth.guard';
import { JANUS_CLIENT_ROLES } from './auth.models';
import { AuthService } from './auth.service';
import { CurrentUserFacade } from '../user/services/current-user.facade';
import { UserPreferences } from '../user/models/user-preferences';

describe('isAccessAllowed', () => {
  const route = (data: Record<string, unknown>, parentData?: Record<string, unknown>) =>
    ({
      data,
      pathFromRoot: [...(parentData ? [{ data: parentData }] : []), { data }],
    }) as unknown as ActivatedRouteSnapshot;
  const state = { url: '/protected' } as RouterStateSnapshot;
  const keycloak = { login: vi.fn().mockResolvedValue(undefined) };
  const router = {
    parseUrl: vi.fn((url: string) => ({ redirectTo: url })),
    createUrlTree: vi.fn((commands: string[], extras: unknown) => ({ commands, extras })),
  };
  const auth = { login: vi.fn().mockResolvedValue(undefined), isUsableIdentity: () => true };
  const preferences: UserPreferences = {
    locale: 'es-ES',
    timeFormat: 'H24',
    defaultTimezone: 'Europe/Madrid',
  };

  const authData = (
    realmRoles: string[],
    clientRoles: Record<string, string[]>,
  ): AuthGuardData => ({
    authenticated: true,
    grantedRoles: { realmRoles, resourceRoles: clientRoles },
    keycloak: keycloak as unknown as Keycloak,
  });

  async function evaluate(
    data: Record<string, unknown>,
    authentication: AuthGuardData,
    parentData?: Record<string, unknown>,
    routerState = state,
    preferences$ = of<UserPreferences | null>(preferences),
  ) {
    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: router },
        { provide: AuthService, useValue: auth },
        { provide: CurrentUserFacade, useValue: { preferences$ } },
      ],
    });
    return TestBed.runInInjectionContext(() =>
      isAccessAllowed(route(data, parentData), routerState, authentication),
    );
  }

  it('accepts a configured Janus API client role', async () => {
    await expect(
      evaluate(
        { clientRole: JANUS_CLIENT_ROLES.ADMIN },
        authData([], { 'janus-api': [JANUS_CLIENT_ROLES.ADMIN] }),
      ),
    ).resolves.toBe(true);
  });

  it('waits for the provisioned account before activating a protected page', async () => {
    const profile = new Subject<UserPreferences | null>();
    let activated = false;
    const result = evaluate({}, authData([], {}), undefined, state, profile).then((value) => {
      activated = value === true;
      return value;
    });
    await Promise.resolve();
    expect(activated).toBe(false);
    profile.next(preferences);
    await expect(result).resolves.toBe(true);
  });

  it('does not activate the page when provisioning fails', async () => {
    await expect(evaluate({}, authData([], {}), undefined, state, of(null))).resolves.toEqual({
      redirectTo: '/forbidden',
    });
  });

  it('does not activate the page when provisioning returns a network error', async () => {
    await expect(
      evaluate(
        {},
        authData([], {}),
        undefined,
        state,
        throwError(() => new Error('request failed')),
      ),
    ).resolves.toEqual({ redirectTo: '/forbidden' });
  });

  it('redirects an authenticated user without any required role to forbidden', async () => {
    await expect(
      evaluate({}, authData([], { 'janus-api': [JANUS_CLIENT_ROLES.USER] }), {
        clientRole: JANUS_CLIENT_ROLES.ADMIN,
      }),
    ).resolves.toEqual({ redirectTo: '/forbidden' });
  });

  it('allows an authenticated user using the policy on the parent route', async () => {
    await expect(
      evaluate({}, authData([], { 'janus-api': [JANUS_CLIENT_ROLES.ADMIN] }), {
        clientRole: JANUS_CLIENT_ROLES.ADMIN,
      }),
    ).resolves.toBe(true);
  });

  it.each([JANUS_CLIENT_ROLES.USER, JANUS_CLIENT_ROLES.EMPLOYEE])(
    'redirects a %s client role when the child replaces the shared policy with admin-only access',
    async (clientRole) => {
      await expect(
        evaluate(
          { clientRole: JANUS_CLIENT_ROLES.ADMIN },
          authData([], { 'janus-api': [clientRole] }),
          {
            clientRole: [
              JANUS_CLIENT_ROLES.EMPLOYEE,
              JANUS_CLIENT_ROLES.USER,
              JANUS_CLIENT_ROLES.ADMIN,
            ],
          },
        ),
      ).resolves.toEqual({ redirectTo: '/forbidden' });
    },
  );

  it('allows an admin when the child replaces the shared policy with admin-only access', async () => {
    await expect(
      evaluate(
        { clientRole: JANUS_CLIENT_ROLES.ADMIN },
        authData([], { 'janus-api': [JANUS_CLIENT_ROLES.ADMIN] }),
        {
          clientRole: [
            JANUS_CLIENT_ROLES.EMPLOYEE,
            JANUS_CLIENT_ROLES.USER,
            JANUS_CLIENT_ROLES.ADMIN,
          ],
        },
      ),
    ).resolves.toBe(true);
  });

  it('ignores realm roles for authorization', async () => {
    await expect(
      evaluate({ clientRole: JANUS_CLIENT_ROLES.ADMIN }, authData([JANUS_CLIENT_ROLES.ADMIN], {})),
    ).resolves.toEqual({ redirectTo: '/forbidden' });
  });

  it('redirects an unauthenticated user to login', async () => {
    await expect(
      evaluate(
        {},
        { ...authData([], {}), authenticated: false },
        { clientRole: JANUS_CLIENT_ROLES.ADMIN },
      ),
    ).resolves.toBe(false);
    expect(auth.login).toHaveBeenCalledWith('/protected');
  });

  it('sends an authenticated identity without verified email to the verification flow', async () => {
    auth.isUsableIdentity = () => false;
    await expect(evaluate({}, authData([], {}))).resolves.toEqual({
      commands: ['/verify-email'],
      extras: { queryParams: { returnUrl: '/protected' } },
    });
    auth.isUsableIdentity = () => true;
  });

  it('uses the root route as the login return route when the router URL is empty', async () => {
    await expect(
      evaluate({}, { ...authData([], {}), authenticated: false }, undefined, {
        url: '',
      } as RouterStateSnapshot),
    ).resolves.toBe(false);
    expect(auth.login).toHaveBeenLastCalledWith('/');
  });
});
