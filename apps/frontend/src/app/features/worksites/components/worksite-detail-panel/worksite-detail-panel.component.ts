/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { CardComponent } from '../../../../shared/ui/card/card.component';
import { ChipComponent } from '../../../../shared/ui/chip/chip.component';
import { Worksite } from '../../models/worksite';

@Component({
  selector: 'app-worksite-detail-panel',
  standalone: true,
  imports: [CardComponent, ChipComponent, TranslatePipe],
  templateUrl: './worksite-detail-panel.component.html',
  styleUrl: './worksite-detail-panel.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorksiteDetailPanelComponent {
  readonly worksite = input.required<Worksite>();
}
