/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CurrentUserFacade } from '../../../core/auth/current-user.facade';
import { PageTemplateComponent } from '../../../shared/layout/page-template/page-template.component';
import { TimelogClockCardComponent } from '../../timelogs/components/timelog-clock-card/timelog-clock-card.component';
import { TimelogTableComponent } from '../../timelogs/components/timelog-table/timelog-table.component';
import { UserCardComponent } from '../../users/components/user-card/user-card.component';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [
    AsyncPipe,
    FormsModule,
    PageTemplateComponent,
    UserCardComponent,
    TimelogTableComponent,
    TimelogClockCardComponent,
  ],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.css',
})
export class DashboardPageComponent {
  private readonly currentUser = inject(CurrentUserFacade);

  tableRefreshToken = 0;

  readonly currentUser$ = this.currentUser.currentUser$;
  readonly isAuthenticated$ = this.currentUser.isAuthenticated$;
  readonly employeeEmail$ = this.currentUser.email$;
  readonly fullName$ = this.currentUser.fullName$;
  readonly canClockInOut$ = this.currentUser.isUser$;

  onClockActionDone(): void {
    this.tableRefreshToken += 1;
  }
}
