/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, computed, effect, inject, signal } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { filter, finalize, map, Observable, of, takeUntil } from 'rxjs';

import { PageTemplateComponent } from '../../../../core/layout/page-template/page-template.component';
import { ACTIVE_SCREEN_HTTP_RETRY_POLICY } from '../../../../core/http/http-retry.interceptor';
import { TimezoneOption } from '../../../../shared/models/timezone-option.model';
import { AutocompleteTextboxComponent } from '../../../../shared/ui/autocomplete-textbox/autocomplete-textbox.component';
import { ButtonComponent } from '../../../../shared/ui/button/button.component';
import { FieldComponent } from '../../../../shared/ui/field/field.component';
import { InputComponent } from '../../../../shared/ui/input/input.component';
import { SelectComponent, SelectOption } from '../../../../shared/ui/select/select.component';
import {
  createTimezoneCatalog,
  resolveTimezoneByZoneId,
} from '../../../../shared/utils/timezone-catalog.util';
import { Worksite, WorksiteScope } from '../../models/worksite';
import { WorksiteApiService } from '../../services/worksite-api.service';

import { MessageComponent } from '../../../../shared/ui/message/message.component';

@Component({
  selector: 'app-worksite-edit-page',
  standalone: true,
  imports: [
    MessageComponent,
    ReactiveFormsModule,
    TranslatePipe,
    AutocompleteTextboxComponent,
    ButtonComponent,
    FieldComponent,
    InputComponent,
    SelectComponent,
    PageTemplateComponent,
  ],
  templateUrl: './worksite-edit-page.component.html',
})
export class WorksiteEditPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly worksiteApiService = inject(WorksiteApiService);

  readonly worksiteCode = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('code') ?? '')),
    { initialValue: '' },
  );

  private readonly worksiteResource = rxResource<Worksite, { code: string }>({
    params: () => ({ code: this.worksiteCode() }),
    stream: ({ params }) =>
      this.worksiteApiService.findByCode(params.code, ACTIVE_SCREEN_HTTP_RETRY_POLICY),
  });

  private readonly worksite = computed(() => {
    if (!this.worksiteResource.hasValue()) {
      return null;
    }

    const worksite = this.worksiteResource.value();
    return worksite?.code === this.worksiteCode() ? worksite : null;
  });

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

  readonly loading = computed(() => this.worksiteResource.isLoading());

  readonly saving = signal(false);

  private readonly saveErrorMessage = signal('');

  readonly errorMessage = computed(() =>
    this.worksiteResource.error() ? 'worksite.detailLoadError' : this.saveErrorMessage(),
  );

  private readonly clearSaveErrorOnWorksiteChangeEffect = effect(() => {
    this.worksiteCode();
    this.saveErrorMessage.set('');
  });

  private readonly populateFormEffect = effect(() => {
    const worksite = this.worksite();
    if (worksite) {
      this.form.reset({
        code: worksite.code,
        name: worksite.name,
        timeZone: worksite.timeZone,
        scope: worksite.scope,
        description: worksite.description,
        address: worksite.address,
      });
    }
  });

  save(): void {
    const worksite = this.worksite();
    if (this.saving() || this.loading() || this.form.invalid || worksite === null) {
      this.form.markAllAsTouched();
      return;
    }

    const rawValue = this.form.getRawValue();

    this.saving.set(true);
    this.saveErrorMessage.set('');

    this.worksiteApiService
      .update(worksite.code, {
        name: rawValue.name.trim(),
        timeZone: rawValue.timeZone!,
        scope: rawValue.scope,
        description: rawValue.description?.trim() || null,
        address: rawValue.address?.trim() || null,
        ownerEmployeeEmail: worksite.ownerEmployeeEmail,
      })
      .pipe(
        takeUntil(
          this.route.paramMap.pipe(
            map((params) => params.get('code') ?? ''),
            filter((code) => code !== worksite.code),
          ),
        ),
        finalize(() => {
          this.saving.set(false);
        }),
      )
      .subscribe({
        next: (worksite) => {
          this.router.navigate(['/worksites', worksite.code]);
        },
        error: () => {
          this.saveErrorMessage.set('worksite.errors.update');
        },
      });
  }

  cancel(): void {
    this.router.navigate(['/worksites', this.worksiteCode()]);
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
