import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
} from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';

import { TranslatePipe } from '@ngx-translate/core';

import { CurrentUserFacade } from '../../../../core/user/services/current-user.facade';
import { DurationPipe } from '../../../../shared/pipes/duration.pipe';

import { TimeLogService, TimeLogPage } from '../../services/timelog-api.service';
import { FALLBACK_LANGUAGE } from '../../../../core/i18n/language.util';
import { PaginatorComponent } from '../../../../shared/ui/paginator/paginator.component';
import { retryTransientHttpErrors } from '../../../../shared/utils/http-retry.util';

@Component({
  selector: 'app-timelog-table',
  standalone: true,
  imports: [TranslatePipe, DatePipe, DurationPipe, PaginatorComponent],
  templateUrl: './timelog-table.component.html',
  styleUrl: './timelog-table.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TimelogTableComponent {
  private static readonly PAGE_SIZE = 5;

  private readonly timeLogService = inject(TimeLogService);
  private readonly currentUser = inject(CurrentUserFacade);

  readonly employeeEmail = input.required<string>();
  readonly refreshToken = input(0);

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

  protected readonly userLocale = computed(
    () => this.currentUserSignal().preferences?.locale ?? FALLBACK_LANGUAGE,
  );

  protected readonly userTimezone = computed(
    () => this.currentUserSignal().preferences?.defaultTimezone ?? undefined,
  );

  protected readonly timeFormat = computed(() =>
    this.currentUserSignal().preferences?.timeFormat === 'H12' ? 'hh:mm a' : 'HH:mm',
  );

  /**
   * Current visible page in the UI (1-based).
   */
  protected readonly currentPage = signal(1);

  protected readonly timelogsResource = rxResource<
    TimeLogPage,
    { employeeEmail: string; refreshToken: number; page: number }
  >({
    params: () => ({
      employeeEmail: this.employeeEmail(),
      refreshToken: this.refreshToken(),
      page: this.currentPage(),
    }),
    stream: ({ params }) =>
      this.timeLogService
        .searchByEmployee(params.employeeEmail, params.page - 1, TimelogTableComponent.PAGE_SIZE)
        .pipe(retryTransientHttpErrors()),
    defaultValue: {
      items: [],
      totalItems: 0,
      page: 0,
      pageSize: TimelogTableComponent.PAGE_SIZE,
      totalPages: 0,
    },
  });

  /**
   * Current page items returned by backend.
   */
  protected readonly timelogs = computed(() => this.timelogsResource.value().items);

  /**
   * Total number of rows available in backend.
   */
  protected readonly totalItems = computed(() => this.timelogsResource.value().totalItems);

  /**
   * In server-side pagination, the backend already returns the current page slice.
   */
  protected readonly pagedTimelogs = computed(() => this.timelogs());

  private readonly pageSyncEffect = effect(() => {
    if (this.timelogsResource.isLoading()) {
      return;
    }

    const totalItems = this.totalItems();
    const maxPage = Math.max(1, Math.ceil(totalItems / TimelogTableComponent.PAGE_SIZE));

    if (this.currentPage() > maxPage) {
      this.currentPage.set(maxPage);
    }
  });

  protected readonly isEmpty = computed(
    () =>
      !this.timelogsResource.isLoading() &&
      this.timelogsResource.error() === undefined &&
      this.totalItems() === 0,
  );

  protected onPageChange(page: number): void {
    this.currentPage.set(page);
  }

  protected get pageSize(): number {
    return TimelogTableComponent.PAGE_SIZE;
  }
}
