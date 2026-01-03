import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient, withInterceptors } from "@angular/common/http";
import { provideTranslateService } from "@ngx-translate/core";
import { provideTranslateHttpLoader } from "@ngx-translate/http-loader";
import { authInterceptor } from './auth/auth.interceptor';

export const appConfig: ApplicationConfig = {
	providers: [
		provideBrowserGlobalErrorListeners(),
		provideZoneChangeDetection({ eventCoalescing: true }),
		provideHttpClient(withInterceptors([authInterceptor])),
		provideTranslateService({
			lang: 'en',
			fallbackLang: 'en',
			loader: provideTranslateHttpLoader({
				prefix: 'assets/i18n/',
				suffix: '.json'
			})
		}),
	]
};
