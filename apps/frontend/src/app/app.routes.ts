/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const appRoutes: Routes = [
  {
    path: 'forbidden',
    loadComponent: () =>
      import('./core/error-pages/forbidden/forbidden.component').then((m) => m.ForbiddenComponent),
  },
  {
    path: 'user-preferences',
    canActivate: [authGuard],
    data: {
      realmRole: ['JANUS_EMPLOYEE', 'JANUS_USER', 'JANUS_ADMIN'],
    },
    loadComponent: () =>
      import('./core/user/pages/user-preferences-page.component').then(
        (m) => m.UserPreferencesPageComponent,
      ),
  },
  {
    path: 'application-settings',
    canActivate: [authGuard],
    data: {
      realmRole: ['JANUS_EMPLOYEE', 'JANUS_USER', 'JANUS_ADMIN'],
    },
    loadComponent: () =>
      import('./features/applicationsettings/pages/application-settings-page.component').then(
        (m) => m.ApplicationSettingsPageComponent,
      ),
  },

  {
    path: 'schedules',
    canActivate: [authGuard],
    data: {
      realmRole: ['JANUS_EMPLOYEE', 'JANUS_USER', 'JANUS_ADMIN'],
    },
    loadComponent: () =>
      import('./features/schedules/pages/schedules-page.component').then(
        (m) => m.SchedulesPageComponent,
      ),
  },
  {
    path: 'worksites/new',
    canActivate: [authGuard],
    data: {
      realmRole: ['JANUS_EMPLOYEE', 'JANUS_USER', 'JANUS_ADMIN'],
    },
    loadComponent: () =>
      import('./features/worksites/pages/worksite-create-page/worksite-create-page.component').then(
        (m) => m.WorksiteCreatePageComponent,
      ),
  },
  {
    path: 'worksites/:code/edit',
    canActivate: [authGuard],
    data: {
      realmRole: ['JANUS_EMPLOYEE', 'JANUS_USER', 'JANUS_ADMIN'],
    },
    loadComponent: () =>
      import('./features/worksites/pages/worksite-edit-page/worksite-edit-page.component').then(
        (m) => m.WorksiteEditPageComponent,
      ),
  },
  {
    path: 'worksites/:code',
    canActivate: [authGuard],
    data: {
      realmRole: ['JANUS_EMPLOYEE', 'JANUS_USER', 'JANUS_ADMIN'],
    },
    loadComponent: () =>
      import('./features/worksites/pages/worksite-detail-page/worksite-detail-page.component').then(
        (m) => m.WorksiteDetailPageComponent,
      ),
  },
  {
    path: 'worksites',
    canActivate: [authGuard],
    data: {
      realmRole: ['JANUS_EMPLOYEE', 'JANUS_USER', 'JANUS_ADMIN'],
    },
    loadComponent: () =>
      import('./features/worksites/pages/worksites-page/worksites-page.component').then(
        (m) => m.WorksitesPageComponent,
      ),
  },
  {
    path: '',
    canActivate: [authGuard],
    data: {
      realmRole: ['JANUS_EMPLOYEE', 'JANUS_USER', 'JANUS_ADMIN'],
    },
    loadComponent: () =>
      import('./features/dashboard/pages/dashboard-page.component').then(
        (m) => m.DashboardPageComponent,
      ),
  },
];
