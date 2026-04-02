/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterOutlet } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { distinctUntilChanged, filter, map } from 'rxjs/operators';

import { resolveSupportedLanguage } from './app.config';
import { CurrentUserFacade } from './core/user/services/current-user.facade';

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
    this.currentUserFacade.preferences$
      .pipe(
        map((preferences) => preferences?.locale),
        filter((locale): locale is string => !!locale),
        map((locale) =>
          resolveSupportedLanguage(locale, resolveSupportedLanguage(this.translateService.currentLang)),
        ),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((language) => {
        this.translateService.use(language);
      });
  }
}
