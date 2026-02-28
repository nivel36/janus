import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { keycloak } from '../../auth/keycloak';

interface AppUserResponse {
  username: string;
  name: string;
  surname: string;
  locale: string;
  timeFormat: string;
}

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
  private readonly appUserBaseUrl = `${environment.apiUrl}/appusers`;
  private readonly isAuthenticatedSubject = new BehaviorSubject<boolean>(
    Boolean(keycloak?.authenticated),
  );
  private readonly usernameSubject = new BehaviorSubject<string | null>(
    this.getUsernameFromClaims(),
  );
  private readonly appUserSubject = new BehaviorSubject<AppUserResponse | null>(null);
  private readonly claimsSubject = new BehaviorSubject<KeycloakClaims | null>(this.getClaims());
  private readonly permissionsSubject = new BehaviorSubject<PermissionState>({
    realmRoles: [],
    clientRoles: {},
  });

  readonly isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
  readonly username$ = this.usernameSubject.asObservable();
  readonly appUser$ = this.appUserSubject.asObservable();
  readonly claims$ = this.claimsSubject.asObservable();
  readonly permissions$ = this.permissionsSubject.asObservable();

  constructor(private readonly http: HttpClient) {
    this.bindKeycloakEvents();
    this.syncAuthState();
  }

  login(): Promise<void> {
    if (!keycloak) {
      return Promise.reject(new Error('Keycloak is not configured'));
    }
    return keycloak.login();
  }

  loginWithRedirect(redirectUri?: string, options?: LoginRedirectOptions): Promise<void> {
    if (!keycloak) {
      return Promise.reject(new Error('Keycloak is not configured'));
    }

    const resolvedRedirectUri = redirectUri
      ? new URL(redirectUri, window.location.origin).toString()
      : window.location.href;

    return keycloak.login({
      redirectUri: resolvedRedirectUri,
      prompt: options?.prompt,
      maxAge: options?.maxAge,
      idpHint: options?.idpHint,
    });
  }

  logout(): Promise<void> {
    if (!keycloak) {
      this.syncAuthState();
      return Promise.resolve();
    }
    return keycloak.logout();
  }

  clearToken(): void {
    void this.logout();
  }

  getToken(): string | null {
    return keycloak?.token ?? null;
  }

  getClaims(): KeycloakClaims | null {
    if (!keycloak?.tokenParsed) {
      return null;
    }

    return keycloak.tokenParsed as KeycloakClaims;
  }

  hasRealmRole(role: string): boolean {
    return this.permissionsSubject.value.realmRoles.includes(role);
  }

  hasClientRole(clientId: string, role: string): boolean {
    return this.permissionsSubject.value.clientRoles[clientId]?.includes(role) ?? false;
  }

  private bindKeycloakEvents(): void {
    if (!keycloak) {
      return;
    }

    keycloak.onAuthSuccess = () => this.syncAuthState();
    keycloak.onAuthRefreshSuccess = () => this.syncAuthState();
    keycloak.onAuthLogout = () => this.syncAuthState();
  }

  private syncAuthState(): void {
    const isAuthenticated = Boolean(keycloak?.authenticated);
    this.isAuthenticatedSubject.next(isAuthenticated);
    const claims = this.getClaims();
    this.claimsSubject.next(claims);
    this.permissionsSubject.next(this.extractPermissions(claims));

    if (!isAuthenticated) {
      this.usernameSubject.next(null);
      this.appUserSubject.next(null);
      return;
    }

    const username = this.getUsernameFromClaims();
    this.usernameSubject.next(username);

    if (!username) {
      this.appUserSubject.next(null);
      return;
    }

    this.fetchAppUser(username);
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

  private fetchAppUser(username: string): void {
    this.http
      .get<AppUserResponse>(`${this.appUserBaseUrl}/${encodeURIComponent(username)}`)
      .subscribe({
        next: (appUser) => this.appUserSubject.next(appUser),
        error: () => this.appUserSubject.next(null),
      });
  }
}
