/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { resolveInitialLanguage } from './app.config';

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
