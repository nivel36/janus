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

import { WorksiteApiService, WorksitePage } from '../../services/worksite-api.service';
import { PaginatorComponent } from '../../../../shared/ui/paginator/paginator.component';
import { ChipComponent } from '../../../../shared/ui/chip/chip.component';
import { retryTransientHttpErrors } from '../../../../shared/utils/http-retry.util';

@Component({
  selector: 'app-worksite-table',
  standalone: true,
  imports: [TranslatePipe, PaginatorComponent, ChipComponent],
  templateUrl: './worksite-table.component.html',
  styleUrl: './worksite-table.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorksiteTableComponent {
  private static readonly PAGE_SIZE = 5;

  private readonly worksiteApiService = inject(WorksiteApiService);

  readonly query = input('');
  readonly refreshToken = input(0);

  protected readonly currentPage = signal(1);

  protected readonly normalizedQuery = computed(() => this.query().trim());

  protected readonly worksitesResource = rxResource<
    WorksitePage,
    { refreshToken: number; page: number; query: string }
  >({
    params: () => ({
      refreshToken: this.refreshToken(),
      page: this.currentPage(),
      query: this.normalizedQuery(),
    }),
    stream: ({ params }) =>
      this.worksiteApiService
        .search(params.page - 1, WorksiteTableComponent.PAGE_SIZE, params.query)
        .pipe(retryTransientHttpErrors()),
    defaultValue: {
      items: [],
      totalItems: 0,
      page: 0,
      pageSize: WorksiteTableComponent.PAGE_SIZE,
      totalPages: 0,
    },
  });

  protected readonly worksites = computed(() => this.worksitesResource.value().items);

  protected readonly totalItems = computed(() => this.worksitesResource.value().totalItems);

  protected readonly pagedWorksites = computed(() => this.worksites());

  private readonly resetPageOnQueryChangeEffect = effect(() => {
    this.normalizedQuery();
    this.currentPage.set(1);
  });

  private readonly pageSyncEffect = effect(() => {
    if (this.worksitesResource.isLoading()) {
      return;
    }

    const maxPage = Math.max(1, Math.ceil(this.totalItems() / WorksiteTableComponent.PAGE_SIZE));

    if (this.currentPage() > maxPage) {
      this.currentPage.set(maxPage);
    }
  });

  protected readonly isEmpty = computed(
    () =>
      !this.worksitesResource.isLoading() &&
      this.worksitesResource.error() === undefined &&
      this.totalItems() === 0,
  );

  protected onPageChange(page: number): void {
    this.currentPage.set(page);
  }

  protected get pageSize(): number {
    return WorksiteTableComponent.PAGE_SIZE;
  }
}
