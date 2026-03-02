/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { bootstrapApplication } from '@angular/platform-browser';

import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';
import { initKeycloak } from './app/core/auth/keycloak';

async function start() {
  try {
    const authenticated = await initKeycloak();

    if (authenticated) {
      console.info('Authenticated with Keycloak.');
    }
  } catch (error) {
    console.warn('Keycloak initialization skipped or failed', error);
  }

  return bootstrapApplication(AppComponent, appConfig);
}

start().catch((error) => console.error(error));
