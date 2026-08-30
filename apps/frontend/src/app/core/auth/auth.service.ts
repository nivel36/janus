/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { computed, effect, inject, Injectable, signal, type Signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import Keycloak, { type KeycloakLoginOptions } from 'keycloak-js';
import { KEYCLOAK_EVENT_SIGNAL } from 'keycloak-angular';
import { AuthRedirectService } from './auth-redirect.service';
import type { ApplicationTokenClaims, ClientRolesByClient, PermissionState } from './auth.models';

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

  login(
    returnRoute?: string,
    options?: Pick<KeycloakLoginOptions, 'prompt' | 'maxAge' | 'idpHint'>,
  ): Promise<void> {
    const redirectUri = this.redirects.loginRedirectUri(returnRoute);

    return this.keycloak.login({
      ...(redirectUri !== undefined ? { redirectUri } : {}),
      ...(options?.prompt !== undefined ? { prompt: options.prompt } : {}),
      ...(options?.maxAge !== undefined ? { maxAge: options.maxAge } : {}),
      ...(options?.idpHint !== undefined ? { idpHint: options.idpHint } : {}),
    });
  }

  logout(): Promise<void> {
    return this.keycloak.logout({ redirectUri: this.redirects.logoutRedirectUri() });
  }

  getToken(): string | null {
    return this.keycloak.token ?? null;
  }

  getClaims(): ApplicationTokenClaims | null {
    if (!this.keycloak.tokenParsed) {
      return null;
    }

    return this.keycloak.tokenParsed;
  }

  hasRealmRole(role: string): boolean {
    return this.keycloak.hasRealmRole(role);
  }

  hasClientRole(clientId: string, role: string): boolean {
    return this.keycloak.hasResourceRole(role, clientId);
  }

  private readKeycloakSnapshot(): {
    isAuthenticated: boolean;
    claims: ApplicationTokenClaims | null;
  } {
    return {
      isAuthenticated: Boolean(this.keycloak.authenticated),
      claims: this.getClaims(),
    };
  }

  private extractPermissions(claims: ApplicationTokenClaims | null): PermissionState {
    const realmRoles = claims?.realm_access?.roles ?? [];
    const clientRoles: ClientRolesByClient = Object.fromEntries(
      Object.entries(claims?.resource_access ?? {}).map(([clientId, access]) => [
        clientId,
        access.roles ?? [],
      ]),
    );

    return { realmRoles, clientRoles };
  }
}
