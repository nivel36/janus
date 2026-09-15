export type TimeFormat = 'H12' | 'H24';

export interface UserPreferences {
  locale: string;
  timeFormat: TimeFormat;
  defaultTimezone: string;
}
