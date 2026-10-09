/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { TimeLogsService } from '../../../client/api/timeLogs.service';
import { TimeLogResponse } from '../../../client/model/timeLogResponse';
import { TimeLogService } from './timelog-api.service';

describe('TimeLogService', () => {
  it('uses nested employee and worksite details for searches and clock actions', async () => {
    const response: TimeLogResponse = {
      employee: { number: 'EMP-0042', fullName: 'Abel Ferrer' },
      worksite: { code: 'MAD-HQ', name: 'Madrid Headquarters', zoneId: 'Europe/Madrid' },
      entryTime: '2026-10-01T08:00:00Z',
      exitTime: null,
      workTime: null,
    };
    const api = {
      searchTimeLogs: vi.fn(() => of({
        content: [response],
        page: { totalElements: 1, number: 0, size: 10, totalPages: 1 },
      })),
      clockIn: vi.fn(() => of(response)),
      clockOut: vi.fn(() => of(response)),
    };
    TestBed.configureTestingModule({ providers: [{ provide: TimeLogsService, useValue: api }] });
    const service = TestBed.inject(TimeLogService);
    const expected = {
      employeeNumber: 'EMP-0042',
      employeeName: 'Abel Ferrer',
      worksiteCode: 'MAD-HQ',
      worksiteName: 'Madrid Headquarters',
      worksiteZoneId: 'Europe/Madrid',
      entryTime: response.entryTime,
      exitTime: null,
      workTime: null,
    };

    expect((await firstValueFrom(service.search())).items).toEqual([expected]);
    expect(api.searchTimeLogs).toHaveBeenLastCalledWith(undefined, undefined, undefined, 0, 10, ['entryTime,desc'], 'body', false, expect.any(Object));
    const range = { start: '2026-10-01T00:00:00Z', end: '2026-10-03T00:00:00Z' };
    await firstValueFrom(service.search(1, 5, range));
    expect(api.searchTimeLogs).toHaveBeenLastCalledWith(undefined, range.start, range.end, 1, 5, ['entryTime,desc'], 'body', false, expect.any(Object));
    expect(await firstValueFrom(service.searchLatestByEmployee('EMP-0042'))).toEqual(expected);
    expect(await firstValueFrom(service.clockIn('EMP-0042', 'MAD-HQ'))).toEqual(expected);
    expect(await firstValueFrom(service.clockOut('EMP-0042', 'MAD-HQ'))).toEqual(expected);
    expect(api.clockIn).toHaveBeenCalledWith('EMP-0042', 'MAD-HQ');
    expect(api.clockOut).toHaveBeenCalledWith('EMP-0042', 'MAD-HQ');
  });
});
