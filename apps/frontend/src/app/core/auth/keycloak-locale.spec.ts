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

  it('uses the resolved locale language for uppercase and alternative regions', () => {
    expect(resolveKeycloakLocale(['ES-mx'])).toBe('es');
  });

  it('uses the resolved locale language when the preference has an extension', () => {
    expect(resolveKeycloakLocale(['ca-ES-u-ca-gregory'])).toBe('ca');
  });

  it('skips locales with unsupported scripts and invalid tags', () => {
    expect(resolveKeycloakLocale(['invalid_locale', 'zh-Hant-TW', 'EN-us'])).toBe('en');
  });

  it('falls back to Spanish when none of the browser languages are supported', () => {
    expect(resolveKeycloakLocale(['fr-FR'])).toBe('es');
  });
});
