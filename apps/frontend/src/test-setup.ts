/**
 * SPDX-License-Identifier: Apache-2.0
 */
import '@angular/compiler';
import { getTestBed } from '@angular/core/testing';
import { BrowserTestingModule, platformBrowserTesting } from '@angular/platform-browser/testing';

getTestBed().initTestEnvironment(BrowserTestingModule, platformBrowserTesting());

if (typeof PointerEvent === 'undefined') {
  // @ts-expect-error polyfill for jsdom
  globalThis.PointerEvent = MouseEvent;
}
