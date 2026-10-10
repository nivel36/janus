/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { HttpContext } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { EmployeesService } from '../../../client/api/employees.service';
import { EmployeeResponse } from '../../../client/model/employeeResponse';
import {
  ACTIVE_SCREEN_HTTP_RETRY_POLICY,
  HTTP_RETRY_POLICY,
} from '../../../core/http/http-retry.interceptor';

export interface EmployeePage {
  items: EmployeeResponse[];
  totalItems: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

@Injectable({ providedIn: 'root' })
export class EmployeeApiService {
  private readonly api = inject(EmployeesService);

  findByEmployeeNumber(employeeNumber: string): Observable<EmployeeResponse> {
    const context = new HttpContext().set(HTTP_RETRY_POLICY, ACTIVE_SCREEN_HTTP_RETRY_POLICY);
    return this.api.findEmployee(employeeNumber, 'body', false, { context });
  }

  search(page = 0, size = 5, query = ''): Observable<EmployeePage> {
    const context = new HttpContext().set(HTTP_RETRY_POLICY, ACTIVE_SCREEN_HTTP_RETRY_POLICY);
    return this.api
      .searchEmployees(
        query.trim() || undefined,
        undefined,
        undefined,
        page,
        size,
        ['employeeNumber,desc'],
        'body',
        false,
        { context },
      )
      .pipe(
        map((response) => ({
          items: response.content ?? [],
          totalItems: response.page?.totalElements ?? 0,
          page: response.page?.number ?? page,
          pageSize: response.page?.size ?? size,
          totalPages: response.page?.totalPages ?? 0,
        })),
      );
  }
}
