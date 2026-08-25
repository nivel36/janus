/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { effect, inject, Injectable, type Signal } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import Keycloak from 'keycloak-js';
import { KEYCLOAK_EVENT_SIGNAL } from 'keycloak-angular';
import { resolveKeycloakLocale } from './keycloak-locale';

interface KeycloakClaims {
  sub?: string;
  iss?: string;
  aud?: string | string[];
  exp?: number;
  iat?: number;
  auth_time?: number;
  session_state?: string;
  azp?: string;
  typ?: string;
  preferred_username?: string;
  email?: string;
  email_verified?: boolean;
  name?: string;
  given_name?: string;
  family_name?: string;
  locale?: string;
  realm_access?: {
    roles?: string[];
  };
  resource_access?: Record<string, { roles?: string[] }>;
}

interface PermissionState {
  realmRoles: string[];
  clientRoles: Record<string, string[]>;
}

interface LoginRedirectOptions {
  prompt?: 'none' | 'login' | 'consent';
  maxAge?: number;
  idpHint?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly keycloak = inject(Keycloak);
  private readonly keycloakEvent = inject(KEYCLOAK_EVENT_SIGNAL) as Signal<unknown>;
  private readonly isAuthenticatedSubject = new BehaviorSubject<boolean>(
    Boolean(this.keycloak.authenticated),
  );
  private readonly usernameSubject = new BehaviorSubject<string | null>(
    this.getUsernameFromClaims(),
  );
  private readonly claimsSubject = new BehaviorSubject<KeycloakClaims | null>(this.getClaims());
  private readonly permissionsSubject = new BehaviorSubject<PermissionState>({
    realmRoles: [],
    clientRoles: {},
  });

  readonly isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
  readonly username$ = this.usernameSubject.asObservable();
  readonly claims$ = this.claimsSubject.asObservable();
  readonly permissions$ = this.permissionsSubject.asObservable();

  constructor() {
    effect(() => {
      this.keycloakEvent();
      this.syncAuthState();
    });
  }

  login(): Promise<void> {
    return this.keycloak.login({ locale: resolveKeycloakLocale() });
  }

  loginWithRedirect(redirectUri?: string, options?: LoginRedirectOptions): Promise<void> {
    const resolvedRedirectUri = redirectUri
      ? new URL(redirectUri, window.location.origin).toString()
      : window.location.href;

    return this.keycloak.login({
      redirectUri: resolvedRedirectUri,
      prompt: options?.prompt,
      maxAge: options?.maxAge,
      idpHint: options?.idpHint,
      locale: resolveKeycloakLocale(),
    });
  }

  logout(): Promise<void> {
    return this.keycloak.logout({ redirectUri: globalThis.location?.origin });
  }

  getToken(): string | null {
    return this.keycloak.token ?? null;
  }

  getClaims(): KeycloakClaims | null {
    if (!this.keycloak.tokenParsed) {
      return null;
    }

    return this.keycloak.tokenParsed as KeycloakClaims;
  }

  hasRealmRole(role: string): boolean {
    return this.keycloak.hasRealmRole(role);
  }

  hasClientRole(clientId: string, role: string): boolean {
    return this.keycloak.hasResourceRole(role, clientId);
  }

  private syncAuthState(): void {
    const isAuthenticated = Boolean(this.keycloak.authenticated);
    this.isAuthenticatedSubject.next(isAuthenticated);
    const claims = this.getClaims();
    this.claimsSubject.next(claims);
    this.permissionsSubject.next(this.extractPermissions(claims));

    if (!isAuthenticated) {
      this.usernameSubject.next(null);
      return;
    }

    const username = this.getUsernameFromClaims();
    this.usernameSubject.next(username);

    if (!username) {
      return;
    }
  }

  private getUsernameFromClaims(): string | null {
    const claims = this.getClaims();
    return claims?.preferred_username ?? claims?.email ?? null;
  }

  private extractPermissions(claims: KeycloakClaims | null): PermissionState {
    const realmRoles = claims?.realm_access?.roles ?? [];
    const clientRoles = Object.entries(claims?.resource_access ?? {}).reduce<
      Record<string, string[]>
    >((accumulator, [clientId, access]) => {
      accumulator[clientId] = access.roles ?? [];
      return accumulator;
    }, {});

    return { realmRoles, clientRoles };
  }
}
