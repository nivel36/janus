/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { CardComponent } from '../../../../shared/ui/card/card.component';

@Component({
  selector: 'app-employee-card',
  standalone: true,
  imports: [CardComponent, TranslatePipe],
  templateUrl: './employee-card.component.html',
  styleUrl: './employee-card.component.css',
})
export class EmployeeCardComponent {
  readonly fullName = input<string | null>(null);
  readonly location = input<string>('Barcelona Headquarters');
  readonly todaysHour = input<string>('9:00 - 17:30');
  readonly avatarSrc = input<string>('assets/images/user.png');
  readonly avatarAlt = input<string>('User avatar');
}
