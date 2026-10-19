// /auth/me comes from the shell's AuthProvider, so it is fetched once per session.
import { useAuth } from '../../shell/auth/useAuth';
import type { Schemas } from './types';

export function useCurrentUser(): Schemas['CurrentUser'] | undefined {
  return useAuth().user ?? undefined;
}
