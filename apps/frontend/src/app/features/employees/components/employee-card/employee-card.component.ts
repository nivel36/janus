/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { inject, Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { AvatarComponent } from '../../../../shared/ui/avatar/avatar.component';
import { ID_GENERATOR } from '../../../../shared/services/id-generator.service';

@Component({
  selector: 'app-employee-card',
  standalone: true,
  imports: [AvatarComponent, TranslatePipe],
  templateUrl: './employee-card.component.html',
  styleUrl: './employee-card.component.css',
})
export class EmployeeCardComponent {
  readonly titleElementId = `${inject(ID_GENERATOR).generate('employee-card')}-title`;

  readonly fullName = input<string | null>(null);
  readonly location = input<string>('Barcelona Headquarters');
  readonly todaysHour = input<string>('9:00 - 17:30');
  readonly avatarSrc = input<string>('assets/images/user.png');
  readonly avatarAlt = input<string>('User avatar');
}
