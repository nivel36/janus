/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, Input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { CardComponent } from '../card/card.component';

@Component({
  selector: 'app-user-card',
  standalone: true,
  imports: [CardComponent, TranslatePipe],
  templateUrl: './user-card.component.html',
  styleUrl: './user-card.component.css',
})
export class UserCardComponent {
  @Input() fullName: string | null = null;
  @Input() location = 'Barcelona Headquarters';
  @Input() todaysHour = '9:00 - 17:30';
  @Input() avatarSrc = 'assets/images/user.png';
  @Input() avatarAlt = 'User avatar';
}
