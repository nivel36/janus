/**
 * SPDX-License-Identifier: Apache-2.0
 */
import type { KeycloakTokenParsed } from 'keycloak-js';

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

/** A client role required by a route authorization policy. */
export interface ClientRoleRequirement {
  clientId: string;
  role: string;
}

/** Authorization policy supported in an Angular route's `data`. */
export interface AuthRouteData {
  realmRole?: string | readonly string[];
  clientRole?: ClientRoleRequirement | readonly ClientRoleRequirement[];
}
