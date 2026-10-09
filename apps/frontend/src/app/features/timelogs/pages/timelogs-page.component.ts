/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ChangeDetectionStrategy, Component } from '@angular/core';

import { PageTemplateComponent } from '../../../core/layout/page-template/page-template.component';
import { TimelogTableComponent } from '../components/timelog-table/timelog-table.component';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-timelogs-page',
  imports: [PageTemplateComponent, TimelogTableComponent],
  templateUrl: './timelogs-page.component.html',
  styleUrl: './timelogs-page.component.css',
})
export class TimelogsPageComponent {}
