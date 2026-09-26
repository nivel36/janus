/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, DestroyRef, computed, effect, inject, input, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { filter, finalize, takeUntil } from 'rxjs';

import { PageTemplateComponent } from '../../../../core/layout/page-template/page-template.component';
import { ACTIVE_SCREEN_HTTP_RETRY_POLICY } from '../../../../core/http/http-retry.interceptor';
import { AutocompleteTextboxComponent } from '../../../../shared/ui/autocomplete-textbox/autocomplete-textbox.component';
import { AutocompleteValueAccessorDirective } from '../../../../shared/ui/autocomplete-textbox/autocomplete-value-accessor.directive';
import { ButtonComponent } from '../../../../shared/ui/button/button.component';
import { FieldComponent } from '../../../../shared/ui/field/field.component';
import { InputComponent } from '../../../../shared/ui/input/input.component';
import { SelectComponent } from '../../../../shared/ui/select/select.component';
import { TimezoneCatalog } from '../../../../shared/services/timezone-catalog.service';
import { createWorksiteFormControls, WORKSITE_SCOPE_OPTIONS } from '../../forms/worksite-form';
import { Worksite } from '../../models/worksite';
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
    AutocompleteValueAccessorDirective,
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
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  readonly timezoneCatalog = inject(TimezoneCatalog);
  readonly timezoneSearch = this.timezoneCatalog.createSearchState(this.destroyRef);
  private readonly worksiteApiService = inject(WorksiteApiService);

  readonly code = input.required<string>();
  private readonly codeChanges = toObservable(this.code);

  private readonly worksiteResource = rxResource<Worksite, { code: string }>({
    params: () => ({ code: this.code() }),
    stream: ({ params }) =>
      this.worksiteApiService.findByCode(params.code, ACTIVE_SCREEN_HTTP_RETRY_POLICY),
  });

  private readonly worksite = computed(() => {
    if (!this.worksiteResource.hasValue()) {
      return null;
    }

    const worksite = this.worksiteResource.value();
    return worksite?.code === this.code() ? worksite : null;
  });

  readonly form = this.fb.group({
    code: this.fb.nonNullable.control({ value: '', disabled: true }),
    ...createWorksiteFormControls(this.fb),
  });

  readonly scopeOptions = WORKSITE_SCOPE_OPTIONS;

  readonly loading = computed(() => this.worksiteResource.isLoading());

  readonly saving = signal(false);

  private readonly saveErrorMessage = signal('');

  readonly errorMessage = computed(() =>
    this.worksiteResource.error() ? 'worksite.detailLoadError' : this.saveErrorMessage(),
  );

  private readonly clearSaveErrorOnWorksiteChangeEffect = effect(() => {
    this.code();
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
      })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        takeUntil(this.codeChanges.pipe(filter((code) => code !== worksite.code))),
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
    this.router.navigate(['/worksites', this.code()]);
  }
}
