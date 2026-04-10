/**
 * SPDX-License-Identifier: Apache-2.0
 */
export const supportedLanguages = ['en-EN', 'es-ES', 'ca-ES'] as const;

export type SupportedLanguage = (typeof supportedLanguages)[number];

export const FALLBACK_LANGUAGE: SupportedLanguage = 'en-EN';

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

export function resolveSupportedLanguage(
  locale: string | undefined | null,
  fallbackLanguage: SupportedLanguage = FALLBACK_LANGUAGE,
): SupportedLanguage {
  return findSupportedLanguage(locale) ?? fallbackLanguage;
}

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
