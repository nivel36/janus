import { Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { ClockComponent } from './shared/ui/clock/clock.component';
import { CardComponent } from './shared/ui/card/card.component';
import { TimelogTableComponent } from './timelog/timelog-table.component';
import { ButtonComponent } from './shared/ui/button/button.component';

@Component({
        selector: 'app-root',
        standalone: true,
        imports: [TranslatePipe, ClockComponent, CardComponent, TimelogTableComponent, ButtonComponent],
        templateUrl: './app.component.html',
        styleUrl: './app.component.css'
})
export class AppComponent {
	title = 'frontend';
}
