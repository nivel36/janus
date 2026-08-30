/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import Keycloak from 'keycloak-js';
import { KEYCLOAK_EVENT_SIGNAL, KeycloakEventType } from 'keycloak-angular';
import { combineLatest } from 'rxjs';
import { describe, expect, it } from 'vitest';

import {
  createDocumentMock,
  createKeycloakEventSignal,
  createKeycloakMock,
} from '../../../testing/auth-mocks';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  it('updates all signal-derived observable values from the same event snapshot', () => {
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
