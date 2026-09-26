/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { isPlatformBrowser } from '@angular/common';
import { inject, InjectionToken, PLATFORM_ID } from '@angular/core';

/** Generates identifiers that only need to be unique in the current document. */
export interface IdGenerator {
  generate(prefix: string): string;
}

/**
 * Predictable generator for tests and non-browser rendering.
 *
 * A fresh instance starts at zero, making server-rendered output reproducible.
 */
export class DeterministicIdGenerator implements IdGenerator {
  private nextId = 0;

  generate(prefix: string): string {
    return `${prefix}-${this.nextId++}`;
  }
}

/**
 * Browser generator whose monotonic sequence matches server-rendered output.
 *
 * Existing DOM ids are intentionally not treated as collisions: during hydration
 * they belong to the component currently being reconstructed and must be reused.
 */
export class BrowserIdGenerator extends DeterministicIdGenerator {}

/** Application-wide source of document-scoped identifiers. */
export const ID_GENERATOR = new InjectionToken<IdGenerator>('ID_GENERATOR', {
  providedIn: 'root',
  factory: () => {
    const platformId = inject(PLATFORM_ID);
    return isPlatformBrowser(platformId)
      ? new BrowserIdGenerator()
      : new DeterministicIdGenerator();
  },
});
