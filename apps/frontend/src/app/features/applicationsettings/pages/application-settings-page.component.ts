import { Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { CurrentUserFacade } from '../../../core/user/services/current-user.facade';
import { PageTemplateComponent } from '../../../core/layout/page-template/page-template.component';
import { TimezoneCatalog } from '../../../shared/services/timezone-catalog.service';
import { AutocompleteTextboxComponent } from '../../../shared/ui/autocomplete-textbox/autocomplete-textbox.component';
import { AutocompleteValueAccessorDirective } from '../../../shared/ui/autocomplete-textbox/autocomplete-value-accessor.directive';
import { ButtonComponent } from '../../../shared/ui/button/button.component';
import { FieldComponent } from '../../../shared/ui/field/field.component';
import { RangeSliderComponent } from '../../../shared/ui/range-slider/range-slider.component';
import { ToggleButtonComponent } from '../../../shared/ui/toggle-button/toggle-button.component';
import { ApplicationSettings } from '../models/application-settings';
import { ApplicationSettingsApiService } from '../services/application-settings-api.service';

import { MessageComponent } from '../../../shared/ui/message/message.component';

@Component({
  selector: 'app-application-settings-page',
  standalone: true,
  imports: [
    MessageComponent,
    ReactiveFormsModule,
    TranslatePipe,
    AutocompleteTextboxComponent,
    AutocompleteValueAccessorDirective,
    FieldComponent,
    RangeSliderComponent,
    ToggleButtonComponent,
    ButtonComponent,
    PageTemplateComponent,
  ],
  templateUrl: './application-settings-page.component.html',
})
export class ApplicationSettingsPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly currentUser = inject(CurrentUserFacade);
  private readonly settingsApiService = inject(ApplicationSettingsApiService);
  private readonly router = inject(Router);
  readonly timezoneCatalog = inject(TimezoneCatalog);
  readonly timezoneSearch = this.timezoneCatalog.createSearchState(this.destroyRef);

  private readonly settingsResource = rxResource({
    stream: () => this.settingsApiService.find(),
  });

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
   * Indicates whether the initial preference load is in progress.
   */
  readonly loading = computed(() => this.settingsResource.isLoading());

  /**
   * Indicates whether a save operation is in progress.
   */
  readonly saving = signal(false);

  /**
   * Translation key of the current error message, if any.
   */
  private readonly saveErrorMessage = signal('');

  readonly errorMessage = computed(() =>
    this.settingsResource.error() ? 'applicationSettings.errors.load' : this.saveErrorMessage(),
  );

  get isAdmin(): boolean {
    return this.currentUser.isAdmin();
  }

  get daysUntilLockedSliderMax(): number {
    return Math.max(31, this.form.controls.daysUntilLocked.value);
  }

  private readonly populateFormEffect = effect(() => {
    if (this.settingsResource.hasValue()) {
      const settings = this.settingsResource.value();
      if (settings) {
        this.applyApplicationSettings(settings);
      }
    }

    if (this.isAdmin) {
      this.form.enable();
    } else {
      this.form.disable();
    }
  });

  /**
   * Loads the settings of the current application and populates the form.
   *
   * If no preferences are available or loading fails, an error
   * translation key is exposed to the template.
   */
  loadSettings(): void {
    this.saveErrorMessage.set('');
    this.settingsResource.reload();
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
    if (!this.isAdmin || this.saving() || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload: ApplicationSettings = this.form.getRawValue();

    this.saving.set(true);
    this.saveErrorMessage.set('');

    this.settingsApiService
      .update(payload)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.saving.set(false);
        }),
      )
      .subscribe({
        next: (updatedSettings) => {
          this.form.reset(updatedSettings);
          this.cancel();
        },
        error: () => {
          this.saveErrorMessage.set('applicationSettings.errors.update');
        },
      });
  }
}
