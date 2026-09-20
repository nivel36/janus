/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import Keycloak from 'keycloak-js';
import { KEYCLOAK_EVENT_SIGNAL, KeycloakEventType } from 'keycloak-angular';
import { describe, expect, it, vi } from 'vitest';

import {
  createDocumentMock,
  createKeycloakEventSignal,
  createKeycloakMock,
} from '../../../testing/auth-mocks';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  it('updates all derived signal values from the same event snapshot', () => {
    const keycloak = createKeycloakMock();
    const keycloakEvent = createKeycloakEventSignal();
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: Keycloak, useValue: keycloak },
        { provide: KEYCLOAK_EVENT_SIGNAL, useValue: keycloakEvent },
      ],
    });
    const service = TestBed.inject(AuthService);
    const state = () => [
      service.isAuthenticated(),
      service.username(),
      service.claims(),
      service.permissions(),
    ];

    TestBed.tick();
    expect(state()).toEqual([false, null, null, { realmRoles: [], clientRoles: {} }]);

    const authenticatedClaims = {
      preferred_username: 'ada',
      email_verified: true,
      realm_access: { roles: ['manager'] },
      resource_access: { janus: { roles: ['editor'] } },
    };
    keycloak.authenticated = true;
    keycloak.tokenParsed = authenticatedClaims;
    keycloakEvent.set({ type: KeycloakEventType.AuthSuccess });
    TestBed.tick();
    expect(state()).toEqual([
      true,
      'ada',
      authenticatedClaims,
      { realmRoles: ['manager'], clientRoles: { janus: ['editor'] } },
    ]);

    const refreshedClaims = {
      email: 'ada@example.com',
      email_verified: true,
      realm_access: { roles: ['admin'] },
      resource_access: { reporting: { roles: ['viewer'] } },
    };
    keycloak.tokenParsed = refreshedClaims;
    keycloakEvent.set({ type: KeycloakEventType.AuthRefreshSuccess });
    TestBed.tick();
    expect(state()).toEqual([
      true,
      'ada@example.com',
      refreshedClaims,
      { realmRoles: ['admin'], clientRoles: { reporting: ['viewer'] } },
    ]);

    keycloak.authenticated = false;
    keycloak.tokenParsed = undefined;
    keycloakEvent.set({ type: KeycloakEventType.AuthLogout });
    TestBed.tick();
    expect(state()).toEqual([false, null, null, { realmRoles: [], clientRoles: {} }]);
  });

  it('delegates supported login options without altering their values', async () => {
    const keycloak = createKeycloakMock();
    configureRedirectTest(keycloak, createDocumentMock());
    const service = TestBed.inject(AuthService);

    await service.login('/employees', { prompt: 'consent', maxAge: 0, idpHint: 'corporate-sso' });

    expect(keycloak.login).toHaveBeenCalledWith({
      redirectUri: 'https://janus.example/employees',
      prompt: 'consent',
      maxAge: 0,
      idpHint: 'corporate-sso',
    });
  });

  it('only treats the exact boolean email verification claim as usable', () => {
    const keycloak = createKeycloakMock({
      authenticated: true,
      tokenParsed: { email_verified: false },
    });
    configureRedirectTest(keycloak, createDocumentMock());
    const service = TestBed.inject(AuthService);

    expect(service.isAuthenticated()).toBe(false);
    expect(service.isUsableIdentity()).toBe(false);
  });

  it('forces a token refresh after verification before exposing the identity', async () => {
    const keycloak = createKeycloakMock({
      authenticated: true,
      tokenParsed: { email_verified: false },
    });
    configureRedirectTest(keycloak, createDocumentMock());
    const service = TestBed.inject(AuthService);
    vi.mocked(keycloak.updateToken).mockImplementation(async () => {
      keycloak.tokenParsed = { email_verified: true };
      return true;
    });

    await expect(service.refreshAfterEmailVerification()).resolves.toBe(true);
    expect(keycloak.updateToken).toHaveBeenCalledWith(-1);
  });

  it('builds login and logout URLs from the injected document', async () => {
    const keycloak = createKeycloakMock();
    configureRedirectTest(keycloak, createDocumentMock());
    const service = TestBed.inject(AuthService);

    await service.login('/employees');
    await service.logout();

    expect(keycloak.login).toHaveBeenCalledWith({ redirectUri: 'https://janus.example/employees' });
    expect(keycloak.logout).toHaveBeenCalledWith({ redirectUri: 'https://janus.example' });
  });

  it('omits undefined login options and redirects when the document has no default view', async () => {
    const keycloak = createKeycloakMock();
    configureRedirectTest(keycloak, { defaultView: null });
    const service = TestBed.inject(AuthService);

    await service.login('/employees', { prompt: undefined, maxAge: undefined, idpHint: undefined });
    await service.logout();

    expect(keycloak.login).toHaveBeenCalledWith({});
    expect(keycloak.logout).toHaveBeenCalledWith({ redirectUri: undefined });
  });
});

function configureRedirectTest(keycloak: ReturnType<typeof createKeycloakMock>, document: object) {
  TestBed.configureTestingModule({
    providers: [
      AuthService,
      { provide: Keycloak, useValue: keycloak },
      { provide: KEYCLOAK_EVENT_SIGNAL, useValue: createKeycloakEventSignal() },
      { provide: DOCUMENT, useValue: document },
    ],
  });
}
