/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';

import { ScheduleApiService, SchedulePage } from '../../services/schedule-api.service';
import { Schedule } from '../../models/schedule';
import { PaginatorComponent } from '../../../../shared/ui/paginator/paginator.component';
import { ButtonComponent } from '../../../../shared/ui/button/button.component';
import {
  DEFAULT_LIST_PAGE,
  DEFAULT_LIST_PAGE_SIZE,
  emptyListPage,
  normalizeListPage,
  normalizeListQuery,
  synchronizeListPage,
} from '../../../../shared/utils/list-query-params.util';

import {
  AsyncEmptyDirective,
  AsyncErrorDirective,
  AsyncLoadingDirective,
  AsyncStateComponent,
} from '../../../../shared/ui/async-state/async-state.component';

@Component({
  selector: 'app-schedule-table',
  imports: [
    ButtonComponent,
    AsyncStateComponent,
    AsyncLoadingDirective,
    AsyncErrorDirective,
    AsyncEmptyDirective,
    TranslatePipe,
    PaginatorComponent,
  ],
  templateUrl: './schedule-table.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ScheduleTableComponent {
  private readonly scheduleApiService = inject(ScheduleApiService);

  readonly query = input('', { transform: normalizeListQuery });
  readonly page = input(DEFAULT_LIST_PAGE, { transform: normalizeListPage });
  readonly refreshToken = input(0);
  readonly pageSize = input(DEFAULT_LIST_PAGE_SIZE);
  readonly pageChange = output<number>();

  protected readonly currentPage = this.page;

  protected readonly schedulesResource = rxResource<
    SchedulePage,
    { refreshToken: number; page: number; pageSize: number; query: string }
  >({
    params: () => ({
      refreshToken: this.refreshToken(),
      page: this.currentPage(),
      pageSize: this.pageSize(),
      query: this.query(),
    }),
    stream: ({ params }) =>
      this.scheduleApiService.search(params.page - 1, params.pageSize, params.query),
    defaultValue: emptyListPage<Schedule>(),
  });

  protected readonly schedules = computed(() => this.schedulesResource.value().items);

  protected readonly totalItems = computed(() => this.schedulesResource.value().totalItems);

  protected readonly pagedSchedules = computed(() => this.schedules());

  private readonly pageSyncEffect = synchronizeListPage(
    this.currentPage,
    this.totalItems,
    this.schedulesResource.isLoading,
    (page) => this.pageChange.emit(page),
    this.pageSize,
  );

  protected readonly isEmpty = computed(
    () =>
      !this.schedulesResource.isLoading() &&
      this.schedulesResource.error() === undefined &&
      this.totalItems() === 0,
  );

  protected onPageChange(page: number): void {
    this.pageChange.emit(page);
  }
}
