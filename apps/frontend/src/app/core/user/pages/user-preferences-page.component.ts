/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { CommonModule, Location } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable, of, finalize, take } from 'rxjs';

import { PageTemplateComponent } from '../../../core/layout/page-template/page-template.component';
import { AutocompleteTextboxComponent } from '../../../shared/ui/autocomplete-textbox/autocomplete-textbox.component';
import { ButtonComponent } from '../../../shared/ui/button/button.component';
import { CardComponent } from '../../../shared/ui/card/card.component';
import { CurrentUserFacade } from '../services/current-user.facade';
import { TimeFormat } from '../services/user-profile-api.service';
import { UserPreferences } from '../models/user-preferences';

type TimezoneOption = {
  zoneId: string;
  literal: string;
};

/**
 * Page responsible for displaying and updating the preferences
 * of the currently authenticated user.
 *
 * Responsibilities:
 * - Load current user preferences into the reactive form
 * - Allow the user to edit those preferences
 * - Submit the updated preferences through CurrentUserFacade
 * - Provide UI helpers for timezone selection
 *
 * This component does NOT:
 * - Resolve the current username
 * - Access backend APIs directly
 * - Manage authenticated user state
 *
 * Those responsibilities belong to CurrentUserFacade.
 */
@Component({
  selector: 'app-user-preferences-page',
  standalone: true,
  imports: [
    CommonModule,
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
   */
  readonly form = this.fb.nonNullable.group({
    locale: ['es-ES', [Validators.required, Validators.pattern(/^[a-z]{2,3}-[A-Z]{2}$/)]],
    timeFormat: ['H24' as TimeFormat, Validators.required],
    defaultTimezone: ['', Validators.required],
  });

  /**
   * Auxiliary control used by the timezone autocomplete component.
   *
   * The selected option is mapped into form.controls.defaultTimezone.
   */
  readonly timezoneControl = this.fb.control<TimezoneOption | null>(null);

  /**
   * Available locale options presented in the UI.
   */
  readonly localeOptions = ['es-ES', 'en-US', 'ca-ES'];

  /**
   * Available time format options presented in the UI.
   */
  readonly timeFormatOptions: TimeFormat[] = ['H24', 'H12'];

  /**
   * Full timezone catalog used by the autocomplete search.
   */
  readonly timezoneCatalog = this.createTimezoneCatalog();

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
   * If the preferences cannot be loaded, an error message key is set.
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
   * If the operation succeeds, the page navigates back.
   * If it fails, an error message key is set.
   */
  save(): void {
    if (this.saving || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.errorMessage = '';

    const payload: UserPreferences = this.form.getRawValue();

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
   * Updates the form timezone value when the autocomplete component
   * emits a selected option.
   *
   * @param option Selected timezone option, or null when cleared
   */
  onTimezoneSelected(option: TimezoneOption | null): void {
    this.form.controls.defaultTimezone.setValue(option?.zoneId ?? '');
  }

  /**
   * Display function used by the autocomplete component.
   */
  readonly timezoneDisplayWith = (option: TimezoneOption): string => option.literal;

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
   * Applies loaded preferences to the form and synchronizes the
   * auxiliary timezone autocomplete control.
   *
   * @param preferences Preferences to display
   */
  private applyPreferences(preferences: UserPreferences): void {
    this.form.reset(preferences);
    this.timezoneControl.setValue(this.findTimezoneOption(preferences.defaultTimezone), {
      emitEvent: false,
    });
  }

  /**
   * Builds the full timezone catalog used by the autocomplete control.
   */
  private createTimezoneCatalog(): TimezoneOption[] {
    return Intl.supportedValuesOf('timeZone').map((zoneId) => ({
      zoneId,
      literal: `${zoneId} (${this.getUtcOffsetLiteral(zoneId)})`,
    }));
  }

  /**
   * Computes a human-readable UTC offset label for a timezone.
   *
   * @param zoneId IANA timezone identifier
   * @returns UTC offset literal
   */
  private getUtcOffsetLiteral(zoneId: string): string {
    const utcOffsetPart = new Intl.DateTimeFormat('en-US', {
      timeZone: zoneId,
      timeZoneName: 'shortOffset',
    })
      .formatToParts(new Date())
      .find((part) => part.type === 'timeZoneName')?.value;

    return utcOffsetPart?.replace('GMT', 'UTC') ?? 'UTC';
  }

  /**
   * Finds the timezone option corresponding to a stored timezone id.
   *
   * @param zoneId IANA timezone identifier
   * @returns Matching option or null
   */
  private findTimezoneOption(zoneId: string): TimezoneOption | null {
    return this.timezoneCatalog.find((option) => option.zoneId === zoneId) ?? null;
  }
}
