/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import Keycloak from 'keycloak-js';
import { catchError, throwError } from 'rxjs';

import { AuthService } from './auth.service';

export const authErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const keycloak = inject(Keycloak);
  const router = inject(Router);
  const auth = inject(AuthService);

  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        keycloak.clearToken();
        void auth.login();
      }
      if (error instanceof HttpErrorResponse && error.status === 403) {
        void router.navigateByUrl('/forbidden');
      }
      return throwError(() => error);
    }),
  );
};
