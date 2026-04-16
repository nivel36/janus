/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { CreateWorksitePayload, UpdateWorksitePayload, Worksite } from '../models/worksite';

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

  constructor(private readonly http: HttpClient) {}

  /**
   * Retrieves all worksites visible to the authenticated user.
   *
   * @returns Observable emitting the complete list of worksites.
   */
  findAll(page = 0, size = 10, query = ''): Observable<WorksitePage> {
    let params = new HttpParams()
      .set('sort', 'code,desc')
      .set('page', String(page))
      .set('size', String(size));

    const normalizedQuery = query.trim();
    if (normalizedQuery !== '') {
      params = params.set('query', normalizedQuery);
    }

    return this.http.get<any>(this.baseUrl, { params }).pipe(
      map((r) => ({
        items: r.content ?? [],
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
    return this.http.get<Worksite>(`${this.baseUrl}/${encodeURIComponent(worksiteCode)}`);
  }

  /**
   * Creates a new worksite.
   *
   * @param payload Worksite data required by the create endpoint.
   * @returns Observable emitting the created worksite.
   */
  create(payload: CreateWorksitePayload): Observable<Worksite> {
    return this.http.post<Worksite>(this.baseUrl, payload);
  }

  /**
   * Updates an existing worksite identified by its code.
   *
   * @param worksiteCode Unique worksite business identifier.
   * @param payload Updated worksite data.
   * @returns Observable emitting the updated worksite.
   */
  update(worksiteCode: string, payload: UpdateWorksitePayload): Observable<Worksite> {
    return this.http.put<Worksite>(`${this.baseUrl}/${encodeURIComponent(worksiteCode)}`, payload);
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
}
