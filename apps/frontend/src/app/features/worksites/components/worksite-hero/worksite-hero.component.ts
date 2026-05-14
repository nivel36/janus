/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faBuilding, faCircle } from '@fortawesome/free-solid-svg-icons';
import { TranslatePipe } from '@ngx-translate/core';

import { ChipComponent } from '../../../../shared/ui/chip/chip.component';
import { Worksite } from '../../models/worksite';

@Component({
  selector: 'app-worksite-hero',
  standalone: true,
  imports: [ChipComponent, FontAwesomeModule, TranslatePipe],
  templateUrl: './worksite-hero.component.html',
  styleUrl: './worksite-hero.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorksiteHeroComponent {
  readonly worksite = input.required<Worksite>();

  protected readonly faBuilding = faBuilding;
  protected readonly faCircle = faCircle;
}
