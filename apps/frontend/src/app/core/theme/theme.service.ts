/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { inject, Injectable, PLATFORM_ID, signal } from '@angular/core';

export type Theme = 'light' | 'dark';

const THEME_STORAGE_KEY = 'janus.theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly platformId = inject(PLATFORM_ID);

  readonly theme = signal<Theme>('light');

  initialize(): void {
    const theme = this.readPersistedTheme() ?? this.preferredSystemTheme();
    this.applyTheme(theme);
  }

  setTheme(theme: Theme): void {
    this.applyTheme(theme);
    this.persistTheme(theme);
  }

  private applyTheme(theme: Theme): void {
    this.theme.set(theme);
    this.document.documentElement.setAttribute('data-theme', theme);
  }

  private readPersistedTheme(): Theme | null {
    const storage = this.browserStorage();
    if (!storage) {
      return null;
    }

    try {
      const storedTheme = storage.getItem(THEME_STORAGE_KEY);
      return storedTheme === 'light' || storedTheme === 'dark' ? storedTheme : null;
    } catch {
      return null;
    }
  }

  private persistTheme(theme: Theme): void {
    try {
      this.browserStorage()?.setItem(THEME_STORAGE_KEY, theme);
    } catch {
      // Applying the theme must not depend on Web Storage being writable.
    }
  }

  private browserStorage(): Storage | null {
    if (!isPlatformBrowser(this.platformId)) {
      return null;
    }

    try {
      return this.document.defaultView?.localStorage ?? null;
    } catch {
      return null;
    }
  }

  private preferredSystemTheme(): Theme {
    if (!isPlatformBrowser(this.platformId)) {
      return 'light';
    }

    return this.document.defaultView?.matchMedia('(prefers-color-scheme: dark)').matches
      ? 'dark'
      : 'light';
  }
}
