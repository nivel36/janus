import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';

export interface TimeZoneCatalogItem {
  zoneId: string;
  literal: string;
  level1: string;
  level2: string;
  utc: string;
}

export interface PagedResponse<T> {
  content: T[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

@Injectable({ providedIn: 'root' })
export class TimezoneCatalogApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/catalogs/time-zones`;

  constructor(private readonly http: HttpClient) {}

  searchTimeZones(search: string, page = 0, size = 25): Observable<PagedResponse<TimeZoneCatalogItem>> {
    const params = new HttpParams()
      .set('search', search)
      .set('page', page)
      .set('size', size);

    return this.http.get<PagedResponse<TimeZoneCatalogItem>>(this.baseUrl, { params });
  }
}
