/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { DOCUMENT } from '@angular/common';
import { Component, DestroyRef, OnInit, effect, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterOutlet } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

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
})
export class AppComponent implements OnInit {
  private readonly currentUserFacade = inject(CurrentUserFacade);
  private readonly translateService = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly document = inject(DOCUMENT);

  constructor() {
    effect(() => {
      const locale = this.currentUserFacade.preferences()?.locale;
      if (locale) {
        this.translateService.use(this.resolveLanguage(locale));
      }
    });
  }

  ngOnInit(): void {
    this.bindDocumentLanguage();
  }

  private bindDocumentLanguage(): void {
    this.updateDocumentLanguage(
      findSupportedLanguage(this.translateService.getCurrentLang()) ?? FALLBACK_LANGUAGE,
    );

    this.translateService.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(({ lang }) => this.updateDocumentLanguage(resolveSupportedLanguage(lang)));
  }

  private updateDocumentLanguage(language: (typeof supportedLanguages)[number]): void {
    this.document.documentElement.lang = language;
  }

  private resolveLanguage(locale: string): (typeof supportedLanguages)[number] {
    const currentLanguage =
      findSupportedLanguage(this.translateService.getCurrentLang()) ?? FALLBACK_LANGUAGE;
    return resolveSupportedLanguage(locale, currentLanguage);
  }
}
