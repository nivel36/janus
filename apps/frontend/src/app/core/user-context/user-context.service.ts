/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Injectable } from '@angular/core';
import { combineLatest, filter, switchMap } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { PreferencesService } from '../../features/preferences/preferences.service';

@Injectable({ providedIn: 'root' })
export class UserContextService {
  constructor(
    private readonly auth: AuthService,
    private readonly prefs: PreferencesService,
  ) {
    this.auth.isAuthenticated$
      .pipe(
        filter(Boolean),
        switchMap(() => this.prefs.load(false)),
      )
      .subscribe({
        error: () => {
          /* decide si ignoras */
        },
      });

    // cuando pasa a no autenticado, limpia
    this.auth.isAuthenticated$.pipe(filter((v) => !v)).subscribe(() => this.prefs.clear());
  }
}
