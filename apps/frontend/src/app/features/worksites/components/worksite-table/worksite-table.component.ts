/**
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  output,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { WorksiteApiService, WorksitePage } from '../../services/worksite-api.service';
import { Worksite } from '../../models/worksite';
import { PaginatorComponent } from '../../../../shared/ui/paginator/paginator.component';
import { ChipComponent } from '../../../../shared/ui/chip/chip.component';
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
  selector: 'app-worksite-table',
  standalone: true,
  imports: [
    AsyncStateComponent,
    AsyncLoadingDirective,
    AsyncErrorDirective,
    AsyncEmptyDirective,
    TranslatePipe,
    PaginatorComponent,
    ChipComponent,
  ],
  templateUrl: './worksite-table.component.html',
  styleUrl: './worksite-table.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorksiteTableComponent {
  private readonly worksiteApiService = inject(WorksiteApiService);
  private readonly router = inject(Router);

  readonly query = input('', { transform: normalizeListQuery });
  readonly page = input(DEFAULT_LIST_PAGE, { transform: normalizeListPage });
  readonly refreshToken = input(0);
  readonly pageChange = output<number>();

  protected readonly currentPage = this.page;

  protected readonly worksitesResource = rxResource<
    WorksitePage,
    { refreshToken: number; page: number; query: string }
  >({
    params: () => ({
      refreshToken: this.refreshToken(),
      page: this.currentPage(),
      query: this.query(),
    }),
    stream: ({ params }) =>
      this.worksiteApiService.search(
        params.page - 1,
        DEFAULT_LIST_PAGE_SIZE,
        params.query,
      ),
    defaultValue: emptyListPage<Worksite>(),
  });

  protected readonly worksites = computed(() => this.worksitesResource.value().items);

  protected readonly totalItems = computed(() => this.worksitesResource.value().totalItems);

  protected readonly pagedWorksites = computed(() => this.worksites());

  private readonly pageSyncEffect = synchronizeListPage(
    this.currentPage,
    this.totalItems,
    this.worksitesResource.isLoading,
    (page) => this.pageChange.emit(page),
  );

  protected readonly isEmpty = computed(
    () =>
      !this.worksitesResource.isLoading() &&
      this.worksitesResource.error() === undefined &&
      this.totalItems() === 0,
  );

  protected onPageChange(page: number): void {
    this.pageChange.emit(page);
  }

  protected openWorksite(worksite: Worksite): void {
    this.router.navigate(['/worksites', worksite.code]);
  }

  protected get pageSize(): number {
    return DEFAULT_LIST_PAGE_SIZE;
  }
}
