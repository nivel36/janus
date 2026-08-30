/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { DOCUMENT } from '@angular/common';
import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { resolveInitialLanguage, resolveSupportedLanguage } from './core/i18n/language.util';
import {
  APP_ORIGIN,
  apiBearerUrlPattern,
  INITIAL_LANGUAGE,
  KEYCLOAK_INIT_OPTIONS,
} from './app.config';
import { resolveKeycloakLocale } from './core/auth/keycloak-locale';

describe('resolveInitialLanguage', () => {
  it('returns ca when browser language is Catalan', () => {
    expect(resolveInitialLanguage(['ca-ES'])).toBe('ca-ES');
  });

  it('returns first supported language from browser preferences', () => {
    expect(resolveInitialLanguage(['fr-FR', 'es-ES', 'en-GB'])).toBe('es-ES');
  });

  it('falls back to Spanish when no supported languages are present', () => {
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
    expect(resolveSupportedLanguage('fr-FR', 'en-GB')).toBe('en-GB');
  });
});

describe('Keycloak application configuration', () => {
  const fakeDocument = (href: string, languages = ['ca-ES']) =>
    ({
      defaultView: {
        location: new URL(href),
        navigator: { language: languages[0], languages },
      },
    }) as unknown as Document;

  it('initializes check-sso with PKCE, silent SSO and the realm locale', () => {
    TestBed.configureTestingModule({
      providers: [
        { provide: DOCUMENT, useValue: fakeDocument('https://janus.example/worksites') },
        { provide: PLATFORM_ID, useValue: 'browser' },
      ],
    });
    const options = TestBed.inject(KEYCLOAK_INIT_OPTIONS);

    expect(options).toMatchObject({
      onLoad: 'check-sso',
      pkceMethod: 'S256',
      checkLoginIframe: false,
      locale: resolveKeycloakLocale(['ca-ES']),
    });
    expect(options.silentCheckSsoRedirectUri).toBe(
      'https://janus.example/assets/silent-check-sso.html',
    );
  });

  it('has explicit server defaults when the document has no default view', () => {
    TestBed.configureTestingModule({
      providers: [
        { provide: DOCUMENT, useValue: { defaultView: null } },
        { provide: PLATFORM_ID, useValue: 'server' },
      ],
    });

    expect(TestBed.inject(APP_ORIGIN)).toBeUndefined();
    expect(TestBed.inject(INITIAL_LANGUAGE)).toBe('es-ES');
    expect(TestBed.inject(KEYCLOAK_INIT_OPTIONS).silentCheckSsoRedirectUri).toBeUndefined();
  });

  it('limits bearer URLs to the configured API path', () => {
    const pattern = apiBearerUrlPattern('/api', 'https://janus.example');

    expect(pattern.test('/api/worksites')).toBe(true);
    expect(pattern.test('https://janus.example/api/worksites')).toBe(true);
    expect(pattern.test('/api-v2/worksites')).toBe(false);
    expect(pattern.test('https://third-party.example/api/worksites')).toBe(false);
  });
});
