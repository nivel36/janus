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

    if (isPlatformBrowser(this.platformId)) {
      this.document.defaultView?.localStorage.setItem(THEME_STORAGE_KEY, theme);
    }
  }

  private applyTheme(theme: Theme): void {
    this.theme.set(theme);
    this.document.documentElement.setAttribute('data-theme', theme);
  }

  private readPersistedTheme(): Theme | null {
    if (!isPlatformBrowser(this.platformId)) {
      return null;
    }

    const storedTheme = this.document.defaultView?.localStorage.getItem(THEME_STORAGE_KEY);
    return storedTheme === 'light' || storedTheme === 'dark' ? storedTheme : null;
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
