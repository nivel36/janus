/**
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  ApplicationConfig,
  inject,
  InjectionToken,
  PLATFORM_ID,
  provideAppInitializer,
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
  type IncludeBearerTokenCondition,
} from 'keycloak-angular';
import Keycloak, { type KeycloakInitOptions } from 'keycloak-js';
import { provideTranslateService, TRANSLATE_SERVICE_CONFIG } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { provideRouter } from '@angular/router';
import { appRoutes } from './app.routes';
import { authErrorInterceptor } from './core/auth/auth-error.interceptor';
import { resolveKeycloakLocale } from './core/auth/keycloak-locale';
import { environment } from '../environments/environment';
import { FALLBACK_LANGUAGE, resolveInitialLanguage } from './core/i18n/language.util';
import { DOCUMENT, isPlatformBrowser, registerLocaleData } from '@angular/common';

import localeEs from '@angular/common/locales/es';
import localeCa from '@angular/common/locales/ca';

registerLocaleData(localeEs);
registerLocaleData(localeCa);

export const APP_ORIGIN = new InjectionToken<string | undefined>('Application origin', {
  factory: () => {
    const platformId = inject(PLATFORM_ID);
    const document = inject(DOCUMENT);
    return isPlatformBrowser(platformId) ? document.defaultView?.location.origin : undefined;
  },
});

export const INITIAL_LANGUAGE = new InjectionToken<string>('Initial language', {
  factory: () => {
    const platformId = inject(PLATFORM_ID);
    const document = inject(DOCUMENT);
    const navigator = isPlatformBrowser(platformId) ? document.defaultView?.navigator : undefined;
    return resolveInitialLanguage(
      navigator ? [navigator.language, ...(navigator.languages ?? [])] : undefined,
    );
  },
});

export const KEYCLOAK_INIT_OPTIONS = new InjectionToken<KeycloakInitOptions>(
  'Keycloak initialization options',
  {
    factory: () => {
      const origin = inject(APP_ORIGIN);
      const language = inject(INITIAL_LANGUAGE);
      return {
        onLoad: 'check-sso',
        pkceMethod: 'S256',
        checkLoginIframe: false,
        silentCheckSsoRedirectUri: origin
          ? `${origin}/assets/silent-check-sso.html`
          : undefined,
        locale: resolveKeycloakLocale([language]),
      };
    },
  },
);

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

export function apiBearerUrlPattern(
  apiBaseUrl: string,
  origin?: string,
): RegExp {
  const base = apiBaseUrl.replace(/\/$/, '');
  const absoluteBase = /^https?:\/\//i.test(base)
    ? base
    : `${origin ?? ''}${base.startsWith('/') ? '' : '/'}${base}`;
  const alternatives = absoluteBase === base ? [base] : [base, absoluteBase];

  return new RegExp(`^(?:${alternatives.map(escapeRegExp).join('|')})(?:/|\\?|$)`, 'i');
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideKeycloak({
      config: environment.keycloak,
      providers: [AutoRefreshTokenService, UserActivityService],
    }),
    provideAppInitializer(() => {
      const keycloak = inject(Keycloak);
      const initOptions = inject(KEYCLOAK_INIT_OPTIONS);
      inject(AutoRefreshTokenService).start({
        onInactivityTimeout: 'logout',
        sessionTimeout: 300_000,
      });
      return keycloak
        .init(initOptions)
        .catch((error) => console.error('Keycloak initialization failed', error));
    }),
    {
      provide: INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
      useFactory: () => [
        createInterceptorCondition<IncludeBearerTokenCondition>({
          urlPattern: apiBearerUrlPattern(environment.apiBaseUrl, inject(APP_ORIGIN)),
          bearerPrefix: 'Bearer',
        }),
      ],
    },
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideHttpClient(withInterceptors([includeBearerTokenInterceptor, authErrorInterceptor])),
    provideRouter(appRoutes),
    provideTranslateService({
      fallbackLang: FALLBACK_LANGUAGE,
      loader: provideTranslateHttpLoader({
        prefix: 'assets/i18n/',
        suffix: '.json',
      }),
    }),
    {
      provide: TRANSLATE_SERVICE_CONFIG,
      useFactory: () => ({ lang: inject(INITIAL_LANGUAGE), fallbackLang: FALLBACK_LANGUAGE }),
    },
  ],
};
