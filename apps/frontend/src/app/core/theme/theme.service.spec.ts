/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { DOCUMENT } from '@angular/common';
import { PLATFORM_ID } from '@angular/core';
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
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it('applies the persisted theme to the root element', () => {
    localStorage.setItem('janus.theme', 'dark');

    service.initialize();

    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(service.theme()).toBe('dark');
  });

  it('uses the system preference when there is no persisted theme', () => {
    vi.stubGlobal(
      'matchMedia',
      vi.fn(() => ({ matches: true }) as MediaQueryList),
    );

    service.initialize();

    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });

  it('falls back to the system preference when reading storage throws', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new DOMException('Storage is unavailable', 'SecurityError');
    });
    vi.stubGlobal(
      'matchMedia',
      vi.fn(() => ({ matches: true }) as MediaQueryList),
    );

    expect(() => service.initialize()).not.toThrow();
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });

  it('falls back when acquiring storage throws', () => {
    TestBed.resetTestingModule();
    const defaultView = {
      get localStorage(): Storage {
        throw new DOMException('Storage is unavailable', 'SecurityError');
      },
      matchMedia: () => ({ matches: true }) as MediaQueryList,
    } as unknown as Window;
    const fakeDocument = { documentElement: document.documentElement, defaultView } as Document;
    TestBed.configureTestingModule({
      providers: [
        { provide: DOCUMENT, useValue: fakeDocument },
        { provide: PLATFORM_ID, useValue: 'browser' },
      ],
    });
    service = TestBed.inject(ThemeService);

    expect(() => service.initialize()).not.toThrow();
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });

  it('applies and persists a selected theme', () => {
    service.setTheme('light');

    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    expect(localStorage.getItem('janus.theme')).toBe('light');
  });

  it('still applies a selected theme when writing storage throws', () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('Storage is unavailable', 'SecurityError');
    });

    expect(() => service.setTheme('dark')).not.toThrow();
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(service.theme()).toBe('dark');
  });
});
