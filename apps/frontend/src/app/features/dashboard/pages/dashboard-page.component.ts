/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { AsyncPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { filter, map, distinctUntilChanged } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AuthService } from '../../../core/auth/auth.service';
import { CardComponent } from '../../../shared/ui/card/card.component';
import { TimelogTableComponent } from '../../timelogs/components/timelog-table/timelog-table.component';
import { TimelogClockCardComponent } from '../../timelogs/components/timelog-clock-card/timelog-clock-card.component';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [
    AsyncPipe,
    FormsModule,
    TranslatePipe,
    CardComponent,
    TimelogTableComponent,
    TimelogClockCardComponent,
  ],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.css',
})
export class DashboardPageComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  /**
   * Email of the currently authenticated employee.
   *
   * This value is resolved from the authentication context
   * and used to feed child components.
   */
  employeeEmail!: string;

  /**
   * Token used to trigger refresh in the timelog table.
   *
   * Incrementing this value forces the table component
   * to reload its data.
   */
  tableRefreshToken = 0;

  /**
   * Observable indicating whether the user is authenticated.
   */
  readonly isAuthenticated$ = this.authService.isAuthenticated$;

  /**
   * Observable containing the username (email) of the user.
   */
  readonly username$ = this.authService.username$;

  /**
   * Indicates whether the user has permission to perform
   * clock in / clock out actions.
   */
  readonly canClockInOut$ = this.authService.permissions$.pipe(
    map((permissions) => permissions.realmRoles.includes('JANUS_USER')),
    distinctUntilChanged(),
  );

  /**
   * Full name of the authenticated user.
   *
   * Built from the claims provided by the identity provider.
   */
  readonly fullName$ = this.authService.claims$.pipe(
    map((claims) => `${claims?.given_name ?? ''} ${claims?.family_name ?? ''}`.trim()),
    distinctUntilChanged(),
  );

  /**
   * Initializes the component by resolving the employee email
   * from the authentication context.
   *
   * The value is stored locally and passed to child components.
   */
  ngOnInit(): void {
    this.username$
      .pipe(
        filter((username): username is string => !!username),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((username) => {
        this.employeeEmail = username;
      });
  }

  /**
   * Handles the completion of a clock action emitted
   * by the child clock card component.
   *
   * Triggers a refresh of the timelog table.
   */
  onClockActionDone(): void {
    this.tableRefreshToken += 1;
  }
}
