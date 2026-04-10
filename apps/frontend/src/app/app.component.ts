/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterOutlet } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { distinctUntilChanged, filter, map } from 'rxjs/operators';

import { CurrentUserFacade } from './core/user/services/current-user.facade';
import {
  FALLBACK_LANGUAGE,
  findSupportedLanguage,
  resolveSupportedLanguage,
  supportedLanguages,
} from './core/i18n/language.util';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent implements OnInit {
  private readonly currentUserFacade = inject(CurrentUserFacade);
  private readonly translateService = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.bindUserLanguage();
  }

  private bindUserLanguage(): void {
    this.currentUserFacade.preferences$
      .pipe(
        map((preferences) => preferences?.locale),
        filter(this.hasLocale),
        map((locale) => this.resolveLanguage(locale)),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((language) => {
        this.translateService.use(language);
      });
  }

  /**
   * Checks whether the provided locale is a non-empty string.
   *
   * @param locale Locale value extracted from user preferences.
   * @returns {@code true} when the locale is a valid non-empty string.
   */
  private hasLocale(locale: string | null | undefined): locale is string {
    return !!locale;
  }

  private resolveLanguage(locale: string): (typeof supportedLanguages)[number] {
    const currentLanguage =
      findSupportedLanguage(this.translateService.getCurrentLang()) ?? FALLBACK_LANGUAGE;
    return resolveSupportedLanguage(locale, currentLanguage);
  }
}
