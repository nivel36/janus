/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Subject, of } from 'rxjs';

export class MockTranslateService {
  readonly onLangChange = new Subject();
  readonly onTranslationChange = new Subject();
  readonly onDefaultLangChange = new Subject();
  readonly onFallbackLangChange = new Subject();

  getCurrentLang(): string {
    return 'es-ES';
  }

  getFallbackLang(): string {
    return 'en-EN';
  }

  instant(key: string, params?: Record<string, unknown>): string {
    if (key === 'autocomplete.manyResultsAvailable' && params?.['count'] !== undefined) {
      return `autocomplete.manyResultsAvailable:${params['count']}`;
    }
    return key;
  }

  get(key: string | string[], params?: Record<string, unknown>) {
    if (Array.isArray(key)) {
      const result: Record<string, string> = {};
      for (const k of key) {
        result[k] = this.instant(k, params);
      }
      return of(result);
    }
    return of(this.instant(key, params));
  }

  stream(key: string | string[], params?: Record<string, unknown>) {
    return this.get(key, params);
  }
}
