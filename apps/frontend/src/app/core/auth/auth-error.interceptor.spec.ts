/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { DOCUMENT } from '@angular/common';
import { HttpErrorResponse, HttpRequest } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import Keycloak from 'keycloak-js';
import { firstValueFrom, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';

import { createDocumentMock, createKeycloakMock } from '../../../testing/auth-mocks';
import { authErrorInterceptor } from './auth-error.interceptor';
import { AuthService } from './auth.service';

describe('authErrorInterceptor', () => {
  it('clears the unusable token and redirects to login after an API 401', async () => {
    const keycloak = createKeycloakMock();
    const auth = { login: vi.fn().mockResolvedValue(undefined) };
    TestBed.configureTestingModule({
      providers: [
        { provide: Keycloak, useValue: keycloak },
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: { navigateByUrl: vi.fn() } },
        { provide: DOCUMENT, useValue: createDocumentMock() },
      ],
    });
    const unauthorized = new HttpErrorResponse({ status: 401, url: '/api/worksites' });
    const response = TestBed.runInInjectionContext(() =>
      authErrorInterceptor(new HttpRequest('GET', '/api/worksites'), () =>
        throwError(() => unauthorized),
      ),
    );

    await expect(firstValueFrom(response)).rejects.toBe(unauthorized);
    expect(keycloak.clearToken).toHaveBeenCalledOnce();
    expect(auth.login).toHaveBeenCalledWith();
  });
});
