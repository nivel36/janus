/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import {
  faCalendarDays,
  faClock,
  faExclamationTriangle,
  faUsers,
} from '@fortawesome/free-solid-svg-icons';

import { CardComponent } from '../../../../shared/ui/card/card.component';
import { SummaryCardComponent } from '../../../../shared/ui/summary-card/summary-card.component';

@Component({
  selector: 'app-worksite-summary-panel',
  standalone: true,
  imports: [CardComponent, SummaryCardComponent, TranslatePipe],
  templateUrl: './worksite-summary-panel.component.html',
  styleUrl: './worksite-summary-panel.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorksiteSummaryPanelComponent {
  protected readonly faCalendarDays = faCalendarDays;
  protected readonly faClock = faClock;
  protected readonly faExclamationTriangle = faExclamationTriangle;
  protected readonly faUsers = faUsers;
}
