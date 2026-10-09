/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Duration } from './duration';

export interface TimeLog {
  employeeNumber: string;
  employeeName?: string;
  worksiteCode: string;
  worksiteName?: string;
  worksiteZoneId: string;
  entryTime: string;
  exitTime?: string | null;
  workTime: Duration | null;
}
