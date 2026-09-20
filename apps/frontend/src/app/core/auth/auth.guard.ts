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
import { JANUS_API_CLIENT_ID, type AuthRouteData } from './auth.models';
import { AuthService } from './auth.service';
import { catchError, firstValueFrom, of } from 'rxjs';
import { CurrentUserFacade } from '../user/services/current-user.facade';

function asArray<T>(v: T | readonly T[] | null | undefined): readonly T[] {
  if (v == null) return [];
  return (Array.isArray(v) ? v : [v]) as readonly T[];
}

export async function isAccessAllowed(
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  { authenticated, grantedRoles: { resourceRoles } }: AuthGuardData,
): Promise<boolean | UrlTree> {
  const router = inject(Router);
  const auth = inject(AuthService);
  const currentUser = inject(CurrentUserFacade);
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

  if (!auth.isUsableIdentity()) {
    return router.createUrlTree(['/verify-email'], {
      queryParams: { returnUrl: state.url || '/' },
    });
  }

  const requiredClientRoles = asArray(roleData?.clientRole);
  const janusClientRoles = resourceRoles[JANUS_API_CLIENT_ID] ?? [];
  const hasAnyClientRole = requiredClientRoles.some((role) => janusClientRoles.includes(role));
  const isAuthorized = requiredClientRoles.length === 0 || hasAnyClientRole;

  if (!isAuthorized) {
    return router.parseUrl('/forbidden');
  }

  // GET /appusers/me provisions the local account. Protected pages must wait
  // for it before issuing requests whose policies resolve that account.
  const preferences = await firstValueFrom(
    currentUser.preferences$.pipe(catchError(() => of(null))),
  );
  return preferences !== null ? true : router.parseUrl('/forbidden');
}

export const authChildGuard = createAuthGuard<CanActivateChildFn>(isAccessAllowed);
