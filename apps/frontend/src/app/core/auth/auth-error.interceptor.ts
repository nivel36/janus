import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from './auth.service';

export const authErrorInterceptor: HttpInterceptorFn = (request, next) => {
	const authService = inject(AuthService);
	const router = inject(Router);

	return next(request).pipe(
		catchError((error: unknown) => {
			if (error instanceof HttpErrorResponse && error.status === 401) {
				authService.clearToken();
				void router.navigate(['/login']);
			}
			return throwError(() => error);
		})
	);
};
