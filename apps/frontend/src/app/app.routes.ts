/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Routes } from '@angular/router';
import { authChildGuard } from './core/auth/auth.guard';

export const appRoutes: Routes = [
  {
    path: 'forbidden',
    loadComponent: () =>
      import('./core/error-pages/forbidden/forbidden.component').then((m) => m.ForbiddenComponent),
  },
  {
    path: '',
    canActivateChild: [authChildGuard],
    data: {
      realmRole: ['JANUS_EMPLOYEE', 'JANUS_USER', 'JANUS_ADMIN'],
    },
    children: [
      {
        path: 'user-preferences',
        loadComponent: () =>
          import('./core/user/pages/user-preferences-page.component').then(
            (m) => m.UserPreferencesPageComponent,
          ),
      },
      {
        path: 'application-settings',
        loadComponent: () =>
          import('./features/applicationsettings/pages/application-settings-page.component').then(
            (m) => m.ApplicationSettingsPageComponent,
          ),
      },
      {
        path: 'schedules',
        loadComponent: () =>
          import('./features/schedules/pages/schedules-page.component').then(
            (m) => m.SchedulesPageComponent,
          ),
      },
      {
        path: 'worksites/new',
        loadComponent: () =>
          import('./features/worksites/pages/worksite-create-page/worksite-create-page.component').then(
            (m) => m.WorksiteCreatePageComponent,
          ),
      },
      {
        path: 'worksites/:code/edit',
        loadComponent: () =>
          import('./features/worksites/pages/worksite-edit-page/worksite-edit-page.component').then(
            (m) => m.WorksiteEditPageComponent,
          ),
      },
      {
        path: 'worksites/:code',
        loadComponent: () =>
          import('./features/worksites/pages/worksite-detail-page/worksite-detail-page.component').then(
            (m) => m.WorksiteDetailPageComponent,
          ),
      },
      {
        path: 'worksites',
        loadComponent: () =>
          import('./features/worksites/pages/worksites-page/worksites-page.component').then(
            (m) => m.WorksitesPageComponent,
          ),
      },
      {
        path: '',
        loadComponent: () =>
          import('./features/dashboard/pages/dashboard-page.component').then(
            (m) => m.DashboardPageComponent,
          ),
      },
    ],
  },
];
