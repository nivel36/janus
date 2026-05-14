/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  CreateWorksitePayload,
  UpdateWorksitePayload,
  Worksite,
  WorksiteScope,
} from '../models/worksite';
import { Page } from '../../../shared/models/page.model';

interface WorksiteResponse {
  code: string;
  name: string;
  timeZone: string;
  scope: WorksiteScope;
  description: string | null;
  address: string | null;
  ownerEmployeeEmail?: string | null;
  active: boolean;
}

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
  private readonly baseUrl = `${environment.apiBaseUrl}/worksites`;
  private readonly http = inject(HttpClient);

  /**
   * Retrieves all worksites visible to the authenticated user.
   *
   * @returns Observable emitting the complete list of worksites.
   */
  search(page = 0, size = 10, query = ''): Observable<WorksitePage> {
    let params = new HttpParams()
      .set('sort', 'code,desc')
      .set('page', String(page))
      .set('size', String(size));

    const normalizedQuery = query.trim();
    if (normalizedQuery !== '') {
      params = params.set('query', normalizedQuery);
    }

    return this.http.get<Page<WorksiteResponse>>(this.baseUrl, { params }).pipe(
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
   * @returns Observable emitting the requested worksite.
   */
  findByCode(worksiteCode: string): Observable<Worksite> {
    return this.http
      .get<WorksiteResponse>(`${this.baseUrl}/${encodeURIComponent(worksiteCode)}`)
      .pipe(map((worksite) => this.mapWorksite(worksite)));
  }

  /**
   * Creates a new worksite.
   *
   * @param payload Worksite data required by the create endpoint.
   * @returns Observable emitting the created worksite.
   */
  create(payload: CreateWorksitePayload): Observable<Worksite> {
    return this.http
      .post<WorksiteResponse>(this.baseUrl, payload)
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
    return this.http
      .put<WorksiteResponse>(`${this.baseUrl}/${encodeURIComponent(worksiteCode)}`, payload)
      .pipe(map((worksite) => this.mapWorksite(worksite)));
  }

  /**
   * Deletes an existing worksite by its code.
   *
   * @param worksiteCode Unique worksite business identifier.
   * @returns Observable completing when deletion succeeds.
   */
  delete(worksiteCode: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${encodeURIComponent(worksiteCode)}`);
  }

  private mapWorksite(response: WorksiteResponse): Worksite {
    return {
      code: response.code,
      name: response.name,
      timeZone: response.timeZone,
      scope: response.scope,
      description: response.description,
      address: response.address,
      ownerEmployeeEmail: response.ownerEmployeeEmail ?? null,
      active: response.active,
    };
  }
}
