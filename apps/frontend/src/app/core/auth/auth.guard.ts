/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { inject } from '@angular/core';
import {
  CanActivateChildFn,
  Router,
  type ActivatedRouteSnapshot,
  type RouterStateSnapshot,
  type UrlTree,
} from '@angular/router';
import { createAuthGuard, type AuthGuardData } from 'keycloak-angular';
import { AuthRedirectService } from './auth-redirect.service';
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
  const redirects = inject(AuthRedirectService);
  // A canActivateChild guard receives the child snapshot. Resolve the policy from
  // the complete route explicitly instead of relying on paramsInheritanceStrategy.
  const roleData = route.pathFromRoot.reduce<RouteRoleData>(
    (policy, snapshot) => ({ ...policy, ...(snapshot.data as RouteRoleData) }),
    {},
  );

  if (!authenticated) {
    const redirectUri = redirects.loginRedirectUri(state.url || '/');
    await keycloak.login({
      ...(redirectUri !== undefined ? { redirectUri } : {}),
      locale: resolveKeycloakLocale(),
    });
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

export const authChildGuard = createAuthGuard<CanActivateChildFn>(isAccessAllowed);
