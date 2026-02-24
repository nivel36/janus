import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, defer, from, map, throwError } from 'rxjs';

import { keycloak } from '../../auth/keycloak';

interface AppUserResponse {
	username: string;
	name: string;
	surname: string;
	locale: string;
	timeFormat: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
	private readonly tokenSubject = new BehaviorSubject<string | null>(keycloak?.token ?? null);
	private readonly usernameSubject = new BehaviorSubject<string | null>(this.extractUsername());
	private readonly appUserSubject = new BehaviorSubject<AppUserResponse | null>(null);

	readonly isAuthenticated$ = this.tokenSubject.pipe(map((token) => !!token));
	readonly username$ = this.usernameSubject.asObservable();
	readonly appUser$ = this.appUserSubject.asObservable();

	login(): Observable<void> {
		return defer(() => {
			if (!keycloak) {
				return throwError(() => new Error('Keycloak is not configured.'));
			}

			return from(keycloak.login()).pipe(map(() => undefined));
		});
	}

	getToken(): string | null {
		return keycloak?.token ?? this.tokenSubject.value;
	}

	clearToken(): void {
		this.tokenSubject.next(null);
		this.usernameSubject.next(null);
		this.appUserSubject.next(null);
	}

	private extractUsername(): string | null {
		if (!keycloak?.tokenParsed) {
			return null;
		}

		const preferredUsername = keycloak.tokenParsed['preferred_username'];
		const email = keycloak.tokenParsed['email'];

		if (typeof preferredUsername === 'string' && preferredUsername.length > 0) {
			return preferredUsername;
		}

		if (typeof email === 'string' && email.length > 0) {
			return email;
		}

		return null;
	}
}
