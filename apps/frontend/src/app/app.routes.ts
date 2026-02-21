import { Routes } from '@angular/router';
import { AUTH_ROUTES } from './auth/auth.routes';
import { authGuard } from './core/auth/auth.guard';

export const appRoutes: Routes = [
	{
		path: '',
		canActivate: [authGuard],
		loadComponent: () =>
			import('./features/dashboard/pages/dashboard-page.component')
				.then(m => m.DashboardPageComponent),
	},
	...AUTH_ROUTES,
];
