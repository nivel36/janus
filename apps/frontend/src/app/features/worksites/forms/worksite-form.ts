/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { FormBuilder, FormControl, ValidatorFn, Validators } from '@angular/forms';

import { SelectOption } from '../../../shared/ui/select/select.component';
import { WorksiteScope } from '../models/worksite';

export const DEFAULT_WORKSITE_TIME_ZONE = 'Europe/Madrid';

export const WORKSITE_SCOPE_OPTIONS: SelectOption<WorksiteScope>[] = (
  ['GLOBAL', 'ASSIGNED'] as const
).map((scope) => ({
  value: scope,
  labelKey: `worksite.scopes.${scope}`,
}));

export const WORKSITE_NAME_VALIDATORS: ValidatorFn[] = [
  Validators.required,
  Validators.maxLength(250),
  Validators.pattern(/^[\p{L}0-9 _'.,-]+$/u),
];

export const WORKSITE_TIME_ZONE_VALIDATORS: ValidatorFn[] = [Validators.required];
export const WORKSITE_SCOPE_VALIDATORS: ValidatorFn[] = [Validators.required];
export const WORKSITE_DESCRIPTION_VALIDATORS: ValidatorFn[] = [Validators.maxLength(500)];
export const WORKSITE_ADDRESS_VALIDATORS: ValidatorFn[] = [Validators.maxLength(500)];

export interface WorksiteFormControls {
  name: FormControl<string>;
  timeZone: FormControl<string | null>;
  scope: FormControl<WorksiteScope>;
  description: FormControl<string | null>;
  address: FormControl<string | null>;
}

export function createWorksiteFormControls(formBuilder: FormBuilder): WorksiteFormControls {
  return {
    name: formBuilder.nonNullable.control('', {
      validators: WORKSITE_NAME_VALIDATORS,
    }),
    timeZone: formBuilder.control<string | null>(DEFAULT_WORKSITE_TIME_ZONE, {
      validators: WORKSITE_TIME_ZONE_VALIDATORS,
    }),
    scope: formBuilder.nonNullable.control<WorksiteScope>('GLOBAL', {
      validators: WORKSITE_SCOPE_VALIDATORS,
    }),
    description: formBuilder.control<string | null>(null, {
      validators: WORKSITE_DESCRIPTION_VALIDATORS,
    }),
    address: formBuilder.control<string | null>(null, {
      validators: WORKSITE_ADDRESS_VALIDATORS,
    }),
  };
}
