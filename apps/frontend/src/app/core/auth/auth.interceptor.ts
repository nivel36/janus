/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { HttpInterceptorFn } from '@angular/common/http';
import { from, switchMap } from 'rxjs';
import { keycloak } from './keycloak';
import { environment } from '../../../environments/environment';

const MIN_TOKEN_VALIDITY_SECONDS = 30;
let refreshInFlight: Promise<string | undefined> | null = null;
let loginRedirectInProgress = false;

function isKeycloakRequest(url: string): boolean {
  const base = environment.keycloak.url?.trim();
  return Boolean(base) && url.startsWith(base!);
}

function isProtectedApiRequest(url: string): boolean {
  if (url.startsWith(environment.apiUrl) || url.startsWith(environment.apiBaseUrl)) {
    return true;
  }
  const origin = globalThis.location?.origin;
  return Boolean(origin) && url.startsWith(`${origin}${environment.apiBaseUrl}`);
}

async function getTokenFresh(): Promise<string | undefined> {
  if (!keycloak?.authenticated) return undefined;

  if (refreshInFlight) return refreshInFlight;

  refreshInFlight = (async () => {
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
    } finally {
      refreshInFlight = null;
    }
  })();

  return refreshInFlight;
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (isKeycloakRequest(req.url) || !isProtectedApiRequest(req.url)) {
    return next(req);
  }

  return from(getTokenFresh()).pipe(
    switchMap((token) => {
      if (!token) {
        return next(req);
      }
      return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
    }),
  );
};
