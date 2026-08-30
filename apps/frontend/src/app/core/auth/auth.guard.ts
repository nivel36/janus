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
import type { AuthRouteData } from './auth.models';
import { AuthService } from './auth.service';

function asArray<T>(v: T | readonly T[] | null | undefined): readonly T[] {
  if (v == null) return [];
  return (Array.isArray(v) ? v : [v]) as readonly T[];
}

export async function isAccessAllowed(
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  { authenticated, grantedRoles: { realmRoles } }: AuthGuardData,
): Promise<boolean | UrlTree> {
  const router = inject(Router);
  const auth = inject(AuthService);
  // A canActivateChild guard receives the child snapshot. Resolve the policy from
  // the complete route explicitly instead of relying on paramsInheritanceStrategy.
  const roleData = route.pathFromRoot.reduce<AuthRouteData>(
    (policy, snapshot) => ({ ...policy, ...(snapshot.data as AuthRouteData) }),
    {},
  );

  if (!authenticated) {
    await auth.login(state.url || '/');
    return false;
  }

  const requiredRealmRoles = asArray(roleData?.realmRole);
  const hasAnyRealmRole = requiredRealmRoles.some((role) => realmRoles.includes(role));
  const isAuthorized = requiredRealmRoles.length === 0 || hasAnyRealmRole;

  return isAuthorized ? true : router.parseUrl('/forbidden');
}

export const authChildGuard = createAuthGuard<CanActivateChildFn>(isAccessAllowed);
