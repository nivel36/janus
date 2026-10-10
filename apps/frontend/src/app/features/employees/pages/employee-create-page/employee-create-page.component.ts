/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';

import { CreateEmployeeRequest } from '../../../../client/model/createEmployeeRequest';
import { PageTemplateComponent } from '../../../../core/layout/page-template/page-template.component';
import { EmployeeFormComponent } from '../../components/employee-form/employee-form.component';
import { EmployeeApiService } from '../../services/employee-api.service';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-employee-create-page',
  imports: [PageTemplateComponent, EmployeeFormComponent],
  templateUrl: './employee-create-page.component.html',
})
export class EmployeeCreatePageComponent {
  private readonly router = inject(Router);
  private readonly employeeApi = inject(EmployeeApiService);

  readonly saveErrorKey = (error: unknown): string =>
    error instanceof HttpErrorResponse && error.status === 409
      ? 'employee.errors.conflict'
      : 'employee.errors.create';

  readonly save = (value: CreateEmployeeRequest) => this.employeeApi.create(value);

  onSaved(): void {
    this.router.navigate(['/employees']);
  }
}
