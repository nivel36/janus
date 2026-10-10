/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faUser } from '@fortawesome/free-solid-svg-icons';

import { EmployeeResponse } from '../../../../client/model/employeeResponse';

@Component({
  selector: 'app-employee-detail-header',
  imports: [FontAwesomeModule],
  templateUrl: './employee-detail-header.component.html',
  styleUrl: './employee-detail-header.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmployeeDetailHeaderComponent {
  readonly employee = input.required<EmployeeResponse>();

  protected readonly faUser = faUser;
}
