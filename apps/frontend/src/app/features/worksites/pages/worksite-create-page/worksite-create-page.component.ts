/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { PageTemplateComponent } from '../../../../core/layout/page-template/page-template.component';
import { TimezoneCatalog } from '../../../../shared/services/timezone-catalog.service';
import { AutocompleteTextboxComponent } from '../../../../shared/ui/autocomplete-textbox/autocomplete-textbox.component';
import { AutocompleteValueAccessorDirective } from '../../../../shared/ui/autocomplete-textbox/autocomplete-value-accessor.directive';
import { FormComponent } from '../../../../shared/ui/form/form.component';
import { FieldComponent } from '../../../../shared/ui/field/field.component';
import { SelectComponent } from '../../../../shared/ui/select/select.component';
import { InputComponent } from '../../../../shared/ui/input/input.component';
import { createWorksiteFormControls, WORKSITE_SCOPE_OPTIONS } from '../../forms/worksite-form';
import { WorksiteApiService } from '../../services/worksite-api.service';
import { UniqueWorksiteCodeValidator } from '../../validators/unique-worksite-code.validator';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-worksite-create-page',
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    AutocompleteTextboxComponent,
    AutocompleteValueAccessorDirective,
    FormComponent,
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

  readonly save = () => {
    const rawValue = this.form.getRawValue();
    return this.worksiteApiService.create({
      code: rawValue.code.trim(),
      name: rawValue.name.trim(),
      timeZone: rawValue.timeZone!,
      scope: rawValue.scope,
      description: rawValue.description?.trim() || null,
      address: rawValue.address?.trim() || null,
    });
  };

  onSaved(): void {
    this.router.navigate(['/worksites']);
  }
}
