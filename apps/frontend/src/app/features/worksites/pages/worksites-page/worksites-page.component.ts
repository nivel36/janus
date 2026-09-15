/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { AsyncPipe } from '@angular/common';
import { Component, inject, input } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { ButtonComponent } from '../../../../shared/ui/button/button.component';
import { CurrentUserFacade } from '../../../../core/user/services/current-user.facade';
import { PageTemplateComponent } from '../../../../core/layout/page-template/page-template.component';
import { SearchBarComponent } from '../../../../shared/ui/search-bar/search-bar.component';
import { WorksiteTableComponent } from '../../components/worksite-table/worksite-table.component';
import {
  DEFAULT_LIST_PAGE,
  listQueryParams,
  normalizeListPage,
  normalizeListQuery,
} from '../../../../shared/utils/list-query-params.util';

@Component({
  selector: 'app-worksites-page',
  standalone: true,
  imports: [
    AsyncPipe,
    ButtonComponent,
    PageTemplateComponent,
    SearchBarComponent,
    TranslatePipe,
    WorksiteTableComponent,
  ],
  templateUrl: './worksites-page.component.html',
})
export class WorksitesPageComponent {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly currentUser = inject(CurrentUserFacade);

  protected readonly query = input('', { transform: normalizeListQuery });
  protected readonly page = input(DEFAULT_LIST_PAGE, { transform: normalizeListPage });
  protected readonly isAdmin = this.currentUser.isAdmin$;

  protected onQueryChange(query: string): void {
    this.navigateTo(query, DEFAULT_LIST_PAGE);
  }

  protected onPageChange(page: number): void {
    this.navigateTo(this.query(), page);
  }

  protected createWorksite(): void {
    this.router.navigate(['/worksites/new']);
  }

  private navigateTo(query: string, page: number): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: listQueryParams(query, page),
    });
  }
}
