/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { HttpContext, HttpErrorResponse, HttpRequest, HttpResponse } from '@angular/common/http';
import { firstValueFrom, defer, of, throwError } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { HTTP_RETRY_POLICY, httpRetryInterceptor } from './http-retry.interceptor';

describe('httpRetryInterceptor', () => {
  afterEach(() => vi.useRealTimers());

  it.each([0, 408, 500, 502, 503, 504])(
    'retries transient HTTP status %i when the request opts in',
    async (status) => {
      vi.useFakeTimers();
      let attempts = 0;
      const request = retryingRequest('GET', 2);
      const result = firstValueFrom(
        httpRetryInterceptor(request, () =>
          defer(() => {
            attempts += 1;
            return attempts < 3
              ? throwError(() => new HttpErrorResponse({ status }))
              : of(new HttpResponse({ status: 200 }));
          }),
        ),
      );

      await vi.runAllTimersAsync();

      expect(await result).toBeInstanceOf(HttpResponse);
      expect(attempts).toBe(3);
    },
  );

  it('does not retry a non-transient HTTP error', async () => {
    let attempts = 0;
    const error = new HttpErrorResponse({ status: 404 });

    await expect(
      firstValueFrom(
        httpRetryInterceptor(retryingRequest('GET', 2), () =>
          defer(() => {
            attempts += 1;
            return throwError(() => error);
          }),
        ),
      ),
    ).rejects.toBe(error);
    expect(attempts).toBe(1);
  });

  it('does not retry a request without the opt-in context', async () => {
    let attempts = 0;
    const error = new HttpErrorResponse({ status: 503 });

    await expect(
      firstValueFrom(
        httpRetryInterceptor(new HttpRequest('GET', '/api/example'), () =>
          defer(() => {
            attempts += 1;
            return throwError(() => error);
          }),
        ),
      ),
    ).rejects.toBe(error);
    expect(attempts).toBe(1);
  });

  it('limits automatic retries to idempotent methods', async () => {
    let attempts = 0;

    await expect(
      firstValueFrom(
        httpRetryInterceptor(retryingRequest('POST', 2), () =>
          defer(() => {
            attempts += 1;
            return throwError(() => new HttpErrorResponse({ status: 500 }));
          }),
        ),
      ),
    ).rejects.toBeInstanceOf(HttpErrorResponse);
    expect(attempts).toBe(1);
  });

  it('retries a non-idempotent method when the caller expressly authorizes it', async () => {
    vi.useFakeTimers();
    let attempts = 0;
    const request = new HttpRequest('POST', '/api/example', null, {
      context: new HttpContext().set(HTTP_RETRY_POLICY, {
        retries: 1,
        baseDelayMs: 0,
        retryNonIdempotent: true,
      }),
    });
    const result = firstValueFrom(
      httpRetryInterceptor(request, () =>
        defer(() => {
          attempts += 1;
          return attempts === 1
            ? throwError(() => new HttpErrorResponse({ status: 502 }))
            : of(new HttpResponse({ status: 200 }));
        }),
      ),
    );

    await vi.runAllTimersAsync();

    expect(await result).toBeInstanceOf(HttpResponse);
    expect(attempts).toBe(2);
  });
});

function retryingRequest(method: string, retries: number): HttpRequest<unknown> {
  return new HttpRequest(method, '/api/example', null, {
    context: new HttpContext().set(HTTP_RETRY_POLICY, { retries, baseDelayMs: 0 }),
  });
}
