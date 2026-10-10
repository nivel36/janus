/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { rxResource, toObservable } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { EMPTY, filter, takeUntil } from 'rxjs';

import { CreateEmployeeRequest } from '../../../../client/model/createEmployeeRequest';
import { EmployeeResponse } from '../../../../client/model/employeeResponse';
import { PageTemplateComponent } from '../../../../core/layout/page-template/page-template.component';
import {
  AsyncErrorDirective,
  AsyncLoadingDirective,
  AsyncStateComponent,
} from '../../../../shared/ui/async-state/async-state.component';
import { ButtonComponent } from '../../../../shared/ui/button/button.component';
import { EmployeeFormComponent } from '../../components/employee-form/employee-form.component';
import { EmployeeApiService } from '../../services/employee-api.service';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-employee-edit-page',
  imports: [
    TranslatePipe,
    PageTemplateComponent,
    AsyncStateComponent,
    AsyncLoadingDirective,
    AsyncErrorDirective,
    ButtonComponent,
    EmployeeFormComponent,
  ],
  templateUrl: './employee-edit-page.component.html',
})
export class EmployeeEditPageComponent {
  private readonly router = inject(Router);
  private readonly employeeApi = inject(EmployeeApiService);

  readonly employeeNumber = input.required<string>();
  private readonly employeeNumberChanges = toObservable(this.employeeNumber);

  protected readonly employeeResource = rxResource({
    params: () => ({ employeeNumber: this.employeeNumber() }),
    stream: ({ params }) => this.employeeApi.findByEmployeeNumber(params.employeeNumber),
  });
  protected readonly employee = computed(() => {
    if (!this.employeeResource.hasValue()) {
      return null;
    }
    const employee = this.employeeResource.value();
    return employee.employeeNumber === this.employeeNumber() ? employee : null;
  });

  readonly saveErrorKey = (error: unknown): string =>
    error instanceof HttpErrorResponse && error.status === 409
      ? 'employee.errors.conflict'
      : 'employee.errors.update';

  readonly save = ({ name, surname, email, scheduleCode }: CreateEmployeeRequest) => {
    const employee = this.employee();
    if (!employee || this.employeeResource.isLoading()) {
      return EMPTY;
    }
    return this.employeeApi
      .update(employee.employeeNumber, { name, surname, email, scheduleCode })
      .pipe(
        takeUntil(
          this.employeeNumberChanges.pipe(filter((number) => number !== employee.employeeNumber)),
        ),
      );
  };

  onSaved(employee: EmployeeResponse): void {
    if (employee.employeeNumber === this.employeeNumber()) {
      this.router.navigate(['/employees', employee.employeeNumber]);
    }
  }
}
