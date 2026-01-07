import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, catchError, map, Observable, switchMap, tap, throwError } from 'rxjs';

interface LoginResponse {
	token: string;
	username: string;
}

interface AppUserResponse {
	username: string;
	name: string;
	surname: string;
	locale: string;
	timeFormat: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
	private readonly baseUrl = '/api/v1/auth';
	private readonly appUserBaseUrl = '/api/v1/appusers';
	private readonly tokenStorageKey = 'janus.auth.token';
	private readonly usernameStorageKey = 'janus.auth.username';
	private readonly appUserStorageKey = 'janus.auth.user';
	private readonly tokenSubject = new BehaviorSubject<string | null>(this.loadToken());
	private readonly usernameSubject = new BehaviorSubject<string | null>(this.loadUsername());
	private readonly appUserSubject = new BehaviorSubject<AppUserResponse | null>(this.loadAppUser());
	readonly isAuthenticated$ = this.tokenSubject.pipe(map((token) => !!token));
	readonly username$ = this.usernameSubject.asObservable();
	readonly appUser$ = this.appUserSubject.asObservable();

	constructor(private readonly http: HttpClient) {}

	login(username: string, password: string): Observable<void> {
		return this.http
			.post<LoginResponse>(`${this.baseUrl}/login`, { username, password })
			.pipe(
				tap((response) => {
					this.setToken(response.token);
					this.setUsername(response.username);
				}),
				switchMap((response) => this.fetchAppUser(response.username)),
				tap((appUser) => this.setAppUser(appUser)),
				map(() => undefined),
				catchError((error) => {
					this.clearToken();
					return throwError(() => error);
				})
			);
	}

	getToken(): string | null {
		return this.tokenSubject.value;
	}

	clearToken(): void {
		this.tokenSubject.next(null);
		this.usernameSubject.next(null);
		this.appUserSubject.next(null);
		localStorage.removeItem(this.tokenStorageKey);
		localStorage.removeItem(this.usernameStorageKey);
		localStorage.removeItem(this.appUserStorageKey);
	}

	private setToken(token: string): void {
		this.tokenSubject.next(token);
		localStorage.setItem(this.tokenStorageKey, token);
	}

	private setUsername(username: string): void {
		this.usernameSubject.next(username);
		localStorage.setItem(this.usernameStorageKey, username);
	}

	private fetchAppUser(username: string): Observable<AppUserResponse> {
		return this.http.get<AppUserResponse>(`${this.appUserBaseUrl}/${encodeURIComponent(username)}`);
	}

	private setAppUser(appUser: AppUserResponse): void {
		this.appUserSubject.next(appUser);
		localStorage.setItem(this.appUserStorageKey, JSON.stringify(appUser));
	}

	private loadToken(): string | null {
		return localStorage.getItem(this.tokenStorageKey);
	}

	private loadUsername(): string | null {
		return localStorage.getItem(this.usernameStorageKey);
	}

	private loadAppUser(): AppUserResponse | null {
		const stored = localStorage.getItem(this.appUserStorageKey);
		if (!stored) {
			return null;
		}
		try {
			return JSON.parse(stored) as AppUserResponse;
		} catch {
			localStorage.removeItem(this.appUserStorageKey);
			return null;
		}
	}
}
