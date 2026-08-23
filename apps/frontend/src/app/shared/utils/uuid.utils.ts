/**
 * Creates a UUID string.
 *
 * <p>Uses the native Web Crypto implementation supported by the application's
 * browser targets. In browsers, this API is only available in a secure context
 * (HTTPS, or HTTP on a potentially trustworthy origin such as localhost).</p>
 *
 * @returns Generated identifier string.
 */
export function createUuid(): string {
  return globalThis.crypto.randomUUID();
}
