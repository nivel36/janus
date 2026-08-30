/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';

import { AuthRedirectService } from './auth-redirect.service';

describe('AuthRedirectService', () => {
  function serviceFor(document: unknown): AuthRedirectService {
    TestBed.configureTestingModule({
      providers: [AuthRedirectService, { provide: DOCUMENT, useValue: document }],
    });
    return TestBed.inject(AuthRedirectService);
  }

  it('resolves a relative login URL against the document origin', () => {
    const service = serviceFor({
      defaultView: { location: new URL('https://janus.example/current') },
    });

    expect(service.loginRedirectUri('/employees?active=true')).toBe(
      'https://janus.example/employees?active=true',
    );
  });

  it('preserves an absolute login URL', () => {
    const service = serviceFor({
      defaultView: { location: new URL('https://janus.example/current') },
    });

    expect(service.loginRedirectUri('https://other.example/complete')).toBe(
      'https://other.example/complete',
    );
  });

  it('returns undefined when the document has no default view', () => {
    const service = serviceFor({ defaultView: null });

    expect(service.loginRedirectUri('/employees')).toBeUndefined();
  });
});
