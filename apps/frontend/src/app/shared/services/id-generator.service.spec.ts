/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, expect, it } from 'vitest';

import { DeterministicIdGenerator } from './id-generator.service';

describe('DeterministicIdGenerator', () => {
  it('produces predictable, collision-free identifiers for each instance lifetime', () => {
    const generator = new DeterministicIdGenerator();

    expect(generator.generate('control')).toBe('control-0');
    expect(generator.generate('control')).toBe('control-1');
    expect(generator.generate('panel')).toBe('panel-2');
  });

  it('starts each rendering context from the same deterministic value', () => {
    expect(new DeterministicIdGenerator().generate('control')).toBe('control-0');
    expect(new DeterministicIdGenerator().generate('control')).toBe('control-0');
  });
});
