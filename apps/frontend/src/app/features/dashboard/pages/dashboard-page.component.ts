/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';

import { CurrentUserFacade } from '../../../core/user/services/current-user.facade';
import { PageTemplateComponent } from '../../../core/layout/page-template/page-template.component';
import { TimelogClockCardComponent } from '../../timelogs/components/timelog-clock-card/timelog-clock-card.component';
import { TimelogTableComponent } from '../../timelogs/components/timelog-table/timelog-table.component';
import { EmployeeCardComponent } from '../../employees/components/employee-card/employee-card.component';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-dashboard-page',
  imports: [
    PageTemplateComponent,
    EmployeeCardComponent,
    TimelogTableComponent,
    TimelogClockCardComponent,
  ],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.css',
})
export class DashboardPageComponent {
  private readonly currentUser = inject(CurrentUserFacade);

  readonly tableRefreshToken = signal(0);

  readonly employeeEmail = this.currentUser.email;
  readonly fullName = this.currentUser.fullName;

  onClockActionDone(): void {
    this.tableRefreshToken.update((token) => token + 1);
  }
}
