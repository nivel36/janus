/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { PageTemplateComponent } from '../../../core/layout/page-template/page-template.component';
import { WorksiteTableComponent } from '../components/worksite-table/worksite-table.component';
import { SearchBarComponent } from '../../../shared/ui/search-bar/search-bar.component';

@Component({
  selector: 'app-worksites-page',
  standalone: true,
  imports: [CommonModule, PageTemplateComponent, WorksiteTableComponent, SearchBarComponent],
  templateUrl: './worksites-page.component.html',
  styleUrl: './worksites-page.component.css',
})
export class WorksitesPageComponent {}
