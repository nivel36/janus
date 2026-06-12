/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable, finalize, of } from 'rxjs';

import { PageTemplateComponent } from '../../../core/layout/page-template/page-template.component';
import { TimezoneOption } from '../../../shared/models/timezone-option.model';
import {
  createTimezoneCatalog,
  resolveTimezoneByZoneId,
} from '../../../shared/utils/timezone-catalog.util';
import { AutocompleteTextboxFieldComponent } from '../../../shared/ui/autocomplete-textbox-field/autocomplete-textbox-field.component';
import { ButtonComponent } from '../../../shared/ui/button/button.component';
import {
  SelectFieldComponent,
  SelectOption,
} from '../../../shared/ui/select-field/select-field.component';
import { WorksiteScope } from '../models/worksite';
import { WorksiteApiService } from '../services/worksite-api.service';
import { UniqueWorksiteCodeValidator } from '../validators/unique-worksite-code.validator';

@Component({
  selector: 'app-worksite-create-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    AutocompleteTextboxFieldComponent,
    ButtonComponent,
    SelectFieldComponent,
    PageTemplateComponent,
  ],
  templateUrl: './worksite-create-page.component.html',
  styleUrl: './worksite-create-page.component.css',
})
export class WorksiteCreatePageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly worksiteApiService = inject(WorksiteApiService);
  private readonly uniqueWorksiteCodeValidator = inject(UniqueWorksiteCodeValidator);

  readonly form = this.fb.group({
    code: this.fb.nonNullable.control('', {
      validators: [
        Validators.required,
        Validators.maxLength(50),
        Validators.pattern(/^[A-Za-z0-9_-]+$/),
      ],
      asyncValidators: [this.uniqueWorksiteCodeValidator.validate],
      updateOn: 'blur',
    }),

    name: this.fb.nonNullable.control('', {
      validators: [
        Validators.required,
        Validators.maxLength(250),
        Validators.pattern(/^[\p{L}0-9 _'.,-]+$/u),
      ],
    }),

    timeZone: this.fb.control<string | null>('Europe/Madrid', {
      validators: [Validators.required],
    }),

    scope: this.fb.nonNullable.control<WorksiteScope>('GLOBAL', {
      validators: [Validators.required],
    }),
    description: this.fb.control<string | null>(null, {
      validators: [Validators.maxLength(500)],
    }),
    address: this.fb.control<string | null>(null, {
      validators: [Validators.maxLength(500)],
    }),
  });

  readonly scopeOptions: SelectOption<WorksiteScope>[] = (
    ['GLOBAL', 'ASSIGNED'] as WorksiteScope[]
  ).map((scope) => ({
    value: scope,
    labelKey: `worksite.scopes.${scope}`,
  }));

  readonly timezoneCatalog = createTimezoneCatalog();

  saving = false;

  errorMessage = '';

  save(): void {
    if (this.saving || this.form.pending || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const rawValue = this.form.getRawValue();

    this.saving = true;
    this.errorMessage = '';

    this.worksiteApiService
      .create({
        code: rawValue.code.trim(),
        name: rawValue.name.trim(),
        timeZone: rawValue.timeZone!,
        scope: rawValue.scope,
        description: rawValue.description?.trim() || null,
        address: rawValue.address?.trim() || null,
      })
      .pipe(
        finalize(() => {
          this.saving = false;
        }),
      )
      .subscribe({
        next: () => {
          this.router.navigate(['/worksites']);
        },
        error: () => {
          this.errorMessage = 'worksite.errors.create';
        },
      });
  }

  cancel(): void {
    this.router.navigate(['/worksites']);
  }

  readonly timezoneDisplayWith = (option: TimezoneOption): string => option.literal;

  readonly timezoneValueWith = (option: TimezoneOption): string => option.zoneId;

  readonly resolveTimezoneByValue = (zoneId: string): TimezoneOption | null =>
    resolveTimezoneByZoneId(this.timezoneCatalog, zoneId);

  readonly searchMethod = (query: string): Observable<TimezoneOption[]> => {
    const normalizedQuery = query.trim().toLowerCase();

    if (!normalizedQuery) {
      return of([]);
    }

    return of(
      this.timezoneCatalog
        .filter(
          (option) =>
            option.zoneId.toLowerCase().includes(normalizedQuery) ||
            option.literal.toLowerCase().includes(normalizedQuery),
        )
        .slice(0, 50),
    );
  };
}
