/**
 * SPDX-License-Identifier: Apache-2.0
 */
import type { KeycloakTokenParsed } from 'keycloak-js';

/** Client roles owned by Janus and assigned through the Janus API resource client. */
export const JANUS_CLIENT_ROLES = {
  ADMIN: 'JANUS_ADMIN',
  USER: 'JANUS_USER',
  EMPLOYEE: 'JANUS_EMPLOYEE',
} as const;

/** @deprecated Use JANUS_CLIENT_ROLES. */
export const JANUS_REALM_ROLES = JANUS_CLIENT_ROLES;

/** Keycloak resource client whose roles grant access to Janus. */
export const JANUS_API_CLIENT_ID = 'janus-api';

export type JanusClientRole = (typeof JANUS_CLIENT_ROLES)[keyof typeof JANUS_CLIENT_ROLES];

export interface AuthTokenClaims
  extends Omit<KeycloakTokenParsed, 'realm_access' | 'resource_access'> {
  readonly preferred_username?: string;
  readonly email?: string;
  readonly given_name?: string;
  readonly family_name?: string;
  readonly locale?: string;
  readonly realm_access?: {
    readonly roles: readonly string[];
  };
  readonly resource_access?: Readonly<
    Record<
      string,
      {
        readonly roles: readonly string[];
      }
    >
  >;
}

/** Roles keyed by the Keycloak client that grants them. */
export type ClientRolesByClient = Readonly<Record<string, readonly string[]>>;

export interface AuthPermissions {
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
  clientRole?: JanusClientRole | readonly JanusClientRole[];
}
