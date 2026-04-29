import { HttpErrorResponse } from '@angular/common/http';
import { MonoTypeOperatorFunction, Observable, throwError, timer } from 'rxjs';
import { retry } from 'rxjs/operators';

/**
 * Retries transient HTTP failures with capped attempts so errors still surface.
 *
 * Intended for read operations in active screens (e.g. polling-like UX without
 * keeping an explicit interval in the component).
 */
export function retryTransientHttpErrors<T>(
  retryDelayMs = 1_000,
  maxRetries = 10,
): MonoTypeOperatorFunction<T> {
  return (source: Observable<T>) =>
    source.pipe(
      retry({
        count: maxRetries,
        delay: (error: unknown, retryCount: number) => {
          if (!isRetryableHttpError(error)) {
            return throwError(() => error);
          }

          return timer(computeBackoffWithJitter(retryDelayMs, retryCount));
        },
      }),
    );
}

function isRetryableHttpError(error: unknown): boolean {
  return (
    error instanceof HttpErrorResponse &&
    (error.status === 0 ||
      error.status === 408 ||
      error.status === 500 ||
      error.status === 502 ||
      error.status === 503 ||
      error.status === 504)
  );
}

function computeBackoffWithJitter(baseDelayMs: number, attempt: number): number {
  const exponentialDelay = baseDelayMs * Math.pow(2, Math.max(0, attempt - 1));
  const cappedDelay = Math.min(exponentialDelay, 30_000);
  const jitterFactor = 0.85 + Math.random() * 0.3;

  return Math.round(cappedDelay * jitterFactor);
}
