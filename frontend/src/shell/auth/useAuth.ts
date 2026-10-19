import { createContext, useContext } from 'react';
import type { components } from '../../api';

export type CurrentUser = components['schemas']['CurrentUser'];
export type AuthStatus = 'loading' | 'anon' | 'authed';

export interface AuthCtx {
  user: CurrentUser | null;
  status: AuthStatus;
  login(email: string, password: string): Promise<'ok' | 'invalid' | 'error'>;
  logout(): Promise<void>;
  isManager: boolean;
  isAdmin: boolean;
}

export const AuthContext = createContext<AuthCtx | null>(null);

export function useAuth(): AuthCtx {
  const c = useContext(AuthContext);
  if (!c) throw new Error('useAuth outside AuthProvider');
  return c;
}
