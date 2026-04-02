/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { resolveInitialLanguage, resolveSupportedLanguage } from './app.config';

describe('resolveInitialLanguage', () => {
  it('returns ca when browser language is Catalan', () => {
    expect(resolveInitialLanguage(['ca-ES'])).toBe('ca');
  });

  it('returns first supported language from browser preferences', () => {
    expect(resolveInitialLanguage(['fr-FR', 'es-ES', 'en-GB'])).toBe('es');
  });

  it('falls back to English when no supported languages are present', () => {
    expect(resolveInitialLanguage(['fr-FR', 'de-DE'])).toBe('en');
  });
});


describe('resolveSupportedLanguage', () => {
  it('normalizes locale tags to a supported language', () => {
    expect(resolveSupportedLanguage('es-ES')).toBe('es');
  });

  it('falls back when locale is not supported', () => {
    expect(resolveSupportedLanguage('fr-FR')).toBe('en');
  });
});
