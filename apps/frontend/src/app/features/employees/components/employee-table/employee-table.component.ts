/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { EmployeeApiService, EmployeePage } from '../../services/employee-api.service';
import { EmployeeResponse } from '../../../../client/model/employeeResponse';
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
  selector: 'app-employee-table',
  imports: [
    ButtonComponent,
    AsyncStateComponent,
    AsyncLoadingDirective,
    AsyncErrorDirective,
    AsyncEmptyDirective,
    TranslatePipe,
    PaginatorComponent,
  ],
  templateUrl: './employee-table.component.html',
  styleUrl: './employee-table.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmployeeTableComponent {
  private readonly employeeApiService = inject(EmployeeApiService);
  private readonly router = inject(Router);

  readonly query = input('', { transform: normalizeListQuery });
  readonly page = input(DEFAULT_LIST_PAGE, { transform: normalizeListPage });
  readonly refreshToken = input(0);
  readonly pageSize = input(DEFAULT_LIST_PAGE_SIZE);
  readonly pageChange = output<number>();

  protected readonly currentPage = this.page;

  protected readonly employeesResource = rxResource<
    EmployeePage,
    { refreshToken: number; page: number; pageSize: number; query: string }
  >({
    params: () => ({
      refreshToken: this.refreshToken(),
      page: this.currentPage(),
      pageSize: this.pageSize(),
      query: this.query(),
    }),
    stream: ({ params }) =>
      this.employeeApiService.search(params.page - 1, params.pageSize, params.query),
    defaultValue: emptyListPage<EmployeeResponse>(),
  });

  protected readonly employees = computed(() =>
    this.employeesResource.hasValue() ? this.employeesResource.value().items : [],
  );

  protected readonly totalItems = computed(() =>
    this.employeesResource.hasValue() ? this.employeesResource.value().totalItems : 0,
  );

  private readonly pageSyncEffect = synchronizeListPage(
    this.currentPage,
    this.totalItems,
    computed(() =>
      this.employeesResource.isLoading() || this.employeesResource.error() !== undefined,
    ),
    (page) => this.pageChange.emit(page),
    this.pageSize,
  );

  protected readonly isEmpty = computed(
    () =>
      !this.employeesResource.isLoading() &&
      this.employeesResource.error() === undefined &&
      this.totalItems() === 0,
  );

  protected onPageChange(page: number): void {
    this.pageChange.emit(page);
  }

  protected openEmployee(employee: EmployeeResponse): void {
    this.router.navigate(['/employees', employee.employeeNumber]);
  }
}
