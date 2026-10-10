/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ChangeDetectionStrategy, Component, computed, inject, input, linkedSignal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';

import { PageTemplateComponent } from '../../../../core/layout/page-template/page-template.component';
import {
  AsyncErrorDirective,
  AsyncLoadingDirective,
  AsyncStateComponent,
} from '../../../../shared/ui/async-state/async-state.component';
import { ButtonComponent } from '../../../../shared/ui/button/button.component';
import { TimelogTableComponent } from '../../../timelogs/components/timelog-table/timelog-table.component';
import { WorksiteTableComponent } from '../../../worksites/components/worksite-table/worksite-table.component';
import { DEFAULT_LIST_PAGE } from '../../../../shared/utils/list-query-params.util';
import { EmployeeApiService } from '../../services/employee-api.service';
import { EmployeeDetailHeaderComponent } from '../../components/employee-detail-header/employee-detail-header.component';

@Component({
  selector: 'app-employee-detail-page',
  imports: [
    PageTemplateComponent,
    AsyncStateComponent,
    AsyncLoadingDirective,
    AsyncErrorDirective,
    ButtonComponent,
    TimelogTableComponent,
    WorksiteTableComponent,
    EmployeeDetailHeaderComponent,
    TranslatePipe,
  ],
  templateUrl: './employee-detail-page.component.html',
  styleUrl: './employee-detail-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmployeeDetailPageComponent {
  private readonly employeeApiService = inject(EmployeeApiService);

  readonly employeeNumber = input.required<string>();

  protected readonly retryLoad = () => this.employeeResource.reload();

  protected readonly employeeResource = rxResource({
    params: () => ({ employeeNumber: this.employeeNumber() }),
    stream: ({ params }) => this.employeeApiService.findByEmployeeNumber(params.employeeNumber),
  });
  protected readonly employee = computed(() =>
    this.employeeResource.hasValue() ? this.employeeResource.value() : undefined,
  );
  protected readonly timelogFilters = computed(() => ({ employeeNumber: this.employeeNumber() }));
  protected readonly worksitePage = linkedSignal({
    source: this.employeeNumber,
    computation: () => DEFAULT_LIST_PAGE,
  });
}
