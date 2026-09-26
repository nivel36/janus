/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { PageTemplateComponent } from '../../../../core/layout/page-template/page-template.component';
import { TimezoneCatalog } from '../../../../shared/services/timezone-catalog.service';
import { AutocompleteTextboxComponent } from '../../../../shared/ui/autocomplete-textbox/autocomplete-textbox.component';
import { AutocompleteValueAccessorDirective } from '../../../../shared/ui/autocomplete-textbox/autocomplete-value-accessor.directive';
import { ButtonComponent } from '../../../../shared/ui/button/button.component';
import { FieldComponent } from '../../../../shared/ui/field/field.component';
import { SelectComponent } from '../../../../shared/ui/select/select.component';
import { InputComponent } from '../../../../shared/ui/input/input.component';
import { createWorksiteFormControls, WORKSITE_SCOPE_OPTIONS } from '../../forms/worksite-form';
import { WorksiteApiService } from '../../services/worksite-api.service';
import { UniqueWorksiteCodeValidator } from '../../validators/unique-worksite-code.validator';

import { MessageComponent } from '../../../../shared/ui/message/message.component';

@Component({
  selector: 'app-worksite-create-page',
  standalone: true,
  imports: [
    MessageComponent,
    ReactiveFormsModule,
    TranslatePipe,
    AutocompleteTextboxComponent,
    AutocompleteValueAccessorDirective,
    ButtonComponent,
    FieldComponent,
    SelectComponent,
    InputComponent,
    PageTemplateComponent,
  ],
  templateUrl: './worksite-create-page.component.html',
})
export class WorksiteCreatePageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  readonly timezoneCatalog = inject(TimezoneCatalog);
  readonly timezoneSearch = this.timezoneCatalog.createSearchState(this.destroyRef);
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

    ...createWorksiteFormControls(this.fb),
  });

  readonly scopeOptions = WORKSITE_SCOPE_OPTIONS;

  readonly saving = signal(false);

  readonly errorMessage = signal('');

  save(): void {
    if (this.saving() || this.form.pending || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const rawValue = this.form.getRawValue();

    this.saving.set(true);
    this.errorMessage.set('');

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
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.saving.set(false);
        }),
      )
      .subscribe({
        next: () => {
          this.router.navigate(['/worksites']);
        },
        error: () => {
          this.errorMessage.set('worksite.errors.create');
        },
      });
  }

  cancel(): void {
    this.router.navigate(['/worksites']);
  }
}
