/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { DOCUMENT } from '@angular/common';
import { inject, Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AuthRedirectService {
  private readonly document = inject(DOCUMENT);

  loginRedirectUri(redirectUri?: string): string | undefined {
    const location = this.document.defaultView?.location;
    if (!location) {
      return undefined;
    }

    return redirectUri ? new URL(redirectUri, location.origin).toString() : location.href;
  }

  logoutRedirectUri(): string | undefined {
    return this.document.defaultView?.location.origin;
  }
}
