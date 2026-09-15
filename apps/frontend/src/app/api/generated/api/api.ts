export * from './schedules.service';
import { SchedulesService } from './schedules.service';
export * from './timeLogs.service';
import { TimeLogsService } from './timeLogs.service';
export * from './worksites.service';
import { WorksitesService } from './worksites.service';
export const APIS = [SchedulesService, TimeLogsService, WorksitesService];
