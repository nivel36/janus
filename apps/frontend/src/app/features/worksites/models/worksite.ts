/**
 * SPDX-License-Identifier: Apache-2.0
 */
export type WorksiteScope = 'GLOBAL' | 'ASSIGNED';

export interface Worksite {
  code: string;
  name: string;
  timeZone: string;
  scope: WorksiteScope;
  ownerEmployeeEmail: string | null;
}

export interface CreateWorksitePayload {
  code: string;
  name: string;
  timeZone: string;
  scope: WorksiteScope;
}

export interface UpdateWorksitePayload {
  name: string;
  timeZone: string;
  scope: WorksiteScope;
  ownerEmployeeEmail: string | null;
}
