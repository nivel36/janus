/**
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  booleanAttribute,
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
import { Observable } from 'rxjs';

import { TimezoneCatalog } from '../../../../shared/services/timezone-catalog.service';
import { AutocompleteTextboxComponent } from '../../../../shared/ui/autocomplete-textbox/autocomplete-textbox.component';
import { AutocompleteValueAccessorDirective } from '../../../../shared/ui/autocomplete-textbox/autocomplete-value-accessor.directive';
import { FieldComponent } from '../../../../shared/ui/field/field.component';
import { FormComponent } from '../../../../shared/ui/form/form.component';
import { InputComponent } from '../../../../shared/ui/input/input.component';
import { SelectComponent, SelectOption } from '../../../../shared/ui/select/select.component';
import { CreateWorksitePayload, Worksite, WorksiteScope } from '../../models/worksite';
import { UniqueWorksiteCodeValidator } from '../../validators/unique-worksite-code.validator';

const FIELD_ERROR_KEYS = {
  code: 'worksite.errors.invalidCode',
  name: 'worksite.errors.invalidName',
  timeZone: 'worksite.errors.invalidTimezone',
  description: 'worksite.errors.invalidDescription',
  address: 'worksite.errors.invalidAddress',
} as const;

type WorksiteFormField = keyof typeof FIELD_ERROR_KEYS;

const OPTIONAL_TEXT_VALIDATORS = [
  Validators.maxLength(500),
  // eslint-disable-next-line no-control-regex -- Reject NUL, which PostgreSQL cannot store.
  Validators.pattern(/^[^\x00]*$/),
];

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-worksite-form',
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    AutocompleteTextboxComponent,
    AutocompleteValueAccessorDirective,
    FormComponent,
    FieldComponent,
    InputComponent,
    SelectComponent,
  ],
  templateUrl: './worksite-form.component.html',
})
export class WorksiteFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly uniqueCodeValidator = inject(UniqueWorksiteCodeValidator);
  readonly timezoneCatalog = inject(TimezoneCatalog);
  readonly timezoneSearch = this.timezoneCatalog.createSearchState(this.destroyRef);

  readonly worksite = input<Worksite | null>(null);
  // Edit mode also keeps the code immutable when loading the worksite fails.
  readonly editMode = input(false, { transform: booleanAttribute });
  readonly save = input.required<(value: CreateWorksitePayload) => Observable<Worksite>>();
  readonly saveErrorKey = input.required<string | ((error: unknown) => string)>();
  readonly errorKey = input('');
  readonly disabled = input(false, { transform: booleanAttribute });
  readonly cancelLink = input<string | readonly unknown[]>();
  readonly saved = output<Worksite>();
  readonly cancelled = output<void>();

  readonly scopeOptions: SelectOption<WorksiteScope>[] = (['GLOBAL', 'ASSIGNED'] as const).map(
    (scope) => ({ value: scope, labelKey: `worksite.scopes.${scope}` }),
  );

  readonly form = this.fb.group({
    code: this.fb.nonNullable.control(
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
      Validators.maxLength(250),
      Validators.pattern(/^[\p{L}0-9 _'.,-]+$/u),
    ]),
    timeZone: this.fb.control<string | null>('Europe/Madrid', [
      Validators.required,
      Validators.maxLength(64),
    ]),
    scope: this.fb.nonNullable.control<WorksiteScope>('GLOBAL', Validators.required),
    description: this.fb.control<string | null>(null, OPTIONAL_TEXT_VALIDATORS),
    address: this.fb.control<string | null>(null, OPTIONAL_TEXT_VALIDATORS),
  });

  private readonly populateFormEffect = effect(() => {
    const worksite = this.worksite();
    const editMode = this.editMode();
    this.form.controls.code.setAsyncValidators(editMode ? null : this.uniqueCodeValidator.validate);
    this.form.reset({
      code: { value: worksite?.code ?? '', disabled: editMode },
      name: worksite?.name ?? '',
      timeZone: worksite?.timeZone ?? 'Europe/Madrid',
      scope: worksite?.scope ?? 'GLOBAL',
      description: worksite?.description ?? null,
      address: worksite?.address ?? null,
    });
  });

  protected get codeHintKey(): string {
    return this.form.controls.code.pending ? 'worksite.messages.codeChecking' : '';
  }

  protected isInvalid(field: WorksiteFormField): boolean {
    const control = this.form.controls[field];
    return control.touched && control.invalid;
  }

  protected fieldErrorKey(field: WorksiteFormField): string {
    if (!this.isInvalid(field)) {
      return '';
    }
    if (field === 'code') {
      if (this.form.controls.code.hasError('duplicatedCode')) {
        return 'worksite.errors.duplicatedCode';
      }
      if (this.form.controls.code.hasError('codeLookupFailed')) {
        return 'worksite.errors.codeLookupFailed';
      }
    }
    return FIELD_ERROR_KEYS[field];
  }

  protected fieldErrorId(field: WorksiteFormField): string | null {
    return this.isInvalid(field) ? `worksite-${field === 'timeZone' ? 'timezone' : field}-error` : null;
  }

  protected fieldDescribedBy(field: WorksiteFormField): string | null {
    const errorId = this.fieldErrorId(field);
    if (errorId) {
      return errorId;
    }
    return field === 'code' && this.form.controls.code.pending ? 'worksite-code-hint' : null;
  }

  readonly submit = () => {
    const value = this.form.getRawValue();
    return this.save()({
      code: value.code.trim(),
      name: value.name.trim(),
      timeZone: value.timeZone!,
      scope: value.scope,
      description: value.description?.trim() || null,
      address: value.address?.trim() || null,
    });
  };
}
