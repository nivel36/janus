import { UserPreferences } from './user-preferences';

export interface User {
  username: string | null;
  email: string | null;
  fullName: string;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isUser: boolean;
  isEmployee: boolean;
  preferences: UserPreferences | null;
}
