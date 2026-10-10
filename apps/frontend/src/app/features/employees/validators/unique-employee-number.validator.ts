/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AbstractControl, ValidationErrors } from '@angular/forms';
import { Observable, catchError, map, of } from 'rxjs';

import { EmployeeApiService } from '../services/employee-api.service';

@Injectable({
  providedIn: 'root',
})
export class UniqueEmployeeNumberValidator {
  private readonly employeeApiService = inject(EmployeeApiService);

  readonly validate = (control: AbstractControl): Observable<ValidationErrors | null> => {
    const employeeNumber = String(control.value ?? '').trim();

    if (!employeeNumber) {
      return of(null);
    }

    if (
      control.hasError('required') ||
      control.hasError('maxlength') ||
      control.hasError('pattern')
    ) {
      return of(null);
    }

    return this.employeeApiService.findByEmployeeNumber(employeeNumber).pipe(
      map(() => ({ duplicatedNumber: true })),
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 404) {
          return of(null);
        }

        return of({ numberLookupFailed: true });
      }),
    );
  };
}
