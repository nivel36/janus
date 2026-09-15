/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { HttpContext } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { WorksitesService } from '../../../api/generated/api/worksites.service';
import { WorksiteResponse } from '../../../api/generated/model/worksiteResponse';
import { WorksiteScope as ApiWorksiteScope } from '../../../api/generated/model/worksiteScope';
import {
  CreateWorksitePayload,
  UpdateWorksitePayload,
  Worksite,
} from '../models/worksite';
import {
  ACTIVE_SCREEN_HTTP_RETRY_POLICY,
  HTTP_RETRY_POLICY,
  type HttpRetryPolicy,
} from '../../../core/http/http-retry.interceptor';

export interface WorksitePage {
  items: Worksite[];
  totalItems: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

/**
 * Provides CRUD operations for worksite resources using the backend REST API.
 *
 * All methods are thin wrappers around HTTP calls and return cold observables
 * that execute when subscribed.
 */
@Injectable({ providedIn: 'root' })
export class WorksiteApiService {
  private readonly api = inject(WorksitesService);

  /**
   * Retrieves all worksites visible to the authenticated user.
   *
   * @returns Observable emitting the complete list of worksites.
   */
  search(page = 0, size = 10, query = ''): Observable<WorksitePage> {
    const normalizedQuery = query.trim();
    const context = new HttpContext().set(HTTP_RETRY_POLICY, ACTIVE_SCREEN_HTTP_RETRY_POLICY);
    return this.api.searchWorksites(normalizedQuery || undefined, undefined, page, size, ['code,desc'], 'body', false, { context }).pipe(
      map((r) => ({
        items: (r.content ?? []).map((worksite) => this.mapWorksite(worksite)),
        totalItems: r.page?.totalElements ?? 0,
        page: r.page?.number ?? page,
        pageSize: r.page?.size ?? size,
        totalPages: r.page?.totalPages ?? 0,
      })),
    );
  }

  /**
   * Retrieves a single worksite by its unique code.
   *
   * @param worksiteCode Unique worksite business identifier.
   * @param retryPolicy Optional retry policy for active-screen callers.
   * @returns Observable emitting the requested worksite.
   */
  findByCode(
    worksiteCode: string,
    retryPolicy: HttpRetryPolicy | null = null,
  ): Observable<Worksite> {
    return this.api
      .findWorksite(worksiteCode, 'body', false, { context: new HttpContext().set(HTTP_RETRY_POLICY, retryPolicy) })
      .pipe(map((worksite) => this.mapWorksite(worksite)));
  }

  /**
   * Creates a new worksite.
   *
   * @param payload Worksite data required by the create endpoint.
   * @returns Observable emitting the created worksite.
   */
  create(payload: CreateWorksitePayload): Observable<Worksite> {
    return this.api.createWorksite({ ...payload, scope: payload.scope as ApiWorksiteScope })
      .pipe(map((worksite) => this.mapWorksite(worksite)));
  }

  /**
   * Updates an existing worksite identified by its code.
   *
   * @param worksiteCode Unique worksite business identifier.
   * @param payload Updated worksite data.
   * @returns Observable emitting the updated worksite.
   */
  update(worksiteCode: string, payload: UpdateWorksitePayload): Observable<Worksite> {
    return this.api.updateWorksite(worksiteCode, {
      ...payload,
      scope: payload.scope as ApiWorksiteScope,
    })
      .pipe(map((worksite) => this.mapWorksite(worksite)));
  }

  /**
   * Deletes an existing worksite by its code.
   *
   * @param worksiteCode Unique worksite business identifier.
   * @returns Observable completing when deletion succeeds.
   */
  delete(worksiteCode: string): Observable<void> {
    return this.api.deleteWorksite(worksiteCode);
  }

  private mapWorksite(response: WorksiteResponse): Worksite {
    return {
      code: response.code,
      name: response.name,
      timeZone: response.timeZone,
      scope: response.scope,
      description: response.description ?? null,
      address: response.address ?? null,
      ownerEmployeeEmail: response.ownerEmployeeEmail ?? null,
      active: response.active,
    };
  }
}
