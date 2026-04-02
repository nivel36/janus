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

export const supportedLanguages = ['en', 'es', 'ca'] as const;

export function resolveInitialLanguage(
  browserLanguages: readonly string[] | undefined,
  fallbackLanguage: (typeof supportedLanguages)[number] = 'en',
): (typeof supportedLanguages)[number] {
  const normalizedLanguages = (browserLanguages ?? [])
    .map((language) => language.toLowerCase().split('-')[0])
    .filter((language): language is (typeof supportedLanguages)[number] =>
      supportedLanguages.includes(language as (typeof supportedLanguages)[number]),
    );

  return normalizedLanguages[0] ?? fallbackLanguage;
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
