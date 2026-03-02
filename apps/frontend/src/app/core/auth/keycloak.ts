/**
 * SPDX-License-Identifier: Apache-2.0
 */
import Keycloak, { type KeycloakConfig } from 'keycloak-js';
import { environment } from '../../../environments/environment';

const keycloakConfig: KeycloakConfig = {
  url: environment.keycloak.url,
  realm: environment.keycloak.realm,
  clientId: environment.keycloak.clientId,
};

const hasValidConfig = Object.values(keycloakConfig).every(Boolean);

if (!hasValidConfig) {
  throw new Error('Invalid Keycloak configuration. Check environment.keycloak settings.');
}

export const keycloak = new Keycloak(keycloakConfig);

export async function initKeycloak(): Promise<boolean> {
  return keycloak.init({
    onLoad: 'check-sso',
    pkceMethod: 'S256',
    checkLoginIframe: false,
    silentCheckSsoRedirectUri: window.location.origin + '/assets/silent-check-sso.html',
  });
}

export function login(redirectUri?: string) {
  return keycloak.login({
    redirectUri: redirectUri ?? window.location.href,
  });
}

export function logout() {
  return keycloak.logout({
    redirectUri: window.location.origin,
  });
}
