import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { map, take } from 'rxjs';

import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = (): ReturnType<CanActivateFn> => {
	const authService = inject(AuthService);
	const router = inject(Router);

	return authService.isAuthenticated$.pipe(
		take(1),
		map((isAuthenticated): boolean | UrlTree =>
			isAuthenticated ? true : router.createUrlTree(['/login'])
		)
	);
};
