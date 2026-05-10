/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { PageTemplateComponent } from '../../../core/layout/page-template/page-template.component';
import { WorksiteTableComponent } from '../components/worksite-table/worksite-table.component';
import { SearchBarComponent } from '../../../shared/ui/search-bar/search-bar.component';
import { ButtonComponent } from '../../../shared/ui/button/button.component';

@Component({
  selector: 'app-worksites-page',
  standalone: true,
  imports: [
    PageTemplateComponent,
    WorksiteTableComponent,
    SearchBarComponent,
    ButtonComponent,
    TranslatePipe,
  ],
  templateUrl: './worksites-page.component.html',
  styleUrl: './worksites-page.component.css',
})
export class WorksitesPageComponent {
  constructor(private readonly router: Router) {}

  protected readonly searchQuery = signal('');

  protected onQueryChange(query: string): void {
    this.searchQuery.set(query);
  }

  protected createWorksite(): void {
    this.router.navigate(['/worksites/new']);
  }
}
