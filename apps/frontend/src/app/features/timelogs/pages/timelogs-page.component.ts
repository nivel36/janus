/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ChangeDetectionStrategy, Component, signal } from '@angular/core';

import { PageTemplateComponent } from '../../../core/layout/page-template/page-template.component';
import { TimelogTableComponent } from '../components/timelog-table/timelog-table.component';

import { TimelogSearchCardComponent } from '../components/timelog-search-card/timelog-search-card.component';
import { TimelogSearchRange } from '../models/timelog-search-range';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-timelogs-page',
  imports: [PageTemplateComponent, TimelogTableComponent, TimelogSearchCardComponent],
  templateUrl: './timelogs-page.component.html',
  styleUrl: './timelogs-page.component.css',
})
export class TimelogsPageComponent {
  protected readonly searchRange = signal<TimelogSearchRange | undefined>(undefined);
  protected readonly searchToken = signal(0);

  protected onSearch(range: TimelogSearchRange | undefined): void {
    this.searchRange.set(range);
    this.searchToken.update((token) => token + 1);
  }
}
