/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, signal } from '@angular/core';
import { PageTemplateComponent } from '../../../core/layout/page-template/page-template.component';
import { ScheduleTableComponent } from '../components/schedule-table/schedule-table.component';
import { SearchBarComponent } from '../../../shared/ui/search-bar/search-bar.component';

@Component({
  selector: 'app-schedules-page',
  standalone: true,
  imports: [PageTemplateComponent, ScheduleTableComponent, SearchBarComponent],
  templateUrl: './schedules-page.component.html',
  styleUrl: './schedules-page.component.css',
})
export class SchedulesPageComponent {
  protected readonly searchQuery = signal('');

  protected onQueryChange(query: string): void {
    this.searchQuery.set(query);
  }
}
