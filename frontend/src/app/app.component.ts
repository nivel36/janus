import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from './auth/auth.service';
import { ClockComponent } from './shared/ui/clock/clock.component';
import { CardComponent } from './shared/ui/card/card.component';
import { TimelogTableComponent } from './timelog/timelog-table.component';
import { ButtonComponent } from './shared/ui/button/button.component';

@Component({
	selector: 'app-root',
	standalone: true,
	imports: [
		AsyncPipe,
		FormsModule,
		TranslatePipe,
		ClockComponent,
		CardComponent,
		TimelogTableComponent,
		ButtonComponent
	],
	templateUrl: './app.component.html',
	styleUrl: './app.component.css'
})
export class AppComponent {
	title = 'frontend';
	private readonly authService = inject(AuthService);
	credentials = {
		username: '',
		password: ''
	};
	errorMessage = '';
	isLoading = false;
	readonly isAuthenticated$ = this.authService.isAuthenticated$;

	onLogin(): void {
		this.errorMessage = '';
		this.isLoading = true;
		this.authService.login(this.credentials.username, this.credentials.password).subscribe({
			next: () => {
				this.isLoading = false;
				this.credentials.password = '';
			},
			error: (error) => {
				this.isLoading = false;
				this.errorMessage = error?.error?.detail ?? 'Unable to sign in. Please try again.';
			}
		});
	}
}
