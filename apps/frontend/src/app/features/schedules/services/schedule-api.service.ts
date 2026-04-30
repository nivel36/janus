import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { Page } from '../../../shared/models/page.model';
import { Schedule } from '../models/schedule';

export interface SchedulePage {
  items: Schedule[];
  totalItems: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

@Injectable({ providedIn: 'root' })
export class ScheduleApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/schedules`;
  private readonly http = inject(HttpClient);

  findAll(page = 0, size = 10): Observable<SchedulePage> {
    const params = new HttpParams().set('sort', 'code,desc').set('page', String(page)).set('size', String(size));

    return this.http.get<Page<Schedule>>(this.baseUrl, { params }).pipe(
      map((r) => ({
        items: r.content ?? [],
        totalItems: r.page?.totalElements ?? 0,
        page: r.page?.number ?? page,
        pageSize: r.page?.size ?? size,
        totalPages: r.page?.totalPages ?? 0,
      })),
    );
  }
}
