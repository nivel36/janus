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
import { WorksiteApiService } from './worksite-api.service';

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
});

function worksiteResponse(code: string) {
  return {
    code,
    name: code,
    timeZone: 'Europe/Madrid',
    scope: 'OFFICE',
    description: null,
    address: null,
    active: true,
  };
}
