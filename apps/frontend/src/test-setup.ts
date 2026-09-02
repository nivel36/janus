/**
 * SPDX-License-Identifier: Apache-2.0
 */
/// <reference types="vite/client" />
import 'zone.js';
import 'zone.js/testing';
import '@angular/compiler';
import { ɵresolveComponentResources as resolveComponentResources } from '@angular/core';
import { getTestBed } from '@angular/core/testing';
import { BrowserTestingModule, platformBrowserTesting } from '@angular/platform-browser/testing';
import { beforeEach } from 'vitest';

const componentResources = import.meta.glob('./app/**/*.{html,css}', {
  query: '?raw',
  import: 'default',
  eager: true,
}) as Record<string, string>;

getTestBed().initTestEnvironment(BrowserTestingModule, platformBrowserTesting());

beforeEach(async () => {
  await resolveComponentResources(async (url) => {
    const resourceName = url.replace(/^\.\//, '');
    const resource = Object.entries(componentResources).find(([path]) =>
      path.endsWith(`/${resourceName}`),
    )?.[1];

    if (resource !== undefined) {
      return resource;
    }

    const response = await fetch(url);
    return response.text();
  });
});

if (typeof PointerEvent === 'undefined') {
  // @ts-expect-error polyfill for jsdom
  globalThis.PointerEvent = MouseEvent;
}
