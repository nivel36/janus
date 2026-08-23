/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { inject } from '@angular/core';
import {
  CanActivateFn,
  Router,
  type ActivatedRouteSnapshot,
  type RouterStateSnapshot,
  type UrlTree,
} from '@angular/router';
import { createAuthGuard, type AuthGuardData } from 'keycloak-angular';
import { resolveKeycloakLocale } from './keycloak-locale';

interface ClientRole {
  clientId: string;
  role: string;
}

interface RouteRoleData {
  realmRole?: string | string[];
  clientRole?: ClientRole | ClientRole[];
}

function asArray<T>(v: T | T[] | null | undefined): T[] {
  if (v == null) return [];
  return Array.isArray(v) ? v : [v];
}

export async function isAccessAllowed(
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  { authenticated, grantedRoles, keycloak }: AuthGuardData,
): Promise<boolean | UrlTree> {
  const router = inject(Router);
  const roleData = route.data as RouteRoleData | undefined;

  if (!authenticated) {
    const targetUrl = new URL(state.url || '/', globalThis.location?.origin).toString();
    await keycloak.login({ redirectUri: targetUrl, locale: resolveKeycloakLocale() });
    return false;
  }

  const requiredRealmRoles = asArray(roleData?.realmRole);
  const requiredClientRoles = asArray(roleData?.clientRole);

  const hasAnyRealmRole = requiredRealmRoles.some((role) => grantedRoles.realmRoles.includes(role));
  const hasAnyClientRole = requiredClientRoles.some((required) =>
    grantedRoles.resourceRoles[required.clientId]?.includes(required.role),
  );

  const anyRoleRequired = requiredRealmRoles.length > 0 || requiredClientRoles.length > 0;
  const isAuthorized = !anyRoleRequired || hasAnyRealmRole || hasAnyClientRole;

  return isAuthorized ? true : router.parseUrl('/forbidden');
}

export const authGuard = createAuthGuard<CanActivateFn>(isAccessAllowed);
