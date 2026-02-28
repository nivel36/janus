import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, take } from 'rxjs';

import { AuthService } from './auth.service';

interface RouteRoleData {
  realmRole?: string;
  clientRole?: {
    clientId: string;
    role: string;
  };
}

export const authGuard: CanActivateFn = (route, state): ReturnType<CanActivateFn> => {
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

      const hasRealmRole = roleData?.realmRole
        ? authService.hasRealmRole(roleData.realmRole)
        : true;
      const hasClientRole = roleData?.clientRole
        ? authService.hasClientRole(roleData.clientRole.clientId, roleData.clientRole.role)
        : true;

      if (hasRealmRole && hasClientRole) {
        return true;
      }

      return router.parseUrl('/login');
    }),
  );
};
