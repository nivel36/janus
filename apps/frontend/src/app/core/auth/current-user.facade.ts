/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Injectable, inject } from '@angular/core';
import { combineLatest, Observable } from 'rxjs';
import { map, distinctUntilChanged, shareReplay } from 'rxjs/operators';

import { AuthService } from './auth.service';

export interface CurrentUser {
  email: string | null;
  fullName: string;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isUser: boolean;
  isEmployee: boolean;
}

@Injectable({ providedIn: 'root' })
export class CurrentUserFacade {
  private readonly authService = inject(AuthService);

  private hasRole(permissions: any, role: string): boolean {
    return Array.isArray(permissions?.realmRoles) && permissions.realmRoles.includes(role);
  }

  readonly isAuthenticated$ = this.authService.isAuthenticated$;

  readonly email$ = this.authService.claims$.pipe(
    map((claims) => claims?.email ?? null),
    distinctUntilChanged(),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  readonly fullName$ = this.authService.claims$.pipe(
    map((claims) => `${claims?.given_name ?? ''} ${claims?.family_name ?? ''}`.trim()),
    distinctUntilChanged(),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  readonly permissions$ = this.authService.permissions$.pipe(
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  readonly isAdmin$ = this.permissions$.pipe(
    map((p) => this.hasRole(p, 'JANUS_ADMIN')),
    distinctUntilChanged(),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  readonly isUser$ = this.permissions$.pipe(
    map((p) => this.hasRole(p, 'JANUS_USER')),
    distinctUntilChanged(),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  readonly isEmployee$ = this.permissions$.pipe(
    map((p) => this.hasRole(p, 'JANUS_EMPLOYEE')),
    distinctUntilChanged(),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  readonly currentUser$: Observable<CurrentUser> = combineLatest([
    this.isAuthenticated$,
    this.email$,
    this.fullName$,
    this.isAdmin$,
    this.isUser$,
    this.isEmployee$,
  ]).pipe(
    map(([isAuthenticated, email, fullName, isAdmin, isUser, isEmployee]) => ({
      email,
      fullName,
      isAuthenticated,
      isAdmin,
      isUser,
      isEmployee,
    })),
    distinctUntilChanged(
      (a, b) =>
        a.email === b.email &&
        a.fullName === b.fullName &&
        a.isAuthenticated === b.isAuthenticated &&
        a.isAdmin === b.isAdmin &&
        a.isUser === b.isUser &&
        a.isEmployee === b.isEmployee,
    ),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  isAdmin(): boolean {
    return this.authService.hasRealmRole('JANUS_ADMIN');
  }

  isUser(): boolean {
    return this.authService.hasRealmRole('JANUS_USER');
  }

  isEmployee(): boolean {
    return this.authService.hasRealmRole('JANUS_EMPLOYEE');
  }
}
