/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { HttpContextToken, HttpErrorResponse, type HttpInterceptorFn } from '@angular/common/http';
import { defer, throwError, timer } from 'rxjs';
import { retry } from 'rxjs/operators';

export interface HttpRetryPolicy {
  /** Number of attempts after the initial request. */
  retries: number;
  /** Delay before the first retry; later delays use exponential backoff. */
  baseDelayMs: number;
  /** Explicitly allow retries for non-idempotent methods such as POST and PATCH. */
  retryNonIdempotent?: boolean;
}

/** A null value keeps retries disabled unless a caller explicitly opts in. */
export const HTTP_RETRY_POLICY = new HttpContextToken<HttpRetryPolicy | null>(() => null);

/** Retry policy for GET requests backing active screens. */
export const ACTIVE_SCREEN_HTTP_RETRY_POLICY: HttpRetryPolicy = {
  retries: 10,
  baseDelayMs: 1_000,
};

const TRANSIENT_HTTP_STATUSES = new Set([0, 408, 500, 502, 503, 504]);
const IDEMPOTENT_METHODS = new Set(['GET', 'HEAD', 'OPTIONS', 'PUT', 'DELETE', 'TRACE']);

export const httpRetryInterceptor: HttpInterceptorFn = (request, next) => {
  const policy = request.context.get(HTTP_RETRY_POLICY);
  const mayRetryMethod =
    IDEMPOTENT_METHODS.has(request.method.toUpperCase()) || policy?.retryNonIdempotent === true;

  if (!policy || policy.retries <= 0 || !mayRetryMethod) {
    return next(request);
  }

  // Re-enter the downstream interceptor chain for every subscription so retries
  // can obtain a freshly refreshed bearer token instead of reusing a cloned request.
  return defer(() => next(request)).pipe(
    retry({
      count: policy.retries,
      delay: (error: unknown, retryCount: number) => {
        if (!isTransientHttpError(error)) {
          return throwError(() => error);
        }

        return timer(computeRetryDelay(policy.baseDelayMs, retryCount));
      },
    }),
  );
};

function isTransientHttpError(error: unknown): boolean {
  return error instanceof HttpErrorResponse && TRANSIENT_HTTP_STATUSES.has(error.status);
}

function computeRetryDelay(baseDelayMs: number, attempt: number): number {
  const exponentialDelay = Math.max(0, baseDelayMs) * Math.pow(2, Math.max(0, attempt - 1));
  const cappedDelay = Math.min(exponentialDelay, 30_000);
  const jitterFactor = 0.85 + Math.random() * 0.3;

  return Math.round(cappedDelay * jitterFactor);
}
