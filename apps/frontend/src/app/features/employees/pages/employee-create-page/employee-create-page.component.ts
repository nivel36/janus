/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize, map } from 'rxjs';

import { PageTemplateComponent } from '../../../../core/layout/page-template/page-template.component';
import { AutocompleteSearchState } from '../../../../shared/state/autocomplete-search.state';
import { AutocompleteTextboxComponent } from '../../../../shared/ui/autocomplete-textbox/autocomplete-textbox.component';
import { AutocompleteValueAccessorDirective } from '../../../../shared/ui/autocomplete-textbox/autocomplete-value-accessor.directive';
import { ButtonComponent } from '../../../../shared/ui/button/button.component';
import { FieldComponent } from '../../../../shared/ui/field/field.component';
import { InputComponent } from '../../../../shared/ui/input/input.component';
import { MessageComponent } from '../../../../shared/ui/message/message.component';
import { Schedule } from '../../../schedules/models/schedule';
import { ScheduleApiService } from '../../../schedules/services/schedule-api.service';
import { EmployeeApiService } from '../../services/employee-api.service';
import { UniqueEmployeeNumberValidator } from '../../validators/unique-employee-number.validator';

const FIELD_ERROR_KEYS = {
  employeeNumber: 'employee.errors.invalidNumber',
  name: 'employee.errors.invalidName',
  surname: 'employee.errors.invalidSurname',
  email: 'employee.errors.invalidEmail',
  schedule: 'employee.errors.invalidSchedule',
} as const;

type EmployeeFormField = keyof typeof FIELD_ERROR_KEYS;

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-employee-create-page',
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    PageTemplateComponent,
    AutocompleteTextboxComponent,
    AutocompleteValueAccessorDirective,
    ButtonComponent,
    FieldComponent,
    InputComponent,
    MessageComponent,
  ],
  templateUrl: './employee-create-page.component.html',
})
export class EmployeeCreatePageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  private readonly employeeApi = inject(EmployeeApiService);
  private readonly scheduleApi = inject(ScheduleApiService);
  private readonly uniqueNumberValidator = inject(UniqueEmployeeNumberValidator);

  readonly scheduleSearch = new AutocompleteSearchState(
    (query: string) => this.scheduleApi.search(0, 50, query).pipe(map((page) => page.items)),
    this.destroyRef,
  );
  readonly displaySchedule = (schedule: Schedule): string => `${schedule.name} (${schedule.code})`;

  readonly form = this.fb.group({
    employeeNumber: this.fb.nonNullable.control('', {
      validators: [Validators.required, Validators.maxLength(50), Validators.pattern(/^[A-Za-z0-9_-]+$/)],
      asyncValidators: [this.uniqueNumberValidator.validate],
      updateOn: 'blur',
    }),
    name: this.fb.nonNullable.control('', [
      Validators.required,
      Validators.maxLength(255),
      Validators.pattern(/^[\p{L} .,'-]+$/u),
      Validators.pattern(/\S/),
    ]),
    surname: this.fb.nonNullable.control('', [
      Validators.required,
      Validators.maxLength(255),
      Validators.pattern(/^[\p{L} .,'-]+$/u),
      Validators.pattern(/\S/),
    ]),
    email: this.fb.nonNullable.control('', [Validators.required, Validators.email, Validators.maxLength(254)]),
    schedule: this.fb.control<Schedule | null>(null, Validators.required),
  });
  readonly formStatus = toSignal(this.form.statusChanges, { initialValue: this.form.status });
  readonly saving = signal(false);
  readonly errorMessage = signal('');

  protected readonly saveDisabled = computed(
    () => this.formStatus() === 'INVALID' || this.formStatus() === 'PENDING' || this.saving(),
  );

  protected get employeeNumberHintKey(): string {
    if (this.form.controls.employeeNumber.pending) {
      return 'employee.messages.numberChecking';
    }
    return '';
  }

  protected isInvalid(field: EmployeeFormField): boolean {
    const control = this.form.controls[field];
    return control.touched && control.invalid;
  }

  protected fieldErrorKey(field: EmployeeFormField): string {
    if (!this.isInvalid(field)) {
      return '';
    }

    const control = this.form.controls[field];
    if (field === 'employeeNumber') {
      if (control.hasError('duplicatedNumber')) {
        return 'employee.errors.duplicatedNumber';
      }
      if (control.hasError('numberLookupFailed')) {
        return 'employee.errors.numberLookupFailed';
      }
    }
    return FIELD_ERROR_KEYS[field];
  }

  protected fieldErrorId(field: EmployeeFormField): string | null {
    if (!this.isInvalid(field)) {
      return null;
    }
    if (field === 'employeeNumber') {
      return 'employee-number-error';
    }
    return `employee-${field}-error`;
  }

  protected fieldDescribedBy(field: EmployeeFormField): string | null {
    const errorId = this.fieldErrorId(field);
    if (errorId) {
      return errorId;
    }
    if (field === 'employeeNumber' && this.form.controls.employeeNumber.pending) {
      return 'employee-number-hint';
    }
    return null;
  }

  save(): void {
    if (this.saving() || this.form.pending || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.saving.set(true);
    this.errorMessage.set('');
    this.employeeApi.create({
      employeeNumber: value.employeeNumber.trim(),
      name: value.name.trim(),
      surname: value.surname.trim(),
      email: value.email.trim(),
      scheduleCode: value.schedule!.code,
    }).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.saving.set(false)),
    ).subscribe({
      next: () => { this.router.navigate(['/employees']); },
      error: (error: unknown) => {
        this.errorMessage.set(
          error instanceof HttpErrorResponse && error.status === 409
            ? 'employee.errors.conflict'
            : 'employee.errors.create',
        );
      },
    });
  }
}
