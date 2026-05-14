/**
 * SPDX-License-Identifier: Apache-2.0
 */
export type WorksiteScope = 'GLOBAL' | 'ASSIGNED';

export interface Worksite {
  code: string;
  name: string;
  timeZone: string;
  scope: WorksiteScope;
  description: string | null;
  address: string | null;
  ownerEmployeeEmail: string | null;
  active: boolean;
}

export interface CreateWorksitePayload {
  code: string;
  name: string;
  timeZone: string;
  scope: WorksiteScope;
  description: string | null;
  address: string | null;
}

export interface UpdateWorksitePayload {
  name: string;
  timeZone: string;
  scope: WorksiteScope;
  description: string | null;
  address: string | null;
  ownerEmployeeEmail: string | null;
}
