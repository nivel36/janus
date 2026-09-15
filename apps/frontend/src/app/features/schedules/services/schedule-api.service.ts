/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { HttpContext } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { SchedulesService } from '../../../api/generated/api/schedules.service';
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
