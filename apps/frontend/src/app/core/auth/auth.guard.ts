import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, take } from 'rxjs';

import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = (_route, state): ReturnType<CanActivateFn> => {
	const authService = inject(AuthService);
	const router = inject(Router);

	return authService.isAuthenticated$.pipe(
		take(1),
		map((isAuthenticated): boolean => {
			if (isAuthenticated) {
				return true;
			}

			const targetUrl = state.url ? router.serializeUrl(router.parseUrl(state.url)) : '/';
			void authService.loginWithRedirect(targetUrl);
			return false;
		})
	);
};
