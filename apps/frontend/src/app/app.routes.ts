/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { KEYCLOAK_CLIENT_ID } from './core/auth/keycloak.constants';

export const appRoutes: Routes = [
  {
    path: 'forbidden',
    loadComponent: () =>
      import('./core/error-pages/forbidden/forbidden.component').then((m) => m.ForbiddenComponent),
  },
  {
    path: '',
    canActivate: [authGuard],
    data: {
      clientRole: { clientId: KEYCLOAK_CLIENT_ID, role: 'JANUS_USER' },
    },
    loadComponent: () =>
      import('./features/dashboard/pages/dashboard-page.component').then(
        (m) => m.DashboardPageComponent,
      ),
  },
];
