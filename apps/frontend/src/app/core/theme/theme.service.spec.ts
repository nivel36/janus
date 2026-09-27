/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { TestBed } from '@angular/core/testing';

import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  let service: ThemeService;

  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    TestBed.configureTestingModule({});
    service = TestBed.inject(ThemeService);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('applies the persisted theme to the root element', () => {
    localStorage.setItem('janus.theme', 'dark');

    service.initialize();

    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(service.theme()).toBe('dark');
  });

  it('uses the system preference when there is no persisted theme', () => {
    vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: true }) as MediaQueryList));

    service.initialize();

    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });

  it('applies and persists a selected theme', () => {
    service.setTheme('light');

    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    expect(localStorage.getItem('janus.theme')).toBe('light');
  });
});
