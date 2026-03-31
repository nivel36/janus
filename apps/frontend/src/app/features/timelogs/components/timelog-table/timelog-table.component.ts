/** * SPDX-License-Identifier: Apache-2.0 */
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  resource,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { TranslatePipe } from '@ngx-translate/core';
import { TimeLogService } from '../../services/timelog-api.service';
import { TimeLog } from '../../models/timelog';
import { DurationPipe } from '../../../../shared/pipes/duration.pipe';
@Component({
  selector: 'app-timelog-table',
  standalone: true,
  imports: [TranslatePipe, DatePipe, DurationPipe],
  templateUrl: './timelog-table.component.html',
  styleUrl: './timelog-table.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TimelogTableComponent {
  readonly employeeEmail = input.required<string>();
  readonly refreshToken = input(0);
  private readonly timeLogService = inject(TimeLogService);
  protected readonly timelogsResource = resource<
    TimeLog[],
    {
      employeeEmail: string;
      refreshToken: number;
    }
  >({
    params: () => ({ employeeEmail: this.employeeEmail(), refreshToken: this.refreshToken() }),
    loader: async ({ params }) =>
      await firstValueFrom(this.timeLogService.searchByEmployee(params.employeeEmail)),
    defaultValue: [],
  });
  protected readonly timelogs = computed(() => this.timelogsResource.value());
  protected readonly isLoading = computed(() => this.timelogsResource.isLoading());
  protected readonly hasError = computed(() => this.timelogsResource.error() !== undefined);
  protected readonly isEmpty = computed(
    () => !this.isLoading() && !this.hasError() && this.timelogs().length === 0,
  );

  protected trackByTimelog(index: number, timelog: TimeLog): string {
    return `${timelog.entryTime}-${timelog.exitTime ?? 'open'}-${index}`;
  }
}
