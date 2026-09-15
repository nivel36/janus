/**
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  HttpContext,
  HttpErrorResponse,
  HttpHeaders,
  HttpRequest,
  HttpResponse,
} from '@angular/common/http';
import { firstValueFrom, defer, of, throwError } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';

import {
  ACTIVE_SCREEN_HTTP_RETRY_POLICY,
  HTTP_RETRY_POLICY,
  httpRetryInterceptor,
} from './http-retry.interceptor';

describe('httpRetryInterceptor', () => {
  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it.each([0, 502, 503, 504])(
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

  it.each([408, 500])('does not retry HTTP status %i', async (status) => {
    let attempts = 0;
    const error = new HttpErrorResponse({ status });

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

  it('keeps active-screen retries deliberately small and time-bounded', async () => {
    vi.useFakeTimers();
    let attempts = 0;
    const error = new HttpErrorResponse({ status: 503 });
    const request = new HttpRequest('GET', '/api/example', null, {
      context: new HttpContext().set(HTTP_RETRY_POLICY, ACTIVE_SCREEN_HTTP_RETRY_POLICY),
    });
    const result = firstValueFrom(
      httpRetryInterceptor(request, () =>
        defer(() => {
          attempts += 1;
          return throwError(() => error);
        }),
      ),
    );

    const rejection = expect(result).rejects.toBe(error);
    await vi.runAllTimersAsync();
    await rejection;

    expect(ACTIVE_SCREEN_HTTP_RETRY_POLICY).toEqual({
      retries: 2,
      baseDelayMs: 1_000,
      maxDelayBudgetMs: 30_000,
    });
    expect(attempts).toBe(3);
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

  it('re-enters downstream interceptors so every retry can use a current bearer token', async () => {
    vi.useFakeTimers();
    const authorizationHeaders: string[] = [];
    let tokenVersion = 0;
    const result = firstValueFrom(
      httpRetryInterceptor(retryingRequest('GET', 1), (request) => {
        tokenVersion += 1;
        const authorizedRequest = request.clone({
          setHeaders: { Authorization: `Bearer token-${tokenVersion}` },
        });
        authorizationHeaders.push(authorizedRequest.headers.get('Authorization')!);

        return tokenVersion === 1
          ? throwError(() => new HttpErrorResponse({ status: 503 }))
          : of(new HttpResponse({ status: 200 }));
      }),
    );

    await vi.runAllTimersAsync();

    expect(await result).toBeInstanceOf(HttpResponse);
    expect(authorizationHeaders).toEqual(['Bearer token-1', 'Bearer token-2']);
  });

  it('adds jitter around the exponential retry delay', async () => {
    vi.useFakeTimers();
    vi.spyOn(Math, 'random').mockReturnValue(0);
    let attempts = 0;
    const request = new HttpRequest('GET', '/api/example', null, {
      context: new HttpContext().set(HTTP_RETRY_POLICY, {
        retries: 1,
        baseDelayMs: 100,
      }),
    });
    const result = firstValueFrom(
      httpRetryInterceptor(request, () => {
        attempts += 1;
        return attempts === 1
          ? throwError(() => new HttpErrorResponse({ status: 503 }))
          : of(new HttpResponse({ status: 200 }));
      }),
    );

    await vi.advanceTimersByTimeAsync(84);
    expect(attempts).toBe(1);

    await vi.advanceTimersByTimeAsync(1);
    expect(await result).toBeInstanceOf(HttpResponse);
    expect(attempts).toBe(2);
  });

  it('honours Retry-After seconds instead of the exponential delay', async () => {
    vi.useFakeTimers();
    let attempts = 0;
    const result = firstValueFrom(
      httpRetryInterceptor(retryingRequest('GET', 1), () => {
        attempts += 1;
        return attempts === 1
          ? throwError(
              () =>
                new HttpErrorResponse({
                  status: 503,
                  headers: new HttpHeaders({ 'Retry-After': '2' }),
                }),
            )
          : of(new HttpResponse({ status: 200 }));
      }),
    );

    await vi.advanceTimersByTimeAsync(1_999);
    expect(attempts).toBe(1);
    await vi.advanceTimersByTimeAsync(1);

    expect(await result).toBeInstanceOf(HttpResponse);
    expect(attempts).toBe(2);
  });

  it('does not schedule a retry beyond the total delay budget', async () => {
    const error = new HttpErrorResponse({
      status: 503,
      headers: new HttpHeaders({ 'Retry-After': '120' }),
    });
    let attempts = 0;
    const request = new HttpRequest('GET', '/api/example', null, {
      context: new HttpContext().set(HTTP_RETRY_POLICY, {
        retries: 2,
        baseDelayMs: 100,
        maxDelayBudgetMs: 30_000,
      }),
    });

    await expect(
      firstValueFrom(
        httpRetryInterceptor(request, () => {
          attempts += 1;
          return throwError(() => error);
        }),
      ),
    ).rejects.toBe(error);
    expect(attempts).toBe(1);
  });

  it('charges only scheduled delays to the delay budget', async () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-01-01T00:00:00Z'));
    let attempts = 0;
    const request = new HttpRequest('GET', '/api/example', null, {
      context: new HttpContext().set(HTTP_RETRY_POLICY, {
        retries: 1,
        baseDelayMs: 100,
        maxDelayBudgetMs: 30_000,
      }),
    });
    const result = firstValueFrom(
      httpRetryInterceptor(request, () => {
        attempts += 1;
        if (attempts === 1) {
          vi.setSystemTime(new Date('2026-01-01T00:01:00Z'));
          return throwError(
            () =>
              new HttpErrorResponse({
                status: 503,
                headers: new HttpHeaders({ 'Retry-After': '0' }),
              }),
          );
        }
        return of(new HttpResponse({ status: 200 }));
      }),
    );

    await vi.runAllTimersAsync();

    expect(await result).toBeInstanceOf(HttpResponse);
    expect(attempts).toBe(2);
  });

  it('accumulates scheduled delays when enforcing the delay budget', async () => {
    vi.useFakeTimers();
    const error = new HttpErrorResponse({
      status: 503,
      headers: new HttpHeaders({ 'Retry-After': '20' }),
    });
    let attempts = 0;
    const request = new HttpRequest('GET', '/api/example', null, {
      context: new HttpContext().set(HTTP_RETRY_POLICY, {
        retries: 2,
        baseDelayMs: 100,
        maxDelayBudgetMs: 30_000,
      }),
    });
    const result = firstValueFrom(
      httpRetryInterceptor(request, () => {
        attempts += 1;
        return throwError(() => error);
      }),
    );

    const rejection = expect(result).rejects.toBe(error);
    await vi.runAllTimersAsync();
    await rejection;

    expect(attempts).toBe(2);
  });
});

function retryingRequest(method: string, retries: number): HttpRequest<unknown> {
  return new HttpRequest(method, '/api/example', null, {
    context: new HttpContext().set(HTTP_RETRY_POLICY, { retries, baseDelayMs: 0 }),
  });
}
