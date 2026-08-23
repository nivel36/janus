/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { HttpRequest, HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router, type ActivatedRouteSnapshot, type RouterStateSnapshot } from '@angular/router';
import Keycloak from 'keycloak-js';
import {
  INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
  createInterceptorCondition,
  includeBearerTokenInterceptor,
  type AuthGuardData,
  type IncludeBearerTokenCondition,
} from 'keycloak-angular';
import { firstValueFrom, of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';

import { apiBearerUrlPattern } from '../../app.config';
import { isAccessAllowed } from './auth.guard';

function requestHandledByOfficialInterceptor(url: string) {
  const keycloak = {
    authenticated: true,
    token: 'access-token',
    updateToken: vi.fn().mockResolvedValue(false),
  };
  TestBed.configureTestingModule({
    providers: [
      { provide: Keycloak, useValue: keycloak },
      {
        provide: INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
        useValue: [
          createInterceptorCondition<IncludeBearerTokenCondition>({
            urlPattern: apiBearerUrlPattern('/api', window.location.origin),
          }),
        ],
      },
    ],
  });

  let forwarded: HttpRequest<unknown> | undefined;
  const response = TestBed.runInInjectionContext(() =>
    includeBearerTokenInterceptor(new HttpRequest('GET', url), (request) => {
      forwarded = request;
      return of(new HttpResponse({ status: 200 }));
    }),
  );

  return { keycloak, forwarded: () => forwarded, response };
}

describe('official bearer interceptor', () => {
  it('includes the token for API requests', async () => {
    const handled = requestHandledByOfficialInterceptor('/api/worksites');
    await firstValueFrom(handled.response);

    expect(handled.forwarded()?.headers.get('Authorization')).toBe('Bearer access-token');
    expect(handled.keycloak.updateToken).toHaveBeenCalledOnce();
  });

  it('excludes the token and refresh for non-API requests', async () => {
    const handled = requestHandledByOfficialInterceptor('https://third-party.example/resource');
    await firstValueFrom(handled.response);

    expect(handled.forwarded()?.headers.has('Authorization')).toBe(false);
    expect(handled.keycloak.updateToken).not.toHaveBeenCalled();
  });

  it('allows concurrent requests to use the adapter refresh queue', async () => {
    const first = requestHandledByOfficialInterceptor('/api/first');
    const second = TestBed.runInInjectionContext(() =>
      includeBearerTokenInterceptor(new HttpRequest('GET', '/api/second'), (request) =>
        of(new HttpResponse({ status: request.headers.has('Authorization') ? 200 : 500 })),
      ),
    );

    const responses = await Promise.all([firstValueFrom(first.response), firstValueFrom(second)]);
    expect(responses.map((response) => (response as HttpResponse<unknown>).status)).toEqual([
      200, 200,
    ]);
    expect(first.keycloak.updateToken).toHaveBeenCalledTimes(2);
  });

  it('does not block a request when the adapter reports a refresh failure', async () => {
    const handled = requestHandledByOfficialInterceptor('/api/worksites');
    handled.keycloak.updateToken.mockRejectedValueOnce(new Error('refresh failed'));

    await expect(firstValueFrom(handled.response)).resolves.toMatchObject({ status: 200 });
  });
});

describe('Keycloak Angular guard', () => {
  const route = (data: Record<string, unknown>) => ({ data }) as unknown as ActivatedRouteSnapshot;
  const state = { url: '/protected' } as RouterStateSnapshot;
  const keycloak = { login: vi.fn().mockResolvedValue(undefined) };
  const router = { parseUrl: vi.fn((url: string) => ({ redirectTo: url })) };

  const authData = (
    realmRoles: string[],
    resourceRoles: Record<string, string[]>,
  ): AuthGuardData => ({
    authenticated: true,
    grantedRoles: { realmRoles, resourceRoles },
    keycloak: keycloak as unknown as Keycloak,
  });

  async function evaluate(data: Record<string, unknown>, authentication: AuthGuardData) {
    TestBed.configureTestingModule({ providers: [{ provide: Router, useValue: router }] });
    return TestBed.runInInjectionContext(() => isAccessAllowed(route(data), state, authentication));
  }

  it('accepts either a realm role or a client role', async () => {
    await expect(evaluate({ realmRole: 'ADMIN' }, authData(['ADMIN'], {}))).resolves.toBe(true);
    TestBed.resetTestingModule();
    await expect(
      evaluate(
        { clientRole: { clientId: 'janus', role: 'editor' } },
        authData([], { janus: ['editor'] }),
      ),
    ).resolves.toBe(true);
  });

  it('redirects an authenticated user without any required role to forbidden', async () => {
    await expect(evaluate({ realmRole: 'ADMIN' }, authData(['USER'], {}))).resolves.toEqual({
      redirectTo: '/forbidden',
    });
  });
});
