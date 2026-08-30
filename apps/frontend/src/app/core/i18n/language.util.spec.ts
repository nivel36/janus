import { describe, expect, it } from 'vitest';
import {
  FALLBACK_LANGUAGE,
  findSupportedLanguage,
  resolveInitialLanguage,
  resolveSupportedLanguage,
  supportedLanguages,
} from './language.util';

describe('language.util', () => {
  describe('supportedLanguages', () => {
    it('should expose the supported languages in the expected order', () => {
      expect(supportedLanguages).toEqual(['en-GB', 'es-ES', 'ca-ES']);
    });
  });

  describe('FALLBACK_LANGUAGE', () => {
    it('should define es-ES as the default fallback language', () => {
      expect(FALLBACK_LANGUAGE).toBe('es-ES');
    });
  });

  describe('findSupportedLanguage', () => {
    it('should return an exact supported locale match', () => {
      expect(findSupportedLanguage('es-ES')).toBe('es-ES');
    });

    it('should match locales case-insensitively', () => {
      expect(findSupportedLanguage('ES-es')).toBe('es-ES');
      expect(findSupportedLanguage('EN-gb')).toBe('en-GB');
    });

    it('should resolve by language when the region is different', () => {
      expect(findSupportedLanguage('es-MX')).toBe('es-ES');
    });

    it('should resolve a language-only locale', () => {
      expect(findSupportedLanguage('ca')).toBe('ca-ES');
    });

    it('should resolve locales containing Unicode extensions by language', () => {
      expect(findSupportedLanguage('ca-ES-u-ca-gregory')).toBe('ca-ES');
    });

    it('should return undefined for an unsupported locale containing a script', () => {
      expect(findSupportedLanguage('zh-Hant-TW')).toBeUndefined();
    });

    it('should return undefined for invalid locale tags', () => {
      expect(findSupportedLanguage('es--ES')).toBeUndefined();
      expect(findSupportedLanguage('not_a_locale')).toBeUndefined();
    });

    it('should return undefined for an unsupported locale', () => {
      expect(findSupportedLanguage('fr-FR')).toBeUndefined();
    });

    it('should return undefined when locale is undefined', () => {
      expect(findSupportedLanguage(undefined)).toBeUndefined();
    });

    it('should return undefined when locale is null', () => {
      expect(findSupportedLanguage(null)).toBeUndefined();
    });

    it('should return undefined when locale is empty', () => {
      expect(findSupportedLanguage('')).toBeUndefined();
    });
  });

  describe('resolveSupportedLanguage', () => {
    it('should return the resolved supported language when a match exists', () => {
      expect(resolveSupportedLanguage('ca-ES')).toBe('ca-ES');
    });

    it('should resolve by base language when possible', () => {
      expect(resolveSupportedLanguage('en-US')).toBe('en-GB');
    });

    it('should return the default fallback language when locale is unsupported', () => {
      expect(resolveSupportedLanguage('fr-FR')).toBe('es-ES');
    });

    it('should return the provided custom fallback language when locale is unsupported', () => {
      expect(resolveSupportedLanguage('fr-FR', 'es-ES')).toBe('es-ES');
    });
  });

  describe('resolveInitialLanguage', () => {
    it('should return the first supported browser language', () => {
      expect(resolveInitialLanguage(['fr-FR', 'es-ES', 'ca-ES'])).toBe('es-ES');
    });

    it('should skip invalid and unsupported locales before negotiating a supported locale', () => {
      expect(resolveInitialLanguage(['invalid_locale', 'zh-Hant-TW', 'es-MX'])).toBe('es-ES');
    });

    it('should resolve the first browser language that matches by base language', () => {
      expect(resolveInitialLanguage(['de-DE', 'ca-AD'])).toBe('ca-ES');
    });

    it('should return the fallback language when no browser languages are supported', () => {
      expect(resolveInitialLanguage(['fr-FR', 'de-DE'])).toBe('es-ES');
    });

    it('should return the provided custom fallback when no browser languages are supported', () => {
      expect(resolveInitialLanguage(['fr-FR', 'de-DE'], 'ca-ES')).toBe('ca-ES');
    });

    it('should return the fallback language when browserLanguages is undefined', () => {
      expect(resolveInitialLanguage(undefined)).toBe('es-ES');
    });

    it('should ignore empty browser language arrays and return the fallback', () => {
      expect(resolveInitialLanguage([])).toBe('es-ES');
    });
  });
});
