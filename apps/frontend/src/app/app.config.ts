/**
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { authInterceptor } from './core/auth/auth.interceptor';
import { authErrorInterceptor } from './core/auth/auth-error.interceptor';
import { provideRouter } from '@angular/router';
import { appRoutes } from './app.routes';

export const supportedLanguages = ['en-EN', 'es-ES', 'ca-ES'] as const;

export function resolveSupportedLanguage(
  locale: string | undefined | null,
  fallbackLanguage: (typeof supportedLanguages)[number] = 'es-ES',
): (typeof supportedLanguages)[number] {
  const normalizedLanguage = locale?.toLowerCase().split('-')[0];

  if (
    normalizedLanguage &&
    supportedLanguages.includes(normalizedLanguage as (typeof supportedLanguages)[number])
  ) {
    return normalizedLanguage as (typeof supportedLanguages)[number];
  }

  return fallbackLanguage;
}

export function resolveInitialLanguage(
  browserLanguages: readonly string[] | undefined,
  fallbackLanguage: (typeof supportedLanguages)[number] = 'en-EN',
): (typeof supportedLanguages)[number] {
  for (const browserLanguage of browserLanguages ?? []) {
    const resolvedLanguage = resolveSupportedLanguage(browserLanguage, fallbackLanguage);

    if (
      resolvedLanguage !== fallbackLanguage ||
      browserLanguage?.toLowerCase().startsWith(fallbackLanguage)
    ) {
      return resolvedLanguage;
    }
  }

  return fallbackLanguage;
}

const initialLanguage = resolveInitialLanguage(
  typeof navigator === 'undefined'
    ? undefined
    : [navigator.language, ...(navigator.languages ?? [])],
);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideHttpClient(withInterceptors([authInterceptor, authErrorInterceptor])),
    provideRouter(appRoutes),
    provideTranslateService({
      lang: initialLanguage,
      fallbackLang: 'en',
      loader: provideTranslateHttpLoader({
        prefix: 'assets/i18n/',
        suffix: '.json',
      }),
    }),
  ],
};
