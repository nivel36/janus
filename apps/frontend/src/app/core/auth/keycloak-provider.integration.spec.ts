/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { HttpRequest, HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import Keycloak from 'keycloak-js';
import {
  INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
  KEYCLOAK_EVENT_SIGNAL,
  KeycloakEventType,
  createInterceptorCondition,
  includeBearerTokenInterceptor,
  type IncludeBearerTokenCondition,
} from 'keycloak-angular';
import { combineLatest, firstValueFrom, of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';

import { createKeycloakEventSignal, createKeycloakMock } from '../../../testing/auth-mocks';
import { apiBearerUrlPattern } from '../../app.config';
import { CurrentUserFacade } from '../user/services/current-user.facade';
import { UserProfileApiService } from '../user/services/user-profile-api.service';
import { JANUS_REALM_ROLES } from './auth.models';
import { AuthService } from './auth.service';

describe('Keycloak event integration with the current-user facade', () => {
  it('keeps reactive and imperative role selectors aligned through login, refresh, and logout', () => {
    const keycloak = createKeycloakMock({
      // Deliberately stale adapter answers prove that consumers use the event snapshot instead.
      hasRealmRole: vi.fn().mockReturnValue(true),
      hasResourceRole: vi.fn().mockReturnValue(true),
    });
    const keycloakEvent = createKeycloakEventSignal();
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
