/**
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * List of application locales officially supported by the system.
 *
 * The values are expressed as language-region tags.
 */
export const supportedLanguages = ['en-GB', 'es-ES', 'ca-ES'] as const;

/**
 * Union type containing every supported language tag.
 */
export type SupportedLanguage = (typeof supportedLanguages)[number];

/**
 * Default language used when no supported locale can be resolved.
 */
export const FALLBACK_LANGUAGE: SupportedLanguage = 'es-ES';

/**
 * Resolves a locale to one of the supported languages.
 *
 * Resolution is performed in two steps:
 * 1. Canonicalize the locale and try an exact match against the full locale.
 * 2. If no exact match is found, try matching only the language part
 *    (for example, {@code es} resolves to {@code es-ES}).
 *
 * Examples:
 * - {@code en-GB} -> {@code en-GB}
 * - {@code EN-gb} -> {@code en-GB}
 * - {@code es-MX} -> {@code es-ES}
 * - {@code fr-FR} -> {@code undefined}
 *
 * @param locale Locale to resolve. It may be {@code undefined} or {@code null}.
 * @returns The matching supported language, or {@code undefined} if none matches.
 */
export function findSupportedLanguage(
  locale: string | undefined | null,
): SupportedLanguage | undefined {
  if (!locale) {
    return undefined;
  }

  let canonicalLocale: string;

  try {
    [canonicalLocale] = Intl.getCanonicalLocales(locale);
  } catch (error) {
    if (error instanceof RangeError) {
      return undefined;
    }

    throw error;
  }

  const exactMatch = supportedLanguages.find(
    (supportedLanguage) => supportedLanguage === canonicalLocale,
  );

  if (exactMatch) {
    return exactMatch;
  }

  const requestedLanguage = new Intl.Locale(canonicalLocale).language;

  return supportedLanguages.find(
    (supportedLanguage) => new Intl.Locale(supportedLanguage).language === requestedLanguage,
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
