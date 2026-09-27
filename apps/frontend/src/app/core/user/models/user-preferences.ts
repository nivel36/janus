export type Theme = 'DARK' | 'LIGHT';

export type TimeFormat = 'H12' | 'H24';

export interface UserPreferences {
  theme: Theme;
  locale: string;
  timeFormat: TimeFormat;
  defaultTimezone: string;
}
