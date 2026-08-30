/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { DOCUMENT } from '@angular/common';
import { HttpErrorResponse, HttpRequest, HttpResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, type ActivatedRouteSnapshot, type RouterStateSnapshot } from '@angular/router';
import Keycloak from 'keycloak-js';
import {
  INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
  KEYCLOAK_EVENT_SIGNAL,
  KeycloakEventType,
  createInterceptorCondition,
  includeBearerTokenInterceptor,
  type AuthGuardData,
  type IncludeBearerTokenCondition,
} from 'keycloak-angular';
import { combineLatest, firstValueFrom, of, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';

import { apiBearerUrlPattern } from '../../app.config';
import { authErrorInterceptor } from './auth-error.interceptor';
import { isAccessAllowed } from './auth.guard';
import { JANUS_REALM_ROLES, type AuthTokenClaims } from './auth.models';
import { AuthService } from './auth.service';
import { CurrentUserFacade } from '../user/services/current-user.facade';
import { UserProfileApiService } from '../user/services/user-profile-api.service';

describe('AuthService Keycloak event state', () => {
  it('updates all signal-derived observable values from the same event snapshot', () => {
    const keycloak = {
      authenticated: false,
      tokenParsed: undefined as AuthTokenClaims | undefined,
      login: vi.fn(),
      logout: vi.fn(),
      hasRealmRole: vi.fn(),
      hasResourceRole: vi.fn(),
    };
    const keycloakEvent = signal({ type: KeycloakEventType.KeycloakAngularNotInitialized });
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: Keycloak, useValue: keycloak },
        { provide: KEYCLOAK_EVENT_SIGNAL, useValue: keycloakEvent },
      ],
    });
    const service = TestBed.inject(AuthService);
    const observedStates: unknown[][] = [];
    const subscription = combineLatest([
      service.isAuthenticated$,
      service.username$,
      service.claims$,
      service.permissions$,
    ]).subscribe((state) => observedStates.push(state));

    TestBed.tick();
    expect(observedStates.at(-1)).toEqual([false, null, null, { realmRoles: [], clientRoles: {} }]);

    const authenticatedClaims = {
      preferred_username: 'ada',
      realm_access: { roles: ['manager'] },
      resource_access: { janus: { roles: ['editor'] } },
    };
    keycloak.authenticated = true;
    keycloak.tokenParsed = authenticatedClaims;
    keycloakEvent.set({ type: KeycloakEventType.AuthSuccess });
    TestBed.tick();

    expect(observedStates.at(-1)).toEqual([
      true,
      'ada',
      authenticatedClaims,
      { realmRoles: ['manager'], clientRoles: { janus: ['editor'] } },
    ]);

    const refreshedClaims = {
      email: 'ada@example.com',
      realm_access: { roles: ['admin'] },
      resource_access: { reporting: { roles: ['viewer'] } },
    };
    keycloak.tokenParsed = refreshedClaims;
    keycloakEvent.set({ type: KeycloakEventType.AuthRefreshSuccess });
    TestBed.tick();

    expect(observedStates.at(-1)).toEqual([
      true,
      'ada@example.com',
      refreshedClaims,
      { realmRoles: ['admin'], clientRoles: { reporting: ['viewer'] } },
    ]);

    keycloak.authenticated = false;
    keycloak.tokenParsed = undefined;
    keycloakEvent.set({ type: KeycloakEventType.AuthLogout });
    TestBed.tick();

    expect(observedStates.at(-1)).toEqual([false, null, null, { realmRoles: [], clientRoles: {} }]);
    subscription.unsubscribe();
  });

  it('keeps reactive and imperative role selectors aligned through login, refresh, and logout', () => {
    const keycloak = {
      authenticated: false,
      tokenParsed: undefined as AuthTokenClaims | undefined,
      login: vi.fn(),
      logout: vi.fn(),
      // Deliberately stale adapter answers prove that consumers use the event snapshot instead.
      hasRealmRole: vi.fn().mockReturnValue(true),
      hasResourceRole: vi.fn().mockReturnValue(true),
    };
    const keycloakEvent = signal({ type: KeycloakEventType.KeycloakAngularNotInitialized });
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        CurrentUserFacade,
        { provide: Keycloak, useValue: keycloak },
        { provide: KEYCLOAK_EVENT_SIGNAL, useValue: keycloakEvent },
        {
          provide: UserProfileApiService,
          useValue: {
            getPreferences: vi.fn().mockReturnValue(of(null)),
            updatePreferences: vi.fn(),
          },
        },
      ],
    });
    const auth = TestBed.inject(AuthService);
    const currentUser = TestBed.inject(CurrentUserFacade);
    const reactiveRoles: boolean[][] = [];
    const subscription = combineLatest([
      currentUser.isAdmin$,
      currentUser.isUser$,
      currentUser.isEmployee$,
    ]).subscribe((roles) => reactiveRoles.push(roles));
    const expectSelectors = (expected: boolean[]) => {
      TestBed.tick();
      expect(reactiveRoles.at(-1)).toEqual(expected);
      expect([currentUser.isAdmin(), currentUser.isUser(), currentUser.isEmployee()]).toEqual(
        expected,
      );
    };

    expectSelectors([false, false, false]);
    expect(auth.hasClientRole('janus', 'editor')).toBe(false);

    keycloak.authenticated = true;
    keycloak.tokenParsed = {
      preferred_username: 'ada',
      realm_access: { roles: [JANUS_REALM_ROLES.ADMIN, JANUS_REALM_ROLES.USER] },
      resource_access: { janus: { roles: ['editor'] } },
    };
    keycloakEvent.set({ type: KeycloakEventType.AuthSuccess });
    expectSelectors([true, true, false]);
    expect(auth.hasClientRole('janus', 'editor')).toBe(true);

    keycloak.tokenParsed = {
      preferred_username: 'ada',
      realm_access: { roles: [JANUS_REALM_ROLES.EMPLOYEE] },
      resource_access: { janus: { roles: ['viewer'] } },
    };
    keycloakEvent.set({ type: KeycloakEventType.AuthRefreshSuccess });
    expectSelectors([false, false, true]);
    expect(auth.hasClientRole('janus', 'editor')).toBe(false);
    expect(auth.hasClientRole('janus', 'viewer')).toBe(true);

    keycloak.authenticated = false;
    keycloak.tokenParsed = undefined;
    keycloakEvent.set({ type: KeycloakEventType.AuthLogout });
    expectSelectors([false, false, false]);
    expect(auth.hasClientRole('janus', 'viewer')).toBe(false);
    expect(keycloak.hasRealmRole).not.toHaveBeenCalled();
    expect(keycloak.hasResourceRole).not.toHaveBeenCalled();
    subscription.unsubscribe();
  });
});

describe('AuthService redirects', () => {
  it('delegates supported login options without altering their values', async () => {
    const keycloak = {
      login: vi.fn().mockResolvedValue(undefined),
      logout: vi.fn(),
      hasRealmRole: vi.fn(),
      hasResourceRole: vi.fn(),
    };
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: Keycloak, useValue: keycloak },
        { provide: KEYCLOAK_EVENT_SIGNAL, useValue: signal({}) },
        {
          provide: DOCUMENT,
          useValue: { defaultView: { location: new URL('https://janus.example/current') } },
        },
      ],
    });
    const service = TestBed.inject(AuthService);

    await service.login('/employees', {
      prompt: 'consent',
      maxAge: 0,
      idpHint: 'corporate-sso',
    });

    expect(keycloak.login).toHaveBeenCalledWith({
      redirectUri: 'https://janus.example/employees',
      prompt: 'consent',
      maxAge: 0,
      idpHint: 'corporate-sso',
    });
  });

  it('builds login and logout URLs from the injected document', async () => {
    const keycloak = {
      login: vi.fn().mockResolvedValue(undefined),
      logout: vi.fn().mockResolvedValue(undefined),
      hasRealmRole: vi.fn(),
      hasResourceRole: vi.fn(),
    };
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: Keycloak, useValue: keycloak },
        { provide: KEYCLOAK_EVENT_SIGNAL, useValue: signal({}) },
        {
          provide: DOCUMENT,
          useValue: { defaultView: { location: new URL('https://janus.example/current') } },
        },
      ],
    });
    const service = TestBed.inject(AuthService);

    await service.login('/employees');
    await service.logout();

    expect(keycloak.login).toHaveBeenCalledWith({
      redirectUri: 'https://janus.example/employees',
    });
    expect(keycloak.logout).toHaveBeenCalledWith({ redirectUri: 'https://janus.example' });
  });

  it('omits undefined login options and redirects when the document has no default view', async () => {
    const keycloak = {
      login: vi.fn().mockResolvedValue(undefined),
      logout: vi.fn().mockResolvedValue(undefined),
      hasRealmRole: vi.fn(),
      hasResourceRole: vi.fn(),
    };
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: Keycloak, useValue: keycloak },
        { provide: KEYCLOAK_EVENT_SIGNAL, useValue: signal({}) },
        { provide: DOCUMENT, useValue: { defaultView: null } },
      ],
    });
    const service = TestBed.inject(AuthService);

    await service.login('/employees', { prompt: undefined, maxAge: undefined, idpHint: undefined });
    await service.logout();

    expect(keycloak.login).toHaveBeenCalledWith({});
    expect(keycloak.logout).toHaveBeenCalledWith({ redirectUri: undefined });
  });
});

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
            urlPattern: apiBearerUrlPattern('/api', 'http://localhost:3000'),
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

describe('authentication error recovery', () => {
  it('clears the unusable token and redirects to login after an API 401', async () => {
    const keycloak = { clearToken: vi.fn() };
    const auth = { login: vi.fn().mockResolvedValue(undefined) };
    TestBed.configureTestingModule({
      providers: [
        { provide: Keycloak, useValue: keycloak },
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: { navigateByUrl: vi.fn() } },
        {
          provide: DOCUMENT,
          useValue: { defaultView: { location: new URL('https://janus.example/current') } },
        },
      ],
    });
    const unauthorized = new HttpErrorResponse({ status: 401, url: '/api/worksites' });
    const response = TestBed.runInInjectionContext(() =>
      authErrorInterceptor(new HttpRequest('GET', '/api/worksites'), () =>
        throwError(() => unauthorized),
      ),
    );

    await expect(firstValueFrom(response)).rejects.toBe(unauthorized);
    expect(keycloak.clearToken).toHaveBeenCalledOnce();
    expect(auth.login).toHaveBeenCalledWith();
  });
});

describe('Keycloak Angular guard', () => {
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

  it('accepts a configured realm role', async () => {
    await expect(
      evaluate({ realmRole: JANUS_REALM_ROLES.ADMIN }, authData([JANUS_REALM_ROLES.ADMIN], {})),
    ).resolves.toBe(true);
  });

  it('redirects an authenticated user without any required role to forbidden', async () => {
    await expect(
      evaluate({}, authData([JANUS_REALM_ROLES.USER], {}), { realmRole: JANUS_REALM_ROLES.ADMIN }),
    ).resolves.toEqual({ redirectTo: '/forbidden' });
  });

  it('allows an authenticated user using the policy on the parent route', async () => {
    await expect(
      evaluate({}, authData([JANUS_REALM_ROLES.ADMIN], {}), { realmRole: JANUS_REALM_ROLES.ADMIN }),
    ).resolves.toBe(true);
  });

  it('redirects an unauthenticated user to login', async () => {
    const authentication = {
      ...authData([], {}),
      authenticated: false,
    };

    await expect(
      evaluate({}, authentication, { realmRole: JANUS_REALM_ROLES.ADMIN }),
    ).resolves.toBe(false);
    expect(auth.login).toHaveBeenCalledWith('/protected');
  });

  it('uses the root route as the login return route when the router URL is empty', async () => {
    const authentication = { ...authData([], {}), authenticated: false };

    await expect(
      evaluate({}, authentication, undefined, { url: '' } as RouterStateSnapshot),
    ).resolves.toBe(false);
    expect(auth.login).toHaveBeenLastCalledWith('/');
  });
});
