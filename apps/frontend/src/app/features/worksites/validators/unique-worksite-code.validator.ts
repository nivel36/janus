/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AbstractControl, ValidationErrors } from '@angular/forms';
import { Observable, catchError, map, of } from 'rxjs';

import { WorksiteApiService } from '../services/worksite-api.service';

@Injectable({
  providedIn: 'root',
})
export class UniqueWorksiteCodeValidator {
  private readonly worksiteApiService = inject(WorksiteApiService);

  readonly validate = (control: AbstractControl): Observable<ValidationErrors | null> => {
    const code = String(control.value ?? '').trim();

    if (!code) {
      return of(null);
    }

    if (
      control.hasError('required') ||
      control.hasError('maxlength') ||
      control.hasError('pattern')
    ) {
      return of(null);
    }

    return this.worksiteApiService.findByCode(code).pipe(
      map(() => ({ duplicatedCode: true })),
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 404) {
          return of(null);
        }

        return of({ codeLookupFailed: true });
      }),
    );
  };
}
