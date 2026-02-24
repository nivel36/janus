import { bootstrapApplication } from '@angular/platform-browser';

import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';
import { initKeycloak } from './app/auth/keycloak';

initKeycloak()
	.catch((error) => {
		console.warn('Keycloak initialization skipped or failed', error);
		return false;
	})
	.then((authenticated: boolean) => {
		if (authenticated) {
			console.info('Authenticated with Keycloak.');
		}

		return bootstrapApplication(AppComponent, appConfig);
	})
	.catch((error) => console.error(error));
