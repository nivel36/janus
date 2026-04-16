/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { beforeEach, describe, expect, it } from 'vitest';

import { DurationPipe } from './duration.pipe';
import { Duration } from '../../features/timelogs/models/duration';

describe('DurationPipe', () => {
  let pipe: DurationPipe;

  beforeEach(() => {
    pipe = new DurationPipe();
  });

  it('should create', () => {
    expect(pipe).toBeTruthy();
  });

  it('should return an empty string when duration is null', () => {
    expect(pipe.transform(null)).toBe('');
  });

  it('should return an empty string when duration is undefined', () => {
    expect(pipe.transform(undefined)).toBe('');
  });

  it('should format hours, minutes, and seconds when seconds are greater than 0', () => {
    const duration: Duration = {
      hours: 2,
      minutes: 15,
      seconds: 30,
      iso8601: 'PT2H15M30S',
    };

    expect(pipe.transform(duration)).toBe('2h 15m 30s');
  });

  it('should format hours and minutes when seconds are 0 and minutes are greater than 0', () => {
    const duration: Duration = {
      hours: 1,
      minutes: 45,
      seconds: 0,
      iso8601: 'PT1H45M',
    };

    expect(pipe.transform(duration)).toBe('1h 45m');
  });

  it('should format only hours when minutes and seconds are 0', () => {
    const duration: Duration = {
      hours: 3,
      minutes: 0,
      seconds: 0,
      iso8601: 'PT3H',
    };

    expect(pipe.transform(duration)).toBe('3h');
  });

  it('should include minutes when seconds are present even if minutes are 0', () => {
    const duration: Duration = {
      hours: 1,
      minutes: 0,
      seconds: 10,
      iso8601: 'PT1H10S',
    };

    expect(pipe.transform(duration)).toBe('1h 0m 10s');
  });

  it('should include hours even when hours are 0', () => {
    const duration: Duration = {
      hours: 0,
      minutes: 5,
      seconds: 0,
      iso8601: 'PT5M',
    };

    expect(pipe.transform(duration)).toBe('0h 5m');
  });

  it('should ignore the iso8601 field when formatting the output', () => {
    const duration: Duration = {
      hours: 2,
      minutes: 15,
      seconds: 30,
      iso8601: 'INVALID_VALUE',
    };

    expect(pipe.transform(duration)).toBe('2h 15m 30s');
  });

  it('should produce the same output regardless of the iso8601 value', () => {
    const durationA: Duration = {
      hours: 4,
      minutes: 20,
      seconds: 0,
      iso8601: 'PT4H20M',
    };

    const durationB: Duration = {
      hours: 4,
      minutes: 20,
      seconds: 0,
      iso8601: 'SOMETHING_ELSE',
    };

    expect(pipe.transform(durationA)).toBe('4h 20m');
    expect(pipe.transform(durationB)).toBe('4h 20m');
  });
});
