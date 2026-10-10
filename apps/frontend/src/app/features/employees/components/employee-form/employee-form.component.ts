/**
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  effect,
  inject,
  input,
  output,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { map, Observable } from 'rxjs';

import { CreateEmployeeRequest } from '../../../../client/model/createEmployeeRequest';
import { EmployeeResponse } from '../../../../client/model/employeeResponse';
import { EmployeeScheduleResponse } from '../../../../client/model/employeeScheduleResponse';
import { AutocompleteSearchState } from '../../../../shared/state/autocomplete-search.state';
import { AutocompleteTextboxComponent } from '../../../../shared/ui/autocomplete-textbox/autocomplete-textbox.component';
import { AutocompleteValueAccessorDirective } from '../../../../shared/ui/autocomplete-textbox/autocomplete-value-accessor.directive';
import { FormComponent } from '../../../../shared/ui/form/form.component';
import { FieldComponent } from '../../../../shared/ui/field/field.component';
import { InputComponent } from '../../../../shared/ui/input/input.component';
import { ScheduleApiService } from '../../../schedules/services/schedule-api.service';
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
  selector: 'app-employee-form',
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    AutocompleteTextboxComponent,
    AutocompleteValueAccessorDirective,
    FormComponent,
    FieldComponent,
    InputComponent,
  ],
  templateUrl: './employee-form.component.html',
})
export class EmployeeFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly scheduleApi = inject(ScheduleApiService);
  private readonly uniqueNumberValidator = inject(UniqueEmployeeNumberValidator);

  // A supplied employee selects edit mode; an empty form creates a new employee.
  readonly employee = input<EmployeeResponse | null>(null);
  readonly save = input.required<(value: CreateEmployeeRequest) => Observable<EmployeeResponse>>();
  readonly saveErrorKey = input.required<string | ((error: unknown) => string)>();
  readonly cancelLink = input.required<string | readonly unknown[]>();
  readonly saved = output<EmployeeResponse>();

  readonly scheduleSearch = new AutocompleteSearchState<EmployeeScheduleResponse>(
    (query: string) => this.scheduleApi.search(0, 50, query).pipe(map((page) => page.items)),
    this.destroyRef,
  );
  readonly displaySchedule = (schedule: EmployeeScheduleResponse): string =>
    `${schedule.name} (${schedule.code})`;

  readonly form = this.fb.group({
    employeeNumber: this.fb.nonNullable.control(
      { value: '', disabled: true },
      {
        validators: [
          Validators.required,
          Validators.maxLength(50),
          Validators.pattern(/^[A-Za-z0-9_-]+$/),
        ],
        updateOn: 'blur',
      },
    ),
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
    email: this.fb.nonNullable.control('', [
      Validators.required,
      Validators.email,
      Validators.maxLength(254),
    ]),
    schedule: this.fb.control<EmployeeScheduleResponse | null>(null, Validators.required),
  });

  private readonly populateFormEffect = effect(() => {
    const employee = this.employee();
    this.form.controls.employeeNumber.setAsyncValidators(
      employee ? null : this.uniqueNumberValidator.validate,
    );
    this.form.reset({
      employeeNumber: { value: employee?.employeeNumber ?? '', disabled: employee !== null },
      name: employee?.name ?? '',
      surname: employee?.surname ?? '',
      email: employee?.email ?? '',
      schedule: employee?.schedule ?? null,
    });
    this.scheduleSearch.search('');
  });

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

  readonly submit = () => {
    const value = this.form.getRawValue();
    return this.save()({
      employeeNumber: value.employeeNumber.trim(),
      name: value.name.trim(),
      surname: value.surname.trim(),
      email: value.email.trim(),
      scheduleCode: value.schedule!.code,
    });
  };
}
