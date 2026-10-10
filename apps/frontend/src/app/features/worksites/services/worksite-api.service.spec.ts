/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import {
  ACTIVE_SCREEN_HTTP_RETRY_POLICY,
  HTTP_RETRY_POLICY,
} from '../../../core/http/http-retry.interceptor';
import { WorksiteApiService, WorksitePage } from './worksite-api.service';

describe('WorksiteApiService', () => {
  let service: WorksiteApiService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(WorksiteApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('does not retry findByCode callers by default', () => {
    service.findByCode('NEW').subscribe();

    const request = httpTesting.expectOne((candidate) => candidate.url.endsWith('/worksites/NEW'));
    expect(request.request.context.get(HTTP_RETRY_POLICY)).toBeNull();
    request.flush(worksiteResponse('NEW'));
  });

  it('allows active screens to opt in to retries for findByCode', () => {
    service.findByCode('MAD', ACTIVE_SCREEN_HTTP_RETRY_POLICY).subscribe();

    const request = httpTesting.expectOne((candidate) => candidate.url.endsWith('/worksites/MAD'));
    expect(request.request.context.get(HTTP_RETRY_POLICY)).toBe(ACTIVE_SCREEN_HTTP_RETRY_POLICY);
    request.flush(worksiteResponse('MAD'));
  });

  it('returns only active worksites assigned to the employee', () => {
    let worksites: unknown;
    service.searchAssignedToEmployee('EMP-0001').subscribe((result) => (worksites = result));

    const request = httpTesting.expectOne((candidate) => candidate.url.endsWith('/worksites'));
    expect(request.request.params.get('employeeNumber')).toBe('EMP-0001');
    expect(request.request.params.get('size')).toBe('100');
    request.flush({
      content: [
        worksiteResponse('ASSIGNED', 'ASSIGNED', true),
        worksiteResponse('GLOBAL', 'GLOBAL', true),
        worksiteResponse('INACTIVE', 'ASSIGNED', false),
      ],
      page: { totalElements: 3, number: 0, size: 100, totalPages: 1 },
    });

    expect(worksites).toMatchObject([{ code: 'ASSIGNED' }]);
  });

  it('filters the requested page by employee and retains global, assigned and inactive worksites', () => {
    let result: WorksitePage | undefined;
    service.search(1, 5, ' office ', 'EMP-0001').subscribe((page) => (result = page));
    const request = httpTesting.expectOne((candidate) => candidate.url.endsWith('/worksites'));
    expect(request.request.params.get('employeeNumber')).toBe('EMP-0001');
    expect(request.request.params.get('query')).toBe('office');
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe('5');
    expect(request.request.params.getAll('sort')).toEqual(['code,desc']);
    expect(request.request.context.get(HTTP_RETRY_POLICY)).toBe(ACTIVE_SCREEN_HTTP_RETRY_POLICY);
    request.flush({
      content: [worksiteResponse('GLOBAL', 'GLOBAL'), worksiteResponse('ASSIGNED'), worksiteResponse('INACTIVE', 'ASSIGNED', false)],
      page: { number: 1, totalPages: 2, totalElements: 8, size: 5 },
    });
    expect(result).toMatchObject({
      items: [
        { code: 'GLOBAL', scope: 'GLOBAL', active: true },
        { code: 'ASSIGNED', scope: 'ASSIGNED', active: true },
        { code: 'INACTIVE', scope: 'ASSIGNED', active: false },
      ],
      page: 1, pageSize: 5, totalPages: 2, totalItems: 8,
    });
  });

  it('keeps the general worksite search unfiltered by employee', () => {
    service.search().subscribe();
    const request = httpTesting.expectOne((candidate) => candidate.url.endsWith('/worksites'));
    expect(request.request.params.has('employeeNumber')).toBe(false);
    request.flush({ content: [], page: { number: 0, size: 10, totalPages: 0, totalElements: 0 } });
  });

  it('returns global worksites when the employee has no manual assignments', () => {
    let worksites: unknown;
    service.search(0, 5, '', 'EMP-0001').subscribe((result) => (worksites = result.items));
    const request = httpTesting.expectOne((candidate) => candidate.url.endsWith('/worksites'));
    expect(request.request.params.get('employeeNumber')).toBe('EMP-0001');
    request.flush({
      content: [worksiteResponse('GLOBAL', 'GLOBAL')],
      page: { number: 0, totalPages: 1, totalElements: 1, size: 100 },
    });
    expect(worksites).toMatchObject([{ code: 'GLOBAL', scope: 'GLOBAL' }]);
  });
});

function worksiteResponse(code: string, scope = 'ASSIGNED', active = true) {
  return {
    code,
    name: code,
    timeZone: 'Europe/Madrid',
    scope,
    description: null,
    address: null,
    active,
  };
}
