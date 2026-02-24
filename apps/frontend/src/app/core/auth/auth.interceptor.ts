import { HttpInterceptorFn } from '@angular/common/http';
import { from, switchMap } from 'rxjs';
import { keycloak } from '../../auth/keycloak';
import { environment } from '../../../environments/environment';

const MIN_TOKEN_VALIDITY_SECONDS = 30;
let loginRedirectInProgress = false;

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

async function getUpdatedToken(): Promise<string | undefined> {
	if (!keycloak?.authenticated) {
		return keycloak?.token;
	}

	try {
		await keycloak.updateToken(MIN_TOKEN_VALIDITY_SECONDS);
		loginRedirectInProgress = false;
		return keycloak.token;
	} catch {
		keycloak.clearToken();
		if (!loginRedirectInProgress) {
			loginRedirectInProgress = true;
			void keycloak.login({ redirectUri: globalThis.location?.href });
		}
		return undefined;
	}
}

export const authInterceptor: HttpInterceptorFn = (request, next) => {
	if (isKeycloakRequest(request.url) || !isProtectedApiRequest(request.url)) {
		return next(request);
	}

	return from(getUpdatedToken()).pipe(
		switchMap((token) => {
			if (!token) {
				return next(request);
			}

			const withAuth = request.clone({
				setHeaders: {
					Authorization: `Bearer ${token}`
				}
			});
			return next(withAuth);
		})
	);
};
