/**
 * Creates a UUID string.
 *
 * <p>Uses the native Web Crypto implementation supported by the application's
 * browser targets.</p>
 *
 * @returns Generated identifier string.
 */
export function createUuid(): string {
  return globalThis.crypto.randomUUID();
}
