/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, take } from 'rxjs';

import { AuthService } from './auth.service';

interface RouteRoleData {
  realmRole?: string | string[];
  clientRole?: { clientId: string; role: string } | Array<{ clientId: string; role: string }>;
}

function asArray<T>(v: T | T[] | null | undefined): T[] {
  return v == null ? [] : Array.isArray(v) ? v : [v];
}

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const roleData = route.data as RouteRoleData | undefined;

  return authService.isAuthenticated$.pipe(
    take(1),
    map((isAuthenticated) => {
      if (!isAuthenticated) {
        const targetUrl = state.url ? router.serializeUrl(router.parseUrl(state.url)) : '/';
        void authService.loginWithRedirect(targetUrl);
        return false;
      }

      const requiredRealmRoles = asArray(roleData?.realmRole);
      const requiredClientRoles = asArray(roleData?.clientRole);

      const hasAnyRealmRole =
        requiredRealmRoles.length === 0 ||
        requiredRealmRoles.some((r) => authService.hasRealmRole(r));

      const hasAnyClientRole =
        requiredClientRoles.length === 0 ||
        requiredClientRoles.some((cr) => authService.hasClientRole(cr.clientId, cr.role));

      const anyRoleRequired = requiredRealmRoles.length > 0 || requiredClientRoles.length > 0;
      const isAuthorized = !anyRoleRequired || hasAnyRealmRole || hasAnyClientRole;

      return isAuthorized ? true : router.parseUrl('/forbidden');
    }),
  );
};
