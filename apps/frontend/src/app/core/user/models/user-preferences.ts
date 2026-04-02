import { TimeFormat } from '../services/user-profile-api.service';

export interface UserPreferences {
  locale: string;
  timeFormat: TimeFormat;
  defaultTimezone: string;
}
