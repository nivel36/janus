import { Routes } from '@angular/router';
import { AUTH_ROUTES } from './auth/auth.routes';
import { authGuard } from './core/auth/auth.guard';
import { KEYCLOAK_CLIENT_ID } from './core/auth/keycloak.constants';

export const appRoutes: Routes = [
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
  ...AUTH_ROUTES,
];
