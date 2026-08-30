/**
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  computed,
  effect,
  inject,
  Injectable,
  signal,
  type Signal,
  type WritableSignal,
} from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import Keycloak, { type KeycloakLoginOptions } from 'keycloak-js';
import { KEYCLOAK_EVENT_SIGNAL } from 'keycloak-angular';
import type { Observable } from 'rxjs';
import { AuthRedirectService } from './auth-redirect.service';
import type { AuthPermissions, AuthTokenClaims, ClientRolesByClient } from './auth.models';

interface KeycloakSnapshot {
  readonly isAuthenticated: boolean;
  readonly claims: AuthTokenClaims | null;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly keycloak = inject(Keycloak);
  private readonly keycloakEvent = inject(KEYCLOAK_EVENT_SIGNAL) as Signal<unknown>;
  private readonly redirects = inject(AuthRedirectService);
  private readonly keycloakSnapshot: WritableSignal<KeycloakSnapshot> = signal(
    this.readKeycloakSnapshot(),
  );

  readonly isAuthenticated: Signal<boolean> = computed(
    () => this.keycloakSnapshot().isAuthenticated,
  );
  readonly claims: Signal<AuthTokenClaims | null> = computed(() => this.keycloakSnapshot().claims);
  readonly username: Signal<string | null> = computed(() => {
    if (!this.isAuthenticated()) {
      return null;
    }

    const claims = this.claims();
    return claims?.preferred_username ?? claims?.email ?? null;
  });
  readonly permissions: Signal<AuthPermissions> = computed(() =>
    this.extractPermissions(this.claims()),
  );

  readonly isAuthenticated$: Observable<boolean> = toObservable(this.isAuthenticated);
  readonly username$: Observable<string | null> = toObservable(this.username);
  readonly claims$: Observable<AuthTokenClaims | null> = toObservable(this.claims);
  readonly permissions$: Observable<AuthPermissions> = toObservable(this.permissions);

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

  getClaims(): AuthTokenClaims | null {
    return this.claims();
  }

  hasRealmRole(role: string): boolean {
    return this.permissions().realmRoles.includes(role);
  }

  hasClientRole(clientId: string, role: string): boolean {
    return this.permissions().clientRoles[clientId]?.includes(role) ?? false;
  }

  private readKeycloakSnapshot(): KeycloakSnapshot {
    return {
      isAuthenticated: Boolean(this.keycloak.authenticated),
      claims: this.keycloak.tokenParsed ?? null,
    };
  }

  private extractPermissions(claims: AuthTokenClaims | null): AuthPermissions {
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
