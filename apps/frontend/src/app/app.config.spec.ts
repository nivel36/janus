/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { resolveInitialLanguage, resolveSupportedLanguage } from './core/i18n/language.util';
import { apiBearerUrlPattern, keycloakInitOptions } from './app.config';
import { resolveKeycloakLocale } from './core/auth/keycloak-locale';

describe('resolveInitialLanguage', () => {
  it('returns ca when browser language is Catalan', () => {
    expect(resolveInitialLanguage(['ca-ES'])).toBe('ca-ES');
  });

  it('returns first supported language from browser preferences', () => {
    expect(resolveInitialLanguage(['fr-FR', 'es-ES', 'en-GB'])).toBe('es-ES');
  });

  it('falls back to English when no supported languages are present', () => {
    expect(resolveInitialLanguage(['fr-FR', 'de-DE'])).toBe('es-ES');
  });
});

describe('resolveSupportedLanguage', () => {
  it('normalizes locale tags to a supported language', () => {
    expect(resolveSupportedLanguage('es-ES')).toBe('es-ES');
  });

  it('maps language-only tags to a supported locale', () => {
    expect(resolveSupportedLanguage('es')).toBe('es-ES');
  });

  it('falls back when locale is not supported', () => {
    expect(resolveSupportedLanguage('fr-FR', 'en-EN')).toBe('en-EN');
  });
});

describe('Keycloak application configuration', () => {
  it('initializes check-sso with PKCE, silent SSO and the realm locale', () => {
    expect(keycloakInitOptions).toMatchObject({
      onLoad: 'check-sso',
      pkceMethod: 'S256',
      checkLoginIframe: false,
      locale: resolveKeycloakLocale(),
    });
    expect(keycloakInitOptions.silentCheckSsoRedirectUri).toBe(
      `${window.location.origin}/assets/silent-check-sso.html`,
    );
  });

  it('limits bearer URLs to the configured API path', () => {
    const pattern = apiBearerUrlPattern('/api', 'https://janus.example');

    expect(pattern.test('/api/worksites')).toBe(true);
    expect(pattern.test('https://janus.example/api/worksites')).toBe(true);
    expect(pattern.test('/api-v2/worksites')).toBe(false);
    expect(pattern.test('https://third-party.example/api/worksites')).toBe(false);
  });
});
