import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, map, Observable, tap } from 'rxjs';

interface LoginResponse {
	token: string;
	username: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
	private readonly baseUrl = '/api/v1/auth';
	private readonly tokenStorageKey = 'janus.auth.token';
	private readonly usernameStorageKey = 'janus.auth.username';
	private readonly tokenSubject = new BehaviorSubject<string | null>(this.loadToken());
	private readonly usernameSubject = new BehaviorSubject<string | null>(this.loadUsername());
	readonly isAuthenticated$ = this.tokenSubject.pipe(map((token) => !!token));
	readonly username$ = this.usernameSubject.asObservable();

	constructor(private readonly http: HttpClient) {}

	login(username: string, password: string): Observable<void> {
		return this.http
			.post<LoginResponse>(`${this.baseUrl}/login`, { username, password })
			.pipe(
				tap((response) => {
					this.setToken(response.token);
					this.setUsername(response.username);
				}),
				map(() => undefined)
			);
	}

	getToken(): string | null {
		return this.tokenSubject.value;
	}

	clearToken(): void {
		this.tokenSubject.next(null);
		this.usernameSubject.next(null);
		localStorage.removeItem(this.tokenStorageKey);
		localStorage.removeItem(this.usernameStorageKey);
	}

	private setToken(token: string): void {
		this.tokenSubject.next(token);
		localStorage.setItem(this.tokenStorageKey, token);
	}

	private setUsername(username: string): void {
		this.usernameSubject.next(username);
		localStorage.setItem(this.usernameStorageKey, username);
	}

	private loadToken(): string | null {
		return localStorage.getItem(this.tokenStorageKey);
	}

	private loadUsername(): string | null {
		return localStorage.getItem(this.usernameStorageKey);
	}
}
