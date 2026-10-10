/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { ACTIVE_SCREEN_HTTP_RETRY_POLICY, HTTP_RETRY_POLICY } from '../../../core/http/http-retry.interceptor';
import { EmployeeApiService } from './employee-api.service';

describe('EmployeeApiService', () => {
  let service: EmployeeApiService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(EmployeeApiService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('sends normalized search and pagination and maps the response', () => {
    let result: unknown;
    service.search(1, 5, '  Abel  ').subscribe((value) => result = value);
    const request = http.expectOne((r) => r.url.endsWith('/employees'));
    expect(request.request.params.get('query')).toBe('Abel');
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe('5');
    expect(request.request.params.getAll('sort')).toEqual(['employeeNumber,desc']);
    expect(request.request.params.has('scheduleCode')).toBe(false);
    expect(request.request.params.has('worksiteCode')).toBe(false);
    expect(request.request.context.get(HTTP_RETRY_POLICY)).toBe(ACTIVE_SCREEN_HTTP_RETRY_POLICY);
    const employee = { employeeNumber: 'EMP-1', name: 'Abel', surname: 'Ferrer', email: 'abel@example.com', schedule: { code: 'DAY', name: 'Day shift' } };
    request.flush({ content: [employee], page: { number: 1, size: 5, totalElements: 6, totalPages: 2 } });
    expect(result).toEqual({ items: [employee], page: 1, pageSize: 5, totalItems: 6, totalPages: 2 });
  });

  it('omits blank queries and handles empty response data', () => {
    let result: unknown;
    service.search(0, 5, '  ').subscribe((value) => result = value);
    const request = http.expectOne((r) => r.url.endsWith('/employees'));
    expect(request.request.params.has('query')).toBe(false);
    request.flush({});
    expect(result).toEqual({ items: [], totalItems: 0, page: 0, pageSize: 5, totalPages: 0 });
  });
});
