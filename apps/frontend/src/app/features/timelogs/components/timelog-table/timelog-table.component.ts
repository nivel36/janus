/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  resource,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { firstValueFrom } from 'rxjs';
import { TranslatePipe } from '@ngx-translate/core';

import { CurrentUserFacade } from '../../../../core/user/services/current-user.facade';
import { DurationPipe } from '../../../../shared/pipes/duration.pipe';
import { TimeLog } from '../../models/timelog';
import { TimeLogService } from '../../services/timelog-api.service';
import { FALLBACK_LANGUAGE } from '../../../../core/i18n/language.util';

/**
 * Displays the time logs of a specific employee in a table.
 *
 * <p>
 * The component retrieves the current user's preferences in order to format
 * dates, times, locale, and timezone consistently with the user's settings.
 * It also loads the employee time logs reactively using an Angular resource,
 * allowing the table to refresh whenever the employee email or refresh token changes.
 * </p>
 */
@Component({
  selector: 'app-timelog-table',
  standalone: true,
  imports: [TranslatePipe, DatePipe, DurationPipe],
  templateUrl: './timelog-table.component.html',
  styleUrl: './timelog-table.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TimelogTableComponent {
  /**
   * Service used to retrieve time log data from the backend API.
   */
  private readonly timeLogService = inject(TimeLogService);

  /**
   * Facade exposing the authenticated user's information and preferences.
   */
  private readonly currentUser = inject(CurrentUserFacade);

  /**
   * Email address of the employee whose time logs must be displayed.
   *
   * <p>This input is required.</p>
   */
  readonly employeeEmail = input.required<string>();

  /**
   * Numeric token used to force reloading the resource when its value changes.
   *
   * <p>
   * This is useful when the parent component needs to trigger a manual refresh
   * without changing the employee email.
   * </p>
   */
  readonly refreshToken = input(0);

  /**
   * Signal containing the current authenticated user state.
   *
   * <p>
   * It is created from the user facade observable so it can be consumed
   * synchronously from computed signals in the component.
   * </p>
   */
  private readonly currentUserSignal = toSignal(this.currentUser.currentUser$, {
    initialValue: {
      username: null,
      email: null,
      fullName: '',
      isAuthenticated: false,
      isAdmin: false,
      isUser: false,
      isEmployee: false,
      preferences: null,
    },
  });

  /**
   * Locale to be used when formatting dates and times.
   *
   * <p>
   * If the user has not configured a locale preference, the application fallback
   * language is used.
   * </p>
   */
  protected readonly userLocale = computed(
    () => this.currentUserSignal().preferences?.locale ?? FALLBACK_LANGUAGE,
  );

  /**
   * Time zone to be used when formatting date and time values.
   *
   * <p>
   * If no default timezone is configured in the user preferences, Angular will
   * use its default behavior.
   * </p>
   */
  protected readonly userTimezone = computed(
    () => this.currentUserSignal().preferences?.defaultTimezone ?? undefined,
  );

  /**
   * Angular date pipe format string used to render time values.
   *
   * <p>
   * The format depends on the user's preferred time format:
   * <ul>
   *   <li><code>hh:mm a</code> for 12-hour format</li>
   *   <li><code>HH:mm</code> for 24-hour format</li>
   * </ul>
   * </p>
   */
  protected readonly timeFormat = computed(() =>
    this.currentUserSignal().preferences?.timeFormat === 'H12' ? 'hh:mm a' : 'HH:mm',
  );

  /**
   * Reactive resource responsible for loading the employee time logs.
   *
   * <p>
   * The resource is reloaded whenever {@link employeeEmail} or {@link refreshToken}
   * changes. The refresh token is included only to trigger reloads and is not
   * sent to the backend service.
   * </p>
   */
  protected readonly timelogsResource = resource<
    TimeLog[],
    { employeeEmail: string; refreshToken: number }
  >({
    params: () => ({
      employeeEmail: this.employeeEmail(),
      refreshToken: this.refreshToken(),
    }),
    loader: ({ params }) =>
      firstValueFrom(this.timeLogService.searchByEmployee(params.employeeEmail)),
    defaultValue: [],
  });

  /**
   * Computed signal containing the loaded list of time logs.
   */
  protected readonly timelogs = computed(() => this.timelogsResource.value());

  /**
   * Indicates whether the component is in the empty state.
   *
   * <p>
   * The state is considered empty when the resource is not loading, no error
   * has occurred, and the loaded time log list is empty.
   * </p>
   */
  protected readonly isEmpty = computed(
    () =>
      !this.timelogsResource.isLoading() &&
      this.timelogsResource.error() === undefined &&
      this.timelogs().length === 0,
  );
}
