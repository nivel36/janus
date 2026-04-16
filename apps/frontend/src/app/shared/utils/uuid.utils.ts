/**
 * Creates a UUID string.
 *
 * <p>Uses the native {@link crypto.randomUUID} implementation when available.
 * When it is not available, it falls back to a UUID-like identifier.</p>
 *
 * @returns Generated identifier string.
 */
export function createUuid(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }

  return `${Date.now()}-${Math.random().toString(36).slice(2, 11)}`;
}
