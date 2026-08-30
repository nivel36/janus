import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpContext } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApplicationSettings } from '../models/application-settings';
import {
  ACTIVE_SCREEN_HTTP_RETRY_POLICY,
  HTTP_RETRY_POLICY,
} from '../../../core/http/http-retry.interceptor';

@Injectable({ providedIn: 'root' })
export class ApplicationSettingsApiService {
  private readonly http = inject(HttpClient);

  private readonly baseUrl = `${environment.apiBaseUrl}/applicationsettings`;

  find(): Observable<ApplicationSettings> {
    return this.http.get<ApplicationSettings>(this.baseUrl, {
      context: new HttpContext().set(HTTP_RETRY_POLICY, ACTIVE_SCREEN_HTTP_RETRY_POLICY),
    });
  }

  update(payload: ApplicationSettings): Observable<ApplicationSettings> {
    return this.http.put<ApplicationSettings>(this.baseUrl, payload);
  }
}
