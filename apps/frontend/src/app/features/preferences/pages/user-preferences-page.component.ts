import { CommonModule, Location } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable, finalize, of } from 'rxjs';

import { PageTemplateComponent } from '../../../core/layout/page-template/page-template.component';
import { AutocompleteTextboxComponent } from '../../../shared/ui/autocomplete-textbox/autocomplete-textbox.component';
import { ButtonComponent } from '../../../shared/ui/button/button.component';
import { CardComponent } from '../../../shared/ui/card/card.component';
import { AppPreferences, PreferencesService, TimeFormat } from '../preferences.service';

type TimezoneOption = {
  zoneId: string;
  literal: string;
};

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
  private readonly preferencesService = inject(PreferencesService);
  private readonly location = inject(Location);
  private readonly router = inject(Router);

  readonly form = this.fb.nonNullable.group({
    locale: ['es-ES', [Validators.required, Validators.pattern(/^[a-z]{2,3}-[A-Z]{2}$/)]],
    timeFormat: ['H24' as TimeFormat, Validators.required],
    defaultTimezone: ['', Validators.required],
  });
  readonly timezoneControl = this.fb.control<TimezoneOption | null>(null);
  readonly timezoneCatalog = this.createTimezoneCatalog();
  readonly localeOptions = ['es-ES', 'en-US', 'ca-ES'];
  readonly timeFormatOptions: TimeFormat[] = ['H24', 'H12'];

  loading = true;
  saving = false;
  errorMessage = '';

  ngOnInit(): void {
    this.loadPreferences();
  }

  loadPreferences(): void {
    this.loading = true;
    this.errorMessage = '';

    this.preferencesService
      .load(true)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (preferences) => {
          this.form.reset(preferences);
          this.timezoneControl.setValue(this.findTimezoneOption(preferences.defaultTimezone), {
            emitEvent: false,
          });
        },
        error: () => {
          this.errorMessage = 'userPreferences.errors.load';
        },
      });
  }

  save(): void {
    if (this.saving || this.form.invalid) {
      return;
    }

    this.saving = true;
    this.errorMessage = '';

    const payload: AppPreferences = this.form.getRawValue();

    this.preferencesService
      .update(payload)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => this.goBack(),
        error: () => {
          this.errorMessage = 'userPreferences.errors.update';
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

  onTimezoneSelected(option: TimezoneOption | null): void {
    this.form.controls.defaultTimezone.setValue(option?.zoneId ?? '');
  }

  timezoneDisplayWith = (option: TimezoneOption): string => option.literal;

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
      .find((part) => part.type === 'timeZoneName')
      ?.value;

    return utcOffsetPart?.replace('GMT', 'UTC') ?? 'UTC';
  }

  private findTimezoneOption(zoneId: string): TimezoneOption | null {
    return this.timezoneCatalog.find((option) => option.zoneId === zoneId) ?? null;
  }
}
