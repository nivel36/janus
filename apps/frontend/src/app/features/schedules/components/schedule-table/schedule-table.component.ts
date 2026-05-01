/**
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';

import { ScheduleApiService, SchedulePage } from '../../services/schedule-api.service';
import { PaginatorComponent } from '../../../../shared/ui/paginator/paginator.component';
import { retryTransientHttpErrors } from '../../../../shared/utils/http-retry.util';

@Component({
  selector: 'app-schedule-table',
  standalone: true,
  imports: [TranslatePipe, PaginatorComponent],
  templateUrl: './schedule-table.component.html',
  styleUrl: './schedule-table.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ScheduleTableComponent {
  private static readonly PAGE_SIZE = 5;

  private readonly scheduleApiService = inject(ScheduleApiService);

  readonly query = input('');
  readonly refreshToken = input(0);

  protected readonly currentPage = signal(1);

  protected readonly normalizedQuery = computed(() => this.query().trim());

  protected readonly schedulesResource = rxResource<
    SchedulePage,
    { refreshToken: number; page: number; query: string }
  >({
    params: () => ({
      refreshToken: this.refreshToken(),
      page: this.currentPage(),
      query: this.normalizedQuery(),
    }),
    stream: ({ params }) =>
      this.scheduleApiService
        .search(params.page - 1, ScheduleTableComponent.PAGE_SIZE, params.query)
        .pipe(retryTransientHttpErrors()),
    defaultValue: {
      items: [],
      totalItems: 0,
      page: 0,
      pageSize: ScheduleTableComponent.PAGE_SIZE,
      totalPages: 0,
    },
  });

  protected readonly schedules = computed(() => this.schedulesResource.value().items);

  protected readonly totalItems = computed(() => this.schedulesResource.value().totalItems);

  protected readonly pagedSchedules = computed(() => this.schedules());

  private readonly resetPageOnQueryChangeEffect = effect(() => {
    this.normalizedQuery();
    this.currentPage.set(1);
  });

  private readonly pageSyncEffect = effect(() => {
    if (this.schedulesResource.isLoading()) {
      return;
    }

    const maxPage = Math.max(1, Math.ceil(this.totalItems() / ScheduleTableComponent.PAGE_SIZE));

    if (this.currentPage() > maxPage) {
      this.currentPage.set(maxPage);
    }
  });

  protected readonly isEmpty = computed(
    () =>
      !this.schedulesResource.isLoading() &&
      this.schedulesResource.error() === undefined &&
      this.totalItems() === 0,
  );

  protected onPageChange(page: number): void {
    this.currentPage.set(page);
  }

  protected get pageSize(): number {
    return ScheduleTableComponent.PAGE_SIZE;
  }
}
