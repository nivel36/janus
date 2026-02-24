import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
	if (request.url.endsWith('/auth/login')) {
		return next(request);
	}

	const token = inject(AuthService).getToken();
	if (!token) {
		return next(request);
	}

	const withAuth = request.clone({
		setHeaders: {
			Authorization: `Bearer ${token}`
		}
	});
	return next(withAuth);
};
