import { HttpErrorResponse } from '@angular/common/http';
import { MonoTypeOperatorFunction, Observable, throwError, timer } from 'rxjs';
import { mergeMap, retryWhen } from 'rxjs/operators';

/**
 * Retries transient HTTP failures while the caller keeps the subscription alive.
 *
 * Intended for read operations in active screens (e.g. polling-like UX without
 * keeping an explicit interval in the component).
 */
export function retryTransientHttpErrors<T>(
  retryDelayMs = 5_000,
  maxRetries = Number.POSITIVE_INFINITY,
): MonoTypeOperatorFunction<T> {
  return (source: Observable<T>) =>
    source.pipe(
      retryWhen((errors) =>
        errors.pipe(
          mergeMap((error: unknown, attemptIndex) => {
            const attempt = attemptIndex + 1;
            const isRetryable = isRetryableHttpError(error);

            if (!isRetryable || attempt > maxRetries) {
              return throwError(() => error);
            }

            return timer(computeBackoffWithJitter(retryDelayMs, attempt));
          }),
        ),
      ),
    );
}

function isRetryableHttpError(error: unknown): boolean {
  if (!(error instanceof HttpErrorResponse)) {
    return false;
  }

  return error.status === 0 || error.status === 502 || error.status === 503 || error.status === 504;
}

function computeBackoffWithJitter(baseDelayMs: number, attempt: number): number {
  const exponentialDelay = baseDelayMs * Math.pow(2, Math.max(0, attempt - 1));
  const cappedDelay = Math.min(exponentialDelay, 30_000);
  const jitterFactor = 0.85 + Math.random() * 0.3;

  return Math.round(cappedDelay * jitterFactor);
}
