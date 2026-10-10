/**
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  effect,
  inject,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { resolveSupportedLanguage, supportedLanguages } from '../../i18n/language.util';
import { PageTemplateComponent } from '../../../core/layout/page-template/page-template.component';
import { TimezoneCatalog } from '../../../shared/services/timezone-catalog.service';
import { AutocompleteTextboxComponent } from '../../../shared/ui/autocomplete-textbox/autocomplete-textbox.component';
import { AutocompleteValueAccessorDirective } from '../../../shared/ui/autocomplete-textbox/autocomplete-value-accessor.directive';
import { FieldComponent } from '../../../shared/ui/field/field.component';
import { SelectComponent, SelectOption } from '../../../shared/ui/select/select.component';
import { FormComponent } from '../../../shared/ui/form/form.component';
import { Theme, TimeFormat, UserPreferences } from '../models/user-preferences';
import { CurrentUserFacade } from '../services/current-user.facade';

/**
 * Page responsible for displaying and updating the preferences
 * of the currently authenticated user.
 *
 * Responsibilities:
 * - Load current persisted preferences
 * - Expose a reactive form for editing those preferences
 * - Provide timezone search and display helpers to the autocomplete component
 * - Persist changes through CurrentUserFacade
 *
 * This component does NOT:
 * - Access backend APIs directly
 * - Manage authentication state
 * - Resolve user identity by itself
 *
 * Those responsibilities belong to CurrentUserFacade and lower layers.
 */

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-user-preferences-page',
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    AutocompleteTextboxComponent,
    AutocompleteValueAccessorDirective,
    FormComponent,
    FieldComponent,
    SelectComponent,
    PageTemplateComponent,
  ],
  templateUrl: './user-preferences-page.component.html',
})
export class UserPreferencesPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly currentUserFacade = inject(CurrentUserFacade);
  private readonly router = inject(Router);
  readonly timezoneCatalog = inject(TimezoneCatalog);
  readonly timezoneSearch = this.timezoneCatalog.createSearchState(this.destroyRef);

  /**
   * Main form containing editable user preferences.
   *
   * The timezone control stores the IANA timezone identifier as a string,
   * or null when no valid selection exists.
   */
  readonly form = this.fb.group({
    locale: this.fb.nonNullable.control('es-ES', {
      validators: [Validators.required, Validators.maxLength(64)],
    }),
    timeFormat: this.fb.nonNullable.control('H24' as TimeFormat, {
      validators: [Validators.required],
    }),
    theme: this.fb.nonNullable.control<Theme>('DARK', {
      validators: [Validators.required],
    }),
    defaultTimezone: this.fb.control<string | null>(null, {
      validators: [Validators.required, Validators.maxLength(64)],
    }),
  });

  /**
   * Available locale options presented in the UI.
   */
  readonly localeOptions: SelectOption[] = supportedLanguages.map((locale) => ({
    value: locale,
    labelKey: `locale.${locale}`,
  }));

  /**
   * Available time format options presented in the UI.
   */
  readonly timeFormatOptions: SelectOption<TimeFormat>[] = (['H24', 'H12'] as TimeFormat[]).map(
    (timeFormat) => ({
      value: timeFormat,
      labelKey: `userPreferences.timeFormat.${timeFormat}`,
    }),
  );

  readonly themeOptions: SelectOption<Theme>[] = (['LIGHT', 'DARK'] as Theme[]).map((theme) => ({
    value: theme,
    labelKey: `userPreferences.theme.${theme.toLowerCase()}`,
  }));

  /**
   * Indicates whether the initial preference load is in progress.
   */
  readonly loading = this.currentUserFacade.preferencesLoading;

  /**
   * Translation key of the current error message, if any.
   */
  readonly errorMessage = computed(() =>
    this.currentUserFacade.preferencesError() ? 'userPreferences.errors.load' : '',
  );

  private readonly populateFormEffect = effect(() => {
    const preferences = this.currentUserFacade.preferences();
    if (preferences) {
      this.applyPreferences(preferences);
    }
  });

  /**
   * Builds the preference update consumed by the shared form.
   */
  readonly save = () => {
    const rawValue = this.form.getRawValue();
    const payload: UserPreferences = {
      theme: rawValue.theme,
      locale: rawValue.locale,
      timeFormat: rawValue.timeFormat,
      defaultTimezone: rawValue.defaultTimezone!,
    };
    return this.currentUserFacade.updatePreferences(payload);
  };

  onSaved(updatedPreferences: UserPreferences): void {
    this.applyPreferences(updatedPreferences);
    this.router.navigate(['/']);
  }

  /**
   * Applies loaded or updated preferences to the form and resets
   * form interaction state.
   *
   * @param preferences Preferences to display
   */
  private applyPreferences(preferences: UserPreferences): void {
    const resolvedLocale = resolveSupportedLanguage(preferences.locale);

    this.form.reset({
      theme: preferences.theme,
      locale: resolvedLocale,
      timeFormat: preferences.timeFormat,
      defaultTimezone: preferences.defaultTimezone,
    });

    this.form.markAsPristine();
    this.form.markAsUntouched();
  }
}
