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

  it('preserves an absolute login URL from the document origin', () => {
    const service = serviceFor({
      defaultView: { location: new URL('https://janus.example/current') },
    });

    expect(service.loginRedirectUri('https://janus.example/complete')).toBe(
      'https://janus.example/complete',
    );
  });

  it('uses the current URL instead of an HTTPS login URL from another origin', () => {
    const service = serviceFor({
      defaultView: { location: new URL('https://janus.example/current') },
    });

    expect(service.loginRedirectUri('https://other.example/complete')).toBe(
      'https://janus.example/current',
    );
  });

  it('uses the current URL instead of a login URL with a disallowed protocol', () => {
    const service = serviceFor({
      defaultView: { location: new URL('https://janus.example/current') },
    });

    expect(service.loginRedirectUri('javascript:alert(1)')).toBe(
      'https://janus.example/current',
    );
  });

  it('uses the current URL when no return route is provided', () => {
    const service = serviceFor({
      defaultView: { location: new URL('https://janus.example/current?tab=details') },
    });

    expect(service.loginRedirectUri()).toBe('https://janus.example/current?tab=details');
  });

  it('returns undefined when the document has no default view', () => {
    const service = serviceFor({ defaultView: null });

    expect(service.loginRedirectUri('/employees')).toBeUndefined();
    expect(service.loginRedirectUri()).toBeUndefined();
  });
});
