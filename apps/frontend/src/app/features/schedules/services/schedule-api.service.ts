/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { HttpContext } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { EMPTY, expand, map, Observable, reduce } from 'rxjs';

import { SchedulesService } from '../../../client/api/schedules.service';
import { Schedule } from '../models/schedule';
import {
  ACTIVE_SCREEN_HTTP_RETRY_POLICY,
  HTTP_RETRY_POLICY,
} from '../../../core/http/http-retry.interceptor';

export interface SchedulePage {
  items: Schedule[];
  totalItems: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

@Injectable({ providedIn: 'root' })
export class ScheduleApiService {
  private readonly api = inject(SchedulesService);

  listAll(): Observable<Schedule[]> {
    return this.search(0, 100).pipe(
      expand((page) =>
        page.page + 1 < page.totalPages ? this.search(page.page + 1, 100) : EMPTY,
      ),
      reduce((items, page) => [...items, ...page.items], [] as Schedule[]),
    );
  }

  search(page = 0, size = 10, query = ''): Observable<SchedulePage> {
    const normalizedQuery = query.trim();
    const context = new HttpContext().set(HTTP_RETRY_POLICY, ACTIVE_SCREEN_HTTP_RETRY_POLICY);
    return this.api.searchSchedules(normalizedQuery || undefined, undefined, page, size, ['code,desc'], 'body', false, { context }).pipe(
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
