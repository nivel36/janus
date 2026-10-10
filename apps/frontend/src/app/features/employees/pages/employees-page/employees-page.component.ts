/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { ButtonComponent } from '../../../../shared/ui/button/button.component';

import { PageTemplateComponent } from '../../../../core/layout/page-template/page-template.component';
import { SearchBarComponent } from '../../../../shared/ui/search-bar/search-bar.component';
import { EmployeeTableComponent } from '../../components/employee-table/employee-table.component';
import {
  DEFAULT_LIST_PAGE,
  listQueryParams,
  normalizeListPage,
  normalizeListQuery,
} from '../../../../shared/utils/list-query-params.util';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-employees-page',
  imports: [ButtonComponent, TranslatePipe, PageTemplateComponent, SearchBarComponent, EmployeeTableComponent],
  templateUrl: './employees-page.component.html',
})
export class EmployeesPageComponent {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly query = input('', { transform: normalizeListQuery });
  protected readonly page = input(DEFAULT_LIST_PAGE, { transform: normalizeListPage });

  protected onQueryChange(query: string): void {
    this.navigateTo(query, DEFAULT_LIST_PAGE);
  }

  protected onPageChange(page: number): void {
    this.navigateTo(this.query(), page);
  }

  private navigateTo(query: string, page: number): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: listQueryParams(query, page),
    });
  }
}
