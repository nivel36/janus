/** SPDX-License-Identifier: Apache-2.0 */
export interface TimelogSearchRange {
  start: string;
  end: string;
}

export type TimelogSearchFilters = (
  | TimelogSearchRange
  | { start?: never; end?: never }
) & { employeeNumber?: string };
