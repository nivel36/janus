/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    globals: true,
    environment: 'jsdom',
    isolate: false,
    include: ['src/**/*.{spec,test}.ts'],
    exclude: ['dist/**', 'node_modules/**'],
    setupFiles: ['src/test-setup.ts'],
  },
});
