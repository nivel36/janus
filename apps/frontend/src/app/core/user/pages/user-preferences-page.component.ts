/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { resolveSupportedLanguage, supportedLanguages } from '../../i18n/language.util';
import { PageTemplateComponent } from '../../../core/layout/page-template/page-template.component';
import { TimezoneCatalog } from '../../../shared/services/timezone-catalog.service';
import { AutocompleteTextboxComponent } from '../../../shared/ui/autocomplete-textbox/autocomplete-textbox.component';
import { AutocompleteValueAccessorDirective } from '../../../shared/ui/autocomplete-textbox/autocomplete-value-accessor.directive';
import { FieldComponent } from '../../../shared/ui/field/field.component';
import { SelectComponent, SelectOption } from '../../../shared/ui/select/select.component';
import { ButtonComponent } from '../../../shared/ui/button/button.component';
import { UserPreferences } from '../models/user-preferences';
import { CurrentUserFacade } from '../services/current-user.facade';
import { TimeFormat } from '../models/user-preferences';

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
import { MessageComponent } from '../../../shared/ui/message/message.component';

@Component({
  selector: 'app-user-preferences-page',
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
      validators: [Validators.required],
    }),
    timeFormat: this.fb.nonNullable.control('H24' as TimeFormat, {
      validators: [Validators.required],
    }),
    defaultTimezone: this.fb.control<string | null>(null, {
      validators: [Validators.required],
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

  /**
   * Indicates whether the initial preference load is in progress.
   */
  readonly loading = this.currentUserFacade.preferencesLoading;

  /**
   * Indicates whether a save operation is in progress.
   */
  readonly saving = signal(false);

  /**
   * Translation key of the current error message, if any.
   */
  private readonly saveErrorMessage = signal('');

  readonly errorMessage = computed(() =>
    this.currentUserFacade.preferencesError()
      ? 'userPreferences.errors.load'
      : this.saveErrorMessage(),
  );

  private readonly populateFormEffect = effect(() => {
    const preferences = this.currentUserFacade.preferences();
    if (preferences) {
      this.applyPreferences(preferences);
    }
  });

  /**
   * Loads the preferences of the current authenticated user and
   * populates the form.
   *
   * If no preferences are available or loading fails, an error
   * translation key is exposed to the template.
   */
  loadPreferences(): void {
    this.saveErrorMessage.set('');
    this.currentUserFacade.reloadPreferences();
  }

  /**
   * Persists the current form values as preferences for the
   * authenticated user.
   *
   * When the form is invalid, all controls are marked as touched
   * so validation feedback becomes visible.
   *
   * On successful save, the form is synchronized with the values
   * returned by the backend and the page navigates back.
   */
  save(): void {
    if (this.saving() || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const rawValue = this.form.getRawValue();

    /**
     * The form is validated before this point, so defaultTimezone is expected
     * to contain a non-null IANA timezone identifier.
     */
    const payload: UserPreferences = {
      locale: rawValue.locale,
      timeFormat: rawValue.timeFormat,
      defaultTimezone: rawValue.defaultTimezone!,
    };

    this.saving.set(true);
    this.saveErrorMessage.set('');

    this.currentUserFacade
      .updatePreferences(payload)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.saving.set(false);
        }),
      )
      .subscribe({
        next: (updatedPreferences) => {
          this.applyPreferences(updatedPreferences);
          this.cancel();
        },
        error: () => {
          this.saveErrorMessage.set('userPreferences.errors.update');
        },
      });
  }

  /**
   * Navigates back to root .
   */
  cancel(): void {
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
      locale: resolvedLocale,
      timeFormat: preferences.timeFormat,
      defaultTimezone: preferences.defaultTimezone,
    });

    this.form.markAsPristine();
    this.form.markAsUntouched();
  }
}
