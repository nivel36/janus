export type WorksiteScope = 'GLOBAL' | 'PERSONAL';

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
  ownerEmployeeEmail: string | null;
}

export interface UpdateWorksitePayload {
  name: string;
  timeZone: string;
  scope: WorksiteScope;
  ownerEmployeeEmail: string | null;
}
