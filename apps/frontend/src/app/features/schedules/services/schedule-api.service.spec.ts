/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { SchedulesService } from '../../../client/api/schedules.service';
import { ScheduleApiService } from './schedule-api.service';

const schedule = (code: string) => ({
  code, name: code, entryTolerance: 'PT0S', exitTolerance: 'PT0S',
});

describe('ScheduleApiService.listAll', () => {
  const api = { searchSchedules: vi.fn() };
  let service: ScheduleApiService;

  beforeEach(() => {
    api.searchSchedules.mockReset();
    TestBed.configureTestingModule({
      providers: [{ provide: SchedulesService, useValue: api }],
    });
    service = TestBed.inject(ScheduleApiService);
  });

  it('loads every page without a search query, including beyond 50 schedules', async () => {
    const firstPage = Array.from({ length: 100 }, (_, i) => schedule(`S${i}`));
    api.searchSchedules
      .mockReturnValueOnce(of({ content: firstPage, page: { number: 0, totalPages: 2 } }))
      .mockReturnValueOnce(of({ content: [schedule('LAST')], page: { number: 1, totalPages: 2 } }));
    expect(await firstValueFrom(service.listAll())).toEqual([...firstPage, schedule('LAST')]);
    expect(api.searchSchedules).toHaveBeenCalledTimes(2);
    expect(api.searchSchedules.mock.calls.map((args) => args.slice(0, 5))).toEqual([
      [undefined, undefined, 0, 100, ['code,desc']],
      [undefined, undefined, 1, 100, ['code,desc']],
    ]);
  });

  it('returns an empty list when there are no schedules', async () => {
    api.searchSchedules.mockReturnValue(of({ content: [], page: { number: 0, totalPages: 0 } }));
    expect(await firstValueFrom(service.listAll())).toEqual([]);
    expect(api.searchSchedules).toHaveBeenCalledTimes(1);
  });

  it('fails the complete load if a later page fails and starts fresh on retry', async () => {
    const error = new Error('Network error');
    api.searchSchedules
      .mockReturnValueOnce(of({ content: [schedule('DAY')], page: { number: 0, totalPages: 2 } }))
      .mockReturnValueOnce(throwError(() => error));
    await expect(firstValueFrom(service.listAll())).rejects.toBe(error);
    api.searchSchedules.mockReturnValue(of({ content: [schedule('DAY')], page: { number: 0, totalPages: 1 } }));
    expect(await firstValueFrom(service.listAll())).toEqual([schedule('DAY')]);
    expect(api.searchSchedules.mock.calls[2][2]).toBe(0);
  });
});
