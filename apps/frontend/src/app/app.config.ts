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
import { provideRouter } from '@angular/router';
import { appRoutes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';
import { authErrorInterceptor } from './core/auth/auth-error.interceptor';
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

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideHttpClient(withInterceptors([authInterceptor, authErrorInterceptor])),
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
