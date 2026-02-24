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
	preferred_username?: string;
	email?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
	private readonly appUserBaseUrl = `${environment.apiUrl}/appusers`;
	private readonly isAuthenticatedSubject = new BehaviorSubject<boolean>(Boolean(keycloak?.authenticated));
	private readonly usernameSubject = new BehaviorSubject<string | null>(this.getUsernameFromClaims());
	private readonly appUserSubject = new BehaviorSubject<AppUserResponse | null>(null);

	readonly isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
	readonly username$ = this.usernameSubject.asObservable();
	readonly appUser$ = this.appUserSubject.asObservable();

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
		const claims = keycloak?.tokenParsed as KeycloakClaims | undefined;
		return claims?.preferred_username ?? claims?.email ?? null;
	}

	private fetchAppUser(username: string): void {
		this.http
			.get<AppUserResponse>(`${this.appUserBaseUrl}/${encodeURIComponent(username)}`)
			.subscribe({
				next: (appUser) => this.appUserSubject.next(appUser),
				error: () => this.appUserSubject.next(null)
			});
	}
}
