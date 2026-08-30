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
import type { AuthRouteData } from './auth.models';

function asArray<T>(v: T | readonly T[] | null | undefined): readonly T[] {
  if (v == null) return [];
  return (Array.isArray(v) ? v : [v]) as readonly T[];
}

export async function isAccessAllowed(
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  {
    authenticated,
    grantedRoles: { realmRoles, resourceRoles: clientRoles },
    keycloak,
  }: AuthGuardData,
): Promise<boolean | UrlTree> {
  const router = inject(Router);
  const redirects = inject(AuthRedirectService);
  // A canActivateChild guard receives the child snapshot. Resolve the policy from
  // the complete route explicitly instead of relying on paramsInheritanceStrategy.
  const roleData = route.pathFromRoot.reduce<AuthRouteData>(
    (policy, snapshot) => ({ ...policy, ...(snapshot.data as AuthRouteData) }),
    {},
  );

  if (!authenticated) {
    const redirectUri = redirects.loginRedirectUri(state.url || '/');
    await keycloak.login({
      ...(redirectUri !== undefined ? { redirectUri } : {}),
    });
    return false;
  }

  const requiredRealmRoles = asArray(roleData?.realmRole);
  const requiredClientRoles = asArray(roleData?.clientRole);

  const hasAnyRealmRole = requiredRealmRoles.some((role) => realmRoles.includes(role));
  const hasAnyClientRole = requiredClientRoles.some((required) =>
    clientRoles[required.clientId]?.includes(required.role),
  );

  const anyRoleRequired = requiredRealmRoles.length > 0 || requiredClientRoles.length > 0;
  const isAuthorized = !anyRoleRequired || hasAnyRealmRole || hasAnyClientRole;

  return isAuthorized ? true : router.parseUrl('/forbidden');
}

export const authChildGuard = createAuthGuard<CanActivateChildFn>(isAccessAllowed);
