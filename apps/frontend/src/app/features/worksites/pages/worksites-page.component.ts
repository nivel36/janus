/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { AsyncPipe } from '@angular/common';
import { Component, signal, inject } from '@angular/core';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { ButtonComponent } from '../../../shared/ui/button/button.component';
import { CurrentUserFacade } from '../../../core/user/services/current-user.facade';
import { PageTemplateComponent } from '../../../core/layout/page-template/page-template.component';
import { SearchBarComponent } from '../../../shared/ui/search-bar/search-bar.component';
import { WorksiteTableComponent } from '../components/worksite-table/worksite-table.component';

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
  styleUrl: './worksites-page.component.css',
})
export class WorksitesPageComponent {
  private readonly router = inject(Router);
  private readonly currentUser = inject(CurrentUserFacade);

  protected readonly searchQuery = signal('');
  protected readonly isAdmin = this.currentUser.isAdmin$;

  protected onQueryChange(query: string): void {
    this.searchQuery.set(query);
  }

  protected createWorksite(): void {
    this.router.navigate(['/worksites/new']);
  }
}
