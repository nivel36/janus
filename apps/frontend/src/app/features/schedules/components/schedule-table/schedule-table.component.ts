import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';

import { PaginatorComponent } from '../../../../shared/ui/paginator/paginator.component';
import { retryTransientHttpErrors } from '../../../../shared/utils/http-retry.util';
import { ScheduleApiService, SchedulePage } from '../../services/schedule-api.service';

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

  protected readonly currentPage = signal(1);

  protected readonly schedulesResource = rxResource<SchedulePage, { page: number }>({
    params: () => ({ page: this.currentPage() }),
    stream: ({ params }) =>
      this.scheduleApiService
        .findAll(params.page - 1, ScheduleTableComponent.PAGE_SIZE)
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
