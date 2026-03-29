import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApplicationSettings } from '../models/application-settings';

@Injectable({ providedIn: 'root' })
export class ApplicationSettingsApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/applicationsettings`;

  constructor(private readonly http: HttpClient) {}

  find(): Observable<ApplicationSettings> {
    return this.http.get<ApplicationSettings>(this.baseUrl);
  }

  update(payload: ApplicationSettings): Observable<ApplicationSettings> {
    return this.http.put<ApplicationSettings>(this.baseUrl, payload);
  }
}
