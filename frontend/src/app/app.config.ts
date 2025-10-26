import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient } from "@angular/common/http";
import { provideTranslateService } from "@ngx-translate/core";
import { provideTranslateHttpLoader } from "@ngx-translate/http-loader";

export const appConfig: ApplicationConfig = {
	providers: [
		provideBrowserGlobalErrorListeners(),
		provideZoneChangeDetection({ eventCoalescing: true }),
		provideHttpClient(),
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