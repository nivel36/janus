/**
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * List of application locales officially supported by the system.
 *
 * The values are expressed as language-region tags.
 */
export const supportedLanguages = ['en-EN', 'es-ES', 'ca-ES'] as const;

/**
 * Union type containing every supported language tag.
 */
export type SupportedLanguage = (typeof supportedLanguages)[number];

/**
 * Default language used when no supported locale can be resolved.
 */
export const FALLBACK_LANGUAGE: SupportedLanguage = 'en-EN';

/**
 * Resolves a locale to one of the supported languages.
 *
 * Resolution is performed in two steps:
 * 1. Try an exact match against the full locale, ignoring case.
 * 2. If no exact match is found, try matching only the language part
 *    (for example, {@code es} resolves to {@code es-ES}).
 *
 * Examples:
 * - {@code en-EN} -> {@code en-EN}
 * - {@code EN-en} -> {@code en-EN}
 * - {@code es-MX} -> {@code es-ES}
 * - {@code fr-FR} -> {@code undefined}
 *
 * @param locale Locale to resolve. It may be {@code undefined} or {@code null}.
 * @returns The matching supported language, or {@code undefined} if none matches.
 */
export function findSupportedLanguage(
  locale: string | undefined | null,
): SupportedLanguage | undefined {
  const normalizedLocale = locale?.toLowerCase();

  const exactMatch = supportedLanguages.find(
    (supportedLanguage) => supportedLanguage.toLowerCase() === normalizedLocale,
  );

  if (exactMatch) {
    return exactMatch;
  }

  const languageOnly = normalizedLocale?.split('-')[0];

  return supportedLanguages.find((supportedLanguage) =>
    supportedLanguage.toLowerCase().startsWith(`${languageOnly}-`),
  );
}

/**
 * Resolves a locale to a supported language and guarantees a valid result.
 *
 * If the provided locale cannot be mapped to any supported language,
 * the given fallback language is returned instead.
 *
 * @param locale Locale to resolve. It may be {@code undefined} or {@code null}.
 * @param fallbackLanguage Language to use when resolution fails.
 * @returns A valid supported language.
 */
export function resolveSupportedLanguage(
  locale: string | undefined | null,
  fallbackLanguage: SupportedLanguage = FALLBACK_LANGUAGE,
): SupportedLanguage {
  return findSupportedLanguage(locale) ?? fallbackLanguage;
}

/**
 * Resolves the initial application language from the browser language list.
 *
 * The function checks the browser languages in order and returns the first
 * locale that can be mapped to a supported language. If none matches,
 * the fallback language is returned.
 *
 * This is intended to work with values such as {@code navigator.languages}.
 *
 * @param browserLanguages Ordered list of browser-preferred locales.
 * @param fallbackLanguage Language to use when no browser locale is supported.
 * @returns A valid supported language.
 */
export function resolveInitialLanguage(
  browserLanguages: readonly string[] | undefined,
  fallbackLanguage: SupportedLanguage = FALLBACK_LANGUAGE,
): SupportedLanguage {
  for (const browserLanguage of browserLanguages ?? []) {
    const resolvedLanguage = findSupportedLanguage(browserLanguage);

    if (resolvedLanguage) {
      return resolvedLanguage;
    }
  }

  return fallbackLanguage;
}
