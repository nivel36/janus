import { Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { ClockComponent } from './shared/ui/clock/clock.component';
import { CardComponent } from './shared/ui/card/card.component';

@Component({
	selector: 'app-root',
	standalone: true,
	imports: [TranslatePipe, ClockComponent, CardComponent],
	templateUrl: './app.component.html',
	styleUrl: './app.component.css'
})
export class AppComponent {
	title = 'frontend';
}
