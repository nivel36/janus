import { HttpInterceptorFn } from '@angular/common/http';
import { keycloak } from '../../auth/keycloak';
import { environment } from '../../../environments/environment';

function isKeycloakRequest(url: string): boolean {
	const keycloakUrl = environment.keycloak.url?.trim();
	if (!keycloakUrl) {
		return false;
	}

	return url.startsWith(keycloakUrl);
}

function isProtectedApiRequest(url: string): boolean {
	if (url.startsWith(environment.apiUrl) || url.startsWith(environment.apiBaseUrl)) {
		return true;
	}

	if (!globalThis.location?.origin) {
		return false;
	}

	return url.startsWith(`${globalThis.location.origin}${environment.apiBaseUrl}`);
}

export const authInterceptor: HttpInterceptorFn = (request, next) => {
	if (isKeycloakRequest(request.url) || !isProtectedApiRequest(request.url)) {
		return next(request);
	}

	const token = keycloak?.token;
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
