import { Routes } from '@angular/router';
import { AUTH_ROUTES } from './auth/auth.routes';

export const appRoutes: Routes = [
	{
		path: '',
		loadComponent: () =>
			import('./dashboard/dashboard-page.component')
				.then(m => m.DashboardPageComponent),
	},
	...AUTH_ROUTES,
];
