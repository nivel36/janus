import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthService } from '../../../core/auth/auth.service';
import { ApplicationSettingsApiService } from '../services/application-settings-api.service';
import { ApplicationSettings } from '../models/application-settings';

@Component({
  selector: 'app-application-settings-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslatePipe],
  templateUrl: './application-settings-page.component.html',
  styleUrl: './application-settings-page.component.css',
})
export class ApplicationSettingsPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly settingsApiService = inject(ApplicationSettingsApiService);

  readonly form = this.fb.nonNullable.group({
    daysUntilLocked: [0, [Validators.required, Validators.min(0)]],
    employeeWorkplaceCreationAllowed: [false, [Validators.required]],
    worksiteChangeDuringShiftAllowed: [false, [Validators.required]],
  });

  loading = true;
  saving = false;
  errorMessage = '';
  successMessage = '';

  get isAdmin(): boolean {
    return this.authService.hasRealmRole('JANUS_ADMIN');
  }

  ngOnInit(): void {
    this.loadSettings();
  }

  loadSettings(): void {
    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.settingsApiService.find().subscribe({
      next: (settings) => {
        this.form.reset(settings);
        if (!this.isAdmin) {
          this.form.disable();
        } else {
          this.form.enable();
        }
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'applicationSettings.errors.load';
      },
    });
  }

  save(): void {
    if (!this.isAdmin || this.form.invalid || this.saving) {
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';
    const payload: ApplicationSettings = this.form.getRawValue();

    this.settingsApiService.update(payload).subscribe({
      next: (updatedSettings) => {
        this.form.reset(updatedSettings);
        this.saving = false;
        this.successMessage = 'applicationSettings.messages.updated';
      },
      error: () => {
        this.saving = false;
        this.errorMessage = 'applicationSettings.errors.update';
      },
    });
  }
}
