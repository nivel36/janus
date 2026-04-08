/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Location } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable, finalize, of, take } from 'rxjs';

import { supportedLanguages } from '../../../app.config';
import { PageTemplateComponent } from '../../../core/layout/page-template/page-template.component';
import { TimezoneOption } from '../../../shared/models/timezone-option.model';
import {
  createTimezoneCatalog,
  resolveTimezoneByZoneId,
} from '../../../shared/utils/timezone-catalog.util';
import { AutocompleteTextboxComponent } from '../../../shared/ui/autocomplete-textbox/autocomplete-textbox.component';
import { ButtonComponent } from '../../../shared/ui/button/button.component';
import { CardComponent } from '../../../shared/ui/card/card.component';
import { UserPreferences } from '../models/user-preferences';
import { CurrentUserFacade } from '../services/current-user.facade';
import { TimeFormat } from '../services/user-profile-api.service';

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
  selector: 'app-user-preferences-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    AutocompleteTextboxComponent,
    ButtonComponent,
    CardComponent,
    PageTemplateComponent,
  ],
  templateUrl: './user-preferences-page.component.html',
  styleUrl: './user-preferences-page.component.css',
})
export class UserPreferencesPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly currentUserFacade = inject(CurrentUserFacade);
  private readonly location = inject(Location);
  private readonly router = inject(Router);

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
  readonly localeOptions = supportedLanguages;

  /**
   * Available time format options presented in the UI.
   */
  readonly timeFormatOptions: TimeFormat[] = ['H24', 'H12'];

  /**
   * Full timezone catalog used by the autocomplete search.
   */
  readonly timezoneCatalog = createTimezoneCatalog();

  /**
   * Indicates whether the initial preference load is in progress.
   */
  loading = true;

  /**
   * Indicates whether a save operation is in progress.
   */
  saving = false;

  /**
   * Translation key of the current error message, if any.
   */
  errorMessage = '';

  ngOnInit(): void {
    this.loadPreferences();
  }

  /**
   * Loads the preferences of the current authenticated user and
   * populates the form.
   *
   * If no preferences are available or loading fails, an error
   * translation key is exposed to the template.
   */
  loadPreferences(): void {
    this.loading = true;
    this.errorMessage = '';

    this.currentUserFacade.preferences$
      .pipe(
        take(1),
        finalize(() => {
          this.loading = false;
        }),
      )
      .subscribe({
        next: (preferences) => {
          if (!preferences) {
            this.errorMessage = 'userPreferences.errors.load';
            return;
          }

          this.applyPreferences(preferences);
        },
        error: () => {
          this.errorMessage = 'userPreferences.errors.load';
        },
      });
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
    if (this.saving || this.form.invalid) {
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

    this.saving = true;
    this.errorMessage = '';

    this.currentUserFacade
      .updatePreferences(payload)
      .pipe(
        finalize(() => {
          this.saving = false;
        }),
      )
      .subscribe({
        next: (updatedPreferences) => {
          this.applyPreferences(updatedPreferences);
          this.goBack();
        },
        error: () => {
          this.errorMessage = 'userPreferences.errors.update';
        },
      });
  }

  /**
   * Navigates back to the previous page when possible.
   * Falls back to the application root if no browser history is available.
   */
  goBack(): void {
    if (window.history.length > 1) {
      this.location.back();
      return;
    }

    this.router.navigate(['/']);
  }

  /**
   * Display function used by the autocomplete component.
   *
   * @param option Timezone option to render
   * @returns Human-readable label shown in the input and result list
   */
  readonly timezoneDisplayWith = (option: TimezoneOption): string => option.literal;

  /**
   * Value mapper used by the autocomplete component.
   *
   * It converts the selected option into the string value stored in
   * the reactive form.
   *
   * @param option Selected timezone option
   * @returns IANA timezone identifier
   */
  readonly timezoneValueWith = (option: TimezoneOption): string => option.zoneId;

  /**
   * Resolver used by the autocomplete component when Angular writes
   * an existing form value back into the control.
   *
   * @param zoneId Stored IANA timezone identifier
   * @returns Matching timezone option or null when not found
   */
  readonly resolveTimezoneByValue = (zoneId: string): TimezoneOption | null =>
    resolveTimezoneByZoneId(this.timezoneCatalog, zoneId);

  /**
   * Search function used by the autocomplete component.
   *
   * @param query Raw user query
   * @returns Matching timezone options
   */
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

  /**
   * Applies loaded or updated preferences to the form and resets
   * form interaction state.
   *
   * @param preferences Preferences to display
   */
  private applyPreferences(preferences: UserPreferences): void {
    this.form.reset({
      locale: preferences.locale,
      timeFormat: preferences.timeFormat,
      defaultTimezone: preferences.defaultTimezone,
    });

    this.form.markAsPristine();
    this.form.markAsUntouched();
  }

}
