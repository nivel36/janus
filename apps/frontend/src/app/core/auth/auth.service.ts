/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { computed, effect, inject, Injectable, signal, type Signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import Keycloak from 'keycloak-js';
import { KEYCLOAK_EVENT_SIGNAL } from 'keycloak-angular';
import { resolveKeycloakLocale } from './keycloak-locale';
import { AuthRedirectService } from './auth-redirect.service';

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
  private readonly redirects = inject(AuthRedirectService);
  private readonly keycloakSnapshot = signal(this.readKeycloakSnapshot());

  readonly isAuthenticated = computed(() => this.keycloakSnapshot().isAuthenticated);
  readonly claims = computed(() => this.keycloakSnapshot().claims);
  readonly username = computed(() => {
    if (!this.isAuthenticated()) {
      return null;
    }

    const claims = this.claims();
    return claims?.preferred_username ?? claims?.email ?? null;
  });
  readonly permissions = computed(() => this.extractPermissions(this.claims()));

  readonly isAuthenticated$ = toObservable(this.isAuthenticated);
  readonly username$ = toObservable(this.username);
  readonly claims$ = toObservable(this.claims);
  readonly permissions$ = toObservable(this.permissions);

  constructor() {
    effect(() => {
      this.keycloakEvent();
      this.keycloakSnapshot.set(this.readKeycloakSnapshot());
    });
  }

  login(): Promise<void> {
    return this.keycloak.login({ locale: resolveKeycloakLocale() });
  }

  loginWithRedirect(redirectUri?: string, options?: LoginRedirectOptions): Promise<void> {
    return this.keycloak.login({
      redirectUri: this.redirects.loginRedirectUri(redirectUri),
      prompt: options?.prompt,
      maxAge: options?.maxAge,
      idpHint: options?.idpHint,
      locale: resolveKeycloakLocale(),
    });
  }

  logout(): Promise<void> {
    return this.keycloak.logout({ redirectUri: this.redirects.logoutRedirectUri() });
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

  private readKeycloakSnapshot(): {
    isAuthenticated: boolean;
    claims: KeycloakClaims | null;
  } {
    return {
      isAuthenticated: Boolean(this.keycloak.authenticated),
      claims: this.getClaims(),
    };
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
