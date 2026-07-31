/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { resolveInitialLanguage } from '../i18n/language.util';

/**
 * Resolves the browser preferences to a locale supported by the Keycloak realm.
 * Passing it explicitly as `ui_locales` prevents a stale Keycloak locale cookie
 * from overriding the language currently selected in the browser.
 */
export function resolveKeycloakLocale(
  browserLanguages: readonly string[] | undefined = globalThis.navigator?.languages,
): string {
  return resolveInitialLanguage(browserLanguages).split('-')[0];
}
