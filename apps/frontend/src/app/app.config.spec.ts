/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { resolveInitialLanguage, resolveSupportedLanguage } from './core/i18n/language.util';

describe('resolveInitialLanguage', () => {
  it('returns ca when browser language is Catalan', () => {
    expect(resolveInitialLanguage(['ca-ES'])).toBe('ca-ES');
  });

  it('returns first supported language from browser preferences', () => {
    expect(resolveInitialLanguage(['fr-FR', 'es-ES', 'en-GB'])).toBe('es-ES');
  });

  it('falls back to English when no supported languages are present', () => {
    expect(resolveInitialLanguage(['fr-FR', 'de-DE'])).toBe('en-EN');
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
