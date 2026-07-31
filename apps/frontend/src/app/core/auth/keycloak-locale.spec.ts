/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, expect, it } from 'vitest';
import { resolveKeycloakLocale } from './keycloak-locale';

describe('resolveKeycloakLocale', () => {
  it('uses Catalan when it is the preferred browser language', () => {
    expect(resolveKeycloakLocale(['ca', 'en-US'])).toBe('ca');
  });

  it('normalizes a regional Catalan locale', () => {
    expect(resolveKeycloakLocale(['ca-ES', 'en-US'])).toBe('ca');
  });

  it('falls back to Spanish when none of the browser languages are supported', () => {
    expect(resolveKeycloakLocale(['fr-FR'])).toBe('es');
  });
});
