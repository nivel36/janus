/**
 * SPDX-License-Identifier: Apache-2.0
 */
import type { KeycloakTokenParsed } from 'keycloak-js';

/** Realm roles owned by Janus and assigned through Keycloak. */
export const JANUS_REALM_ROLES = {
  ADMIN: 'JANUS_ADMIN',
  USER: 'JANUS_USER',
  EMPLOYEE: 'JANUS_EMPLOYEE',
} as const;

export type JanusRealmRole = (typeof JANUS_REALM_ROLES)[keyof typeof JANUS_REALM_ROLES];

export interface ApplicationTokenClaims extends KeycloakTokenParsed {
  preferred_username?: string;
  email?: string;
  given_name?: string;
  family_name?: string;
  locale?: string;
}

/** Roles keyed by the Keycloak client that grants them. */
export type ClientRolesByClient = Readonly<Record<string, readonly string[]>>;

export interface PermissionState {
  readonly realmRoles: readonly string[];
  /**
   * Keycloak client roles. This is the application-facing name for the roles
   * represented by `resource_access` in a token and `resourceRoles` in
   * keycloak-angular.
   */
  readonly clientRoles: ClientRolesByClient;
}

/** Authorization policy supported in an Angular route's `data`. */
export interface AuthRouteData {
  realmRole?: JanusRealmRole | readonly JanusRealmRole[];
}
