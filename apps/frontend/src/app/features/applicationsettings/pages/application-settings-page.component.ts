import { Location } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable, of, finalize } from 'rxjs';

import { CurrentUserFacade } from '../../../core/user/services/current-user.facade';
import { TimezoneOption } from '../../../shared/models/timezone-option.model';
import { PageTemplateComponent } from '../../../core/layout/page-template/page-template.component';
import {
  createTimezoneCatalog,
  resolveTimezoneByZoneId,
} from '../../../shared/utils/timezone-catalog.util';
import { AutocompleteTextboxComponent } from '../../../shared/ui/autocomplete-textbox/autocomplete-textbox.component';
import { ButtonComponent } from '../../../shared/ui/button/button.component';
import { CardComponent } from '../../../shared/ui/card/card.component';
import { RangeSliderComponent } from '../../../shared/ui/range-slider/range-slider.component';
import { ToggleButtonComponent } from '../../../shared/ui/toggle-button/toggle-button.component';
import { ApplicationSettings } from '../models/application-settings';
import { ApplicationSettingsApiService } from '../services/application-settings-api.service';

@Component({
  selector: 'app-application-settings-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    AutocompleteTextboxComponent,
    RangeSliderComponent,
    ToggleButtonComponent,
    ButtonComponent,
    CardComponent,
    PageTemplateComponent
],
  templateUrl: './application-settings-page.component.html',
  styleUrl: './application-settings-page.component.css',
})
export class ApplicationSettingsPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly currentUser = inject(CurrentUserFacade);
  private readonly settingsApiService = inject(ApplicationSettingsApiService);
  private readonly location = inject(Location);
  private readonly router = inject(Router);

  /**
   * Main form containing editable application settings.
   *
   * The timezone control stores the IANA timezone identifier as a string,
   * or null when no valid selection exists.
   */
  readonly form = this.fb.nonNullable.group({
    daysUntilLocked: [0, [Validators.required, Validators.min(0)]],
    employeeWorkplaceCreationAllowed: [false],
    worksiteChangeDuringShiftAllowed: [false],
    employeeManualTimelogEntryAllowed: [false],
    defaultTimezone: ['Europe/Madrid', Validators.required],
  });

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

  get isAdmin(): boolean {
    return this.currentUser.isAdmin();
  }

  get daysUntilLockedSliderMax(): number {
    return Math.max(31, this.form.controls.daysUntilLocked.value);
  }

  ngOnInit(): void {
    this.loadSettings();
  }

  /**
   * Loads the settings of the current application and populates the form.
   *
   * If no preferences are available or loading fails, an error
   * translation key is exposed to the template.
   */
  loadSettings(): void {
    this.loading = true;
    this.errorMessage = '';

    this.settingsApiService
      .find()
      .pipe(
        finalize(() => {
          this.loading = false;
        }),
      )
      .subscribe({
        next: (settings) => {
          if (!settings) {
            this.errorMessage = 'applicationSettings.errors.load';
            return;
          }

          this.applyApplicationSettings(settings);

          if (!this.isAdmin) {
            this.form.disable();
          } else {
            this.form.enable();
          }
        },
        error: () => {
          this.errorMessage = 'applicationSettings.errors.load';
        },
      });
  }

  /**
   * Applies loaded or updated ApplicationSettings to the form and resets
   * form interaction state.
   *
   * @param preferences applicationSettings to display
   */
  private applyApplicationSettings(applicationSettings: ApplicationSettings): void {
    this.form.reset({
      daysUntilLocked: applicationSettings.daysUntilLocked,
      employeeWorkplaceCreationAllowed: applicationSettings.employeeWorkplaceCreationAllowed,
      worksiteChangeDuringShiftAllowed: applicationSettings.worksiteChangeDuringShiftAllowed,
      employeeManualTimelogEntryAllowed: applicationSettings.employeeManualTimelogEntryAllowed,
      defaultTimezone: applicationSettings.defaultTimezone,
    });
  }

  cancel(): void {
    this.router.navigate(['/']);
  }

  save(): void {
    if (!this.isAdmin || this.saving || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload: ApplicationSettings = this.form.getRawValue();

    this.saving = true;
    this.errorMessage = '';

    this.settingsApiService
      .update(payload)
      .pipe(
        finalize(() => {
          this.saving = false;
        }),
      )
      .subscribe({
        next: (updatedSettings) => {
          this.form.reset(updatedSettings);
          this.cancel();
        },
        error: () => {
          this.errorMessage = 'applicationSettings.errors.update';
        },
      });
  }

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
}
