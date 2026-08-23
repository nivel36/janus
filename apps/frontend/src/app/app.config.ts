/**
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  AutoRefreshTokenService,
  createInterceptorCondition,
  INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
  includeBearerTokenInterceptor,
  provideKeycloak,
  UserActivityService,
  withAutoRefreshToken,
  type IncludeBearerTokenCondition,
} from 'keycloak-angular';
import type { KeycloakInitOptions } from 'keycloak-js';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { provideRouter } from '@angular/router';
import { appRoutes } from './app.routes';
import { authErrorInterceptor } from './core/auth/auth-error.interceptor';
import { resolveKeycloakLocale } from './core/auth/keycloak-locale';
import { environment } from '../environments/environment';
import { FALLBACK_LANGUAGE, resolveInitialLanguage } from './core/i18n/language.util';
import { registerLocaleData } from '@angular/common';

import localeEs from '@angular/common/locales/es';
import localeCa from '@angular/common/locales/ca';

registerLocaleData(localeEs);
registerLocaleData(localeCa);

const initialLanguage = resolveInitialLanguage(
  typeof navigator === 'undefined'
    ? undefined
    : [navigator.language, ...(navigator.languages ?? [])],
);

export const keycloakInitOptions: KeycloakInitOptions = {
  onLoad: 'check-sso',
  pkceMethod: 'S256',
  checkLoginIframe: false,
  silentCheckSsoRedirectUri: `${globalThis.location?.origin ?? ''}/assets/silent-check-sso.html`,
  locale: resolveKeycloakLocale(),
};

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

export function apiBearerUrlPattern(
  apiBaseUrl: string,
  origin = globalThis.location?.origin,
): RegExp {
  const base = apiBaseUrl.replace(/\/$/, '');
  const absoluteBase = /^https?:\/\//i.test(base)
    ? base
    : `${origin ?? ''}${base.startsWith('/') ? '' : '/'}${base}`;
  const alternatives = absoluteBase === base ? [base] : [base, absoluteBase];

  return new RegExp(`^(?:${alternatives.map(escapeRegExp).join('|')})(?:/|\\?|$)`, 'i');
}

export const apiBearerCondition = createInterceptorCondition<IncludeBearerTokenCondition>({
  urlPattern: apiBearerUrlPattern(environment.apiBaseUrl),
  bearerPrefix: 'Bearer',
});

export const appConfig: ApplicationConfig = {
  providers: [
    provideKeycloak({
      config: environment.keycloak,
      initOptions: keycloakInitOptions,
      features: [
        withAutoRefreshToken({
          onInactivityTimeout: 'logout',
          sessionTimeout: 300_000,
        }),
      ],
      providers: [AutoRefreshTokenService, UserActivityService],
    }),
    {
      provide: INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
      useValue: [apiBearerCondition],
    },
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideHttpClient(withInterceptors([includeBearerTokenInterceptor, authErrorInterceptor])),
    provideRouter(appRoutes),
    provideTranslateService({
      lang: initialLanguage,
      fallbackLang: FALLBACK_LANGUAGE,
      loader: provideTranslateHttpLoader({
        prefix: 'assets/i18n/',
        suffix: '.json',
      }),
    }),
  ],
};
