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

    if (!redirectUri) {
      return location.href;
    }

    const resolved = new URL(redirectUri, location.origin);
    const isHttp = resolved.protocol === 'http:' || resolved.protocol === 'https:';

    return isHttp && resolved.origin === location.origin ? resolved.toString() : location.href;
  }

  logoutRedirectUri(): string | undefined {
    return this.document.defaultView?.location.origin;
  }
}
