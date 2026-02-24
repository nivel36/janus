import Keycloak, { type KeycloakConfig } from 'keycloak-js';

import { environment } from '../../environments/environment';

const keycloakConfig: KeycloakConfig = {
	url: environment.keycloak.url,
	realm: environment.keycloak.realm,
	clientId: environment.keycloak.clientId
};

const hasValidConfig = Object.values(keycloakConfig).every((value) => Boolean(value));

export const keycloak = hasValidConfig ? new Keycloak(keycloakConfig) : null;

export function initKeycloak(): Promise<boolean> {
	if (!keycloak) {
		return Promise.resolve(false);
	}

	return keycloak.init({
		onLoad: 'check-sso',
		pkceMethod: 'S256'
	});
}
