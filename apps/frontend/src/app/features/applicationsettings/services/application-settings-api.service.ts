import { HttpContext } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApplicationSettingsService as ApplicationSettingsTransportService } from '../../../api/generated/api/applicationSettings.service';
import { ApplicationSettings } from '../models/application-settings';
import {
  ACTIVE_SCREEN_HTTP_RETRY_POLICY,
  HTTP_RETRY_POLICY,
} from '../../../core/http/http-retry.interceptor';

@Injectable({ providedIn: 'root' })
export class ApplicationSettingsApiService {
  private readonly api = inject(ApplicationSettingsTransportService);

  find(): Observable<ApplicationSettings> {
    const context = new HttpContext().set(HTTP_RETRY_POLICY, ACTIVE_SCREEN_HTTP_RETRY_POLICY);
    return this.api.findApplicationSettings('body', false, { context });
  }

  update(payload: ApplicationSettings): Observable<ApplicationSettings> {
    return this.api.updateApplicationSettings(payload);
  }
}
