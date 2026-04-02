/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, Input } from '@angular/core';
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
  @Input() fullName: string | null = null;
  @Input() location = 'Barcelona Headquarters';
  @Input() todaysHour = '9:00 - 17:30';
  @Input() avatarSrc = 'assets/images/user.png';
  @Input() avatarAlt = 'User avatar';
}
