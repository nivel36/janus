/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { TestBed } from '@angular/core/testing';
import { Router, type ActivatedRouteSnapshot, type RouterStateSnapshot } from '@angular/router';
import Keycloak from 'keycloak-js';
import type { AuthGuardData } from 'keycloak-angular';
import { describe, expect, it, vi } from 'vitest';

import { isAccessAllowed } from './auth.guard';
import { JANUS_CLIENT_ROLES } from './auth.models';
import { AuthService } from './auth.service';

describe('isAccessAllowed', () => {
  const route = (data: Record<string, unknown>, parentData?: Record<string, unknown>) =>
    ({
      data,
      pathFromRoot: [...(parentData ? [{ data: parentData }] : []), { data }],
    }) as unknown as ActivatedRouteSnapshot;
  const state = { url: '/protected' } as RouterStateSnapshot;
  const keycloak = { login: vi.fn().mockResolvedValue(undefined) };
  const router = { parseUrl: vi.fn((url: string) => ({ redirectTo: url })) };
  const auth = { login: vi.fn().mockResolvedValue(undefined) };

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
  ) {
    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: router },
        { provide: AuthService, useValue: auth },
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

  it('redirects an authenticated user without any required role to forbidden', async () => {
    await expect(
      evaluate(
        {},
        authData([], { 'janus-api': [JANUS_CLIENT_ROLES.USER] }),
        { clientRole: JANUS_CLIENT_ROLES.ADMIN },
      ),
    ).resolves.toEqual({ redirectTo: '/forbidden' });
  });

  it('allows an authenticated user using the policy on the parent route', async () => {
    await expect(
      evaluate(
        {},
        authData([], { 'janus-api': [JANUS_CLIENT_ROLES.ADMIN] }),
        { clientRole: JANUS_CLIENT_ROLES.ADMIN },
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

  it('uses the root route as the login return route when the router URL is empty', async () => {
    await expect(
      evaluate({}, { ...authData([], {}), authenticated: false }, undefined, {
        url: '',
      } as RouterStateSnapshot),
    ).resolves.toBe(false);
    expect(auth.login).toHaveBeenLastCalledWith('/');
  });
});
