import { CommonModule, Location } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize, map } from 'rxjs';

import { CurrentUserFacade } from '../../../core/auth/current-user.facade';
import { PageTemplateComponent } from '../../../core/layout/page-template/page-template.component';
import { AutocompleteTextboxComponent } from '../../../shared/ui/autocomplete-textbox/autocomplete-textbox.component';
import { ButtonComponent } from '../../../shared/ui/button/button.component';
import { CardComponent } from '../../../shared/ui/card/card.component';
import { RangeSliderComponent } from '../../../shared/ui/range-slider/range-slider.component';
import { ToggleButtonComponent } from '../../../shared/ui/toggle-button/toggle-button.component';
import { ApplicationSettings } from '../models/application-settings';
import { ApplicationSettingsApiService } from '../services/application-settings-api.service';
import { TimeZoneCatalogItem, TimezoneCatalogApiService } from '../services/timezone-catalog-api.service';

@Component({
  selector: 'app-application-settings-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TranslatePipe,
    RangeSliderComponent,
    ToggleButtonComponent,
    ButtonComponent,
    AutocompleteTextboxComponent,
    CardComponent,
    PageTemplateComponent,
  ],
  templateUrl: './application-settings-page.component.html',
  styleUrl: './application-settings-page.component.css',
})
export class ApplicationSettingsPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly currentUser = inject(CurrentUserFacade);
  private readonly settingsApiService = inject(ApplicationSettingsApiService);
  private readonly timezoneCatalogApiService = inject(TimezoneCatalogApiService);
  private readonly location = inject(Location);
  private readonly router = inject(Router);

  readonly form = this.fb.nonNullable.group({
    daysUntilLocked: [0, [Validators.required, Validators.min(0)]],
    employeeWorkplaceCreationAllowed: [false],
    worksiteChangeDuringShiftAllowed: [false],
    defaultTimezone: ['', [Validators.required]],
  });

  loading = true;
  saving = false;
  errorMessage = '';

  get isAdmin(): boolean {
    return this.currentUser.isAdmin();
  }

  get daysUntilLockedSliderMax(): number {
    return Math.max(31, this.form.controls.daysUntilLocked.value);
  }

  readonly searchTimeZones = (query: string) =>
    this.timezoneCatalogApiService.searchTimeZones(query).pipe(map((response) => response.content));

  readonly formatTimeZoneOption = (option: TimeZoneCatalogItem): string => option.literal;

  onDefaultTimezoneChange(option: TimeZoneCatalogItem | null): void {
    this.form.controls.defaultTimezone.setValue(option?.zoneId ?? '');
  }

  ngOnInit(): void {
    this.loadSettings();
  }

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
          this.form.reset(settings);

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

  goBack(): void {
    if (window.history.length > 1) {
      this.location.back();
      return;
    }

    this.router.navigate(['/']);
  }

  save(): void {
    if (!this.isAdmin || this.saving || this.form.invalid) {
      return;
    }

    this.saving = true;
    this.errorMessage = '';

    const payload: ApplicationSettings = this.form.getRawValue();

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
          this.goBack();
        },
        error: () => {
          this.errorMessage = 'applicationSettings.errors.update';
        },
      });
  }
}
