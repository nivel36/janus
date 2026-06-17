/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable, finalize, of } from 'rxjs';

import { PageTemplateComponent } from '../../../core/layout/page-template/page-template.component';
import { TimezoneOption } from '../../../shared/models/timezone-option.model';
import { AutocompleteTextboxComponent } from '../../../shared/ui/autocomplete-textbox/autocomplete-textbox.component';
import { ButtonComponent } from '../../../shared/ui/button/button.component';
import { FieldComponent } from '../../../shared/ui/field/field.component';
import { SelectComponent, SelectOption } from '../../../shared/ui/select/select.component';
import { retryTransientHttpErrors } from '../../../shared/utils/http-retry.util';
import {
  createTimezoneCatalog,
  resolveTimezoneByZoneId,
} from '../../../shared/utils/timezone-catalog.util';
import { Worksite, WorksiteScope } from '../models/worksite';
import { WorksiteApiService } from '../services/worksite-api.service';

@Component({
  selector: 'app-worksite-edit-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    AutocompleteTextboxComponent,
    ButtonComponent,
    FieldComponent,
    SelectComponent,
    PageTemplateComponent,
  ],
  templateUrl: './worksite-edit-page.component.html',
  styleUrl: './worksite-create-page.component.css',
})
export class WorksiteEditPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly worksiteApiService = inject(WorksiteApiService);

  private loadedWorksite: Worksite | null = null;

  readonly worksiteCode = this.route.snapshot.paramMap.get('code') ?? '';

  readonly form = this.fb.group({
    code: this.fb.nonNullable.control({ value: '', disabled: true }),

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

  loading = true;

  saving = false;

  errorMessage = '';

  ngOnInit(): void {
    this.worksiteApiService
      .findByCode(this.worksiteCode)
      .pipe(
        retryTransientHttpErrors(),
        finalize(() => {
          this.loading = false;
        }),
      )
      .subscribe({
        next: (worksite) => {
          this.loadedWorksite = worksite;
          this.form.reset({
            code: worksite.code,
            name: worksite.name,
            timeZone: worksite.timeZone,
            scope: worksite.scope,
            description: worksite.description,
            address: worksite.address,
          });
        },
        error: () => {
          this.errorMessage = 'worksite.detailLoadError';
        },
      });
  }

  save(): void {
    if (this.saving || this.loading || this.form.invalid || this.loadedWorksite === null) {
      this.form.markAllAsTouched();
      return;
    }

    const rawValue = this.form.getRawValue();

    this.saving = true;
    this.errorMessage = '';

    this.worksiteApiService
      .update(this.loadedWorksite.code, {
        name: rawValue.name.trim(),
        timeZone: rawValue.timeZone!,
        scope: rawValue.scope,
        description: rawValue.description?.trim() || null,
        address: rawValue.address?.trim() || null,
        ownerEmployeeEmail: this.loadedWorksite.ownerEmployeeEmail,
      })
      .pipe(
        finalize(() => {
          this.saving = false;
        }),
      )
      .subscribe({
        next: (worksite) => {
          this.router.navigate(['/worksites', worksite.code]);
        },
        error: () => {
          this.errorMessage = 'worksite.errors.update';
        },
      });
  }

  cancel(): void {
    this.router.navigate(['/worksites', this.worksiteCode]);
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
