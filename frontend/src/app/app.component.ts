import { Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { ClockComponent } from './shared/ui/clock/clock.component';

@Component({
	selector: 'app-root',
	standalone: true,
	imports: [TranslatePipe, ClockComponent],
	templateUrl: './app.component.html',
	styleUrl: './app.component.css'
})
export class AppComponent {
	title = 'frontend';
}
