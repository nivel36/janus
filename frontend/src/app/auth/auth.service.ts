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
	private readonly tokenSubject = new BehaviorSubject<string | null>(this.loadToken());
	readonly isAuthenticated$ = this.tokenSubject.pipe(map((token) => !!token));

	constructor(private readonly http: HttpClient) {}

	login(username: string, password: string): Observable<void> {
		return this.http
			.post<LoginResponse>(`${this.baseUrl}/login`, { username, password })
			.pipe(
				tap((response) => this.setToken(response.token)),
				map(() => undefined)
			);
	}

	getToken(): string | null {
		return this.tokenSubject.value;
	}

	clearToken(): void {
		this.tokenSubject.next(null);
		localStorage.removeItem(this.tokenStorageKey);
	}

	private setToken(token: string): void {
		this.tokenSubject.next(token);
		localStorage.setItem(this.tokenStorageKey, token);
	}

	private loadToken(): string | null {
		return localStorage.getItem(this.tokenStorageKey);
	}
}
