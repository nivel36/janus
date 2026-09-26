import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';

import { TranslatePipe } from '@ngx-translate/core';

import { CurrentUserFacade } from '../../../../core/user/services/current-user.facade';
import { DurationPipe } from '../../../../shared/pipes/duration.pipe';

import { TimeLogService, TimeLogPage } from '../../services/timelog-api.service';
import { FALLBACK_LANGUAGE } from '../../../../core/i18n/language.util';
import { PaginatorComponent } from '../../../../shared/ui/paginator/paginator.component';
import { ButtonComponent } from '../../../../shared/ui/button/button.component';
import {
  DEFAULT_LIST_PAGE,
  DEFAULT_LIST_PAGE_SIZE,
  emptyListPage,
  synchronizeListPage,
} from '../../../../shared/utils/list-query-params.util';
import { TimeLog } from '../../models/timelog';

import {
  AsyncEmptyDirective,
  AsyncErrorDirective,
  AsyncLoadingDirective,
  AsyncStateComponent,
} from '../../../../shared/ui/async-state/async-state.component';

@Component({
  selector: 'app-timelog-table',
  standalone: true,
  imports: [
    ButtonComponent,
    AsyncStateComponent,
    AsyncLoadingDirective,
    AsyncErrorDirective,
    AsyncEmptyDirective,
    TranslatePipe,
    DatePipe,
    DurationPipe,
    PaginatorComponent,
  ],
  templateUrl: './timelog-table.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TimelogTableComponent {
  private readonly timeLogService = inject(TimeLogService);
  private readonly currentUser = inject(CurrentUserFacade);

  readonly refreshToken = input(0);

  protected readonly userLocale = computed(
    () => this.currentUser.preferences()?.locale ?? FALLBACK_LANGUAGE,
  );

  protected readonly userTimezone = computed(
    () => this.currentUser.preferences()?.defaultTimezone ?? undefined,
  );

  protected readonly timeFormat = computed(() =>
    this.currentUser.preferences()?.timeFormat === 'H12' ? 'hh:mm a' : 'HH:mm',
  );

  /**
   * Current visible page in the UI (1-based).
   */
  protected readonly currentPage = signal(DEFAULT_LIST_PAGE);

  protected readonly timelogsResource = rxResource<
    TimeLogPage,
    { refreshToken: number; page: number }
  >({
    params: () => ({
      refreshToken: this.refreshToken(),
      page: this.currentPage(),
    }),
    stream: ({ params }) =>
      this.timeLogService.search(params.page - DEFAULT_LIST_PAGE, DEFAULT_LIST_PAGE_SIZE),
    defaultValue: emptyListPage<TimeLog>(),
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

  private readonly pageSyncEffect = synchronizeListPage(
    this.currentPage,
    this.totalItems,
    this.timelogsResource.isLoading,
    (page) => this.currentPage.set(page),
  );

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
    return DEFAULT_LIST_PAGE_SIZE;
  }
}
