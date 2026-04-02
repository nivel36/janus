import { CommonModule, Location } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable, of, finalize } from 'rxjs';

import { CurrentUserFacade } from '../../../core/user/services/current-user.facade';
import { PageTemplateComponent } from '../../../core/layout/page-template/page-template.component';
import { AutocompleteTextboxComponent } from '../../../shared/ui/autocomplete-textbox/autocomplete-textbox.component';
import { ButtonComponent } from '../../../shared/ui/button/button.component';
import { CardComponent } from '../../../shared/ui/card/card.component';
import { RangeSliderComponent } from '../../../shared/ui/range-slider/range-slider.component';
import { ToggleButtonComponent } from '../../../shared/ui/toggle-button/toggle-button.component';
import { ApplicationSettings } from '../models/application-settings';
import { ApplicationSettingsApiService } from '../services/application-settings-api.service';

type TimezoneOption = {
  zoneId: string;
  literal: string;
};

@Component({
  selector: 'app-application-settings-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TranslatePipe,
    AutocompleteTextboxComponent,
    RangeSliderComponent,
    ToggleButtonComponent,
    ButtonComponent,
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
  private readonly location = inject(Location);
  private readonly router = inject(Router);

  readonly form = this.fb.nonNullable.group({
    daysUntilLocked: [0, [Validators.required, Validators.min(0)]],
    employeeWorkplaceCreationAllowed: [false],
    worksiteChangeDuringShiftAllowed: [false],
    defaultTimezone: ['', Validators.required],
  });
  readonly timezoneControl = this.fb.control<TimezoneOption | null>(null);
  readonly timezoneCatalog = this.createTimezoneCatalog();

  loading = true;
  saving = false;
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
          this.timezoneControl.setValue(this.findTimezoneOption(settings.defaultTimezone), {
            emitEvent: false,
          });

          if (!this.isAdmin) {
            this.form.disable();
            this.timezoneControl.disable();
          } else {
            this.form.enable();
            this.timezoneControl.enable();
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

  searchMethod = (query: string): Observable<TimezoneOption[]> => {
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

  onTimezoneSelected(option: TimezoneOption | null): void {
    this.form.controls.defaultTimezone.setValue(option?.zoneId ?? '');
  }

  timezoneDisplayWith = (option: TimezoneOption): string => option.literal;

  private createTimezoneCatalog(): TimezoneOption[] {
    return Intl.supportedValuesOf('timeZone').map((zoneId) => ({
      zoneId,
      literal: `${zoneId} (${this.getUtcOffsetLiteral(zoneId)})`,
    }));
  }

  private getUtcOffsetLiteral(zoneId: string): string {
    const utcOffsetPart = new Intl.DateTimeFormat('en-US', {
      timeZone: zoneId,
      timeZoneName: 'shortOffset',
    })
      .formatToParts(new Date())
      .find((part) => part.type === 'timeZoneName')?.value;

    return utcOffsetPart?.replace('GMT', 'UTC') ?? 'UTC';
  }

  private findTimezoneOption(zoneId: string): TimezoneOption | null {
    return this.timezoneCatalog.find((option) => option.zoneId === zoneId) ?? null;
  }
}
