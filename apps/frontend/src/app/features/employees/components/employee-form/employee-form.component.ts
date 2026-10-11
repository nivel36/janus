/**
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  effect,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable } from 'rxjs';

import { CreateEmployeeRequest } from '../../../../client/model/createEmployeeRequest';
import { EmployeeResponse } from '../../../../client/model/employeeResponse';
import { EmployeeScheduleResponse } from '../../../../client/model/employeeScheduleResponse';
import { SelectComponent, SelectOption } from '../../../../shared/ui/select/select.component';
import {
  AsyncStateComponent,
  AsyncLoadingDirective,
  AsyncErrorDirective,
  AsyncEmptyDirective,
} from '../../../../shared/ui/async-state/async-state.component';
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
    SelectComponent,
    AsyncStateComponent,
    AsyncLoadingDirective,
    AsyncErrorDirective,
    AsyncEmptyDirective,
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

  readonly schedules = signal<EmployeeScheduleResponse[]>([]);
  readonly schedulesLoading = signal(false);
  readonly schedulesError = signal<unknown>(null);
  readonly scheduleOptions = computed<SelectOption[]>(() => {
    const schedules = [...this.schedules()];
    const current = this.employee()?.schedule;
    if (current && !schedules.some((schedule) => schedule.code === current.code)) {
      schedules.push(current);
    }
    return [
      { value: '', labelKey: 'employee.scheduleHint' },
      ...schedules.map((schedule) => ({
        value: schedule.code,
        label: `${schedule.name} (${schedule.code})`,
      })),
    ];
  });

  constructor() {
    this.loadSchedules();
  }

  readonly loadSchedules = () => {
    if (this.schedulesLoading()) {
      return;
    }
    this.schedulesLoading.set(true);
    this.schedulesError.set(null);
    this.scheduleApi.listAll().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (schedules) => {
        this.schedules.set(schedules);
        this.schedulesLoading.set(false);
      },
      error: (error: unknown) => {
        this.schedulesError.set(error);
        this.schedulesLoading.set(false);
      },
    });
  };

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
    schedule: this.fb.control<string | null>(null, Validators.required),
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
      schedule: employee?.schedule.code ?? null,
    });
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
      scheduleCode: value.schedule!,
    });
  };
}
