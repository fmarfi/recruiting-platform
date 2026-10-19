import { useEffect, useMemo, useState, type ReactNode } from 'react';
import i18n from 'i18next';
import { api } from '../../api';
import { AuthContext, type AuthCtx, type AuthStatus, type CurrentUser } from './useAuth';

function applyLanguage(u: CurrentUser) {
  const l = u.language?.toLowerCase();
  if (l && l !== i18n.language) void i18n.changeLanguage(l);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [status, setStatus] = useState<AuthStatus>('loading');

  useEffect(() => {
    // Any 401 on a data call means the session is gone: drop to the login page (route guard does the redirect).
    const mw = {
      onResponse({ response, schemaPath }: { response: Response; schemaPath: string }) {
        if (response.status === 401 && schemaPath !== '/auth/login' && schemaPath !== '/auth/me') {
          setUser(null);
          setStatus('anon');
        }
        return undefined;
      },
    };
    api.use(mw);
    let alive = true;
    api.GET('/auth/me').then(
      ({ data }) => {
        if (!alive) return;
        if (data) {
          setUser(data);
          setStatus('authed');
          applyLanguage(data);
        } else setStatus('anon');
      },
      () => alive && setStatus('anon'),
    );
    return () => {
      alive = false;
      api.eject(mw);
    };
  }, []);

  const value = useMemo<AuthCtx>(
    () => ({
      user,
      status,
      isManager: user?.role === 'MANAGER' || user?.role === 'ADMIN',
      isAdmin: user?.role === 'ADMIN',
      async login(email, password) {
        try {
          const { error, response } = await api.POST('/auth/login', { body: { email, password } });
          if (error || !response.ok) return response.status === 401 || response.status === 400 ? 'invalid' : 'error';
          const me = await api.GET('/auth/me');
          if (!me.data) return 'error';
          setUser(me.data);
          setStatus('authed');
          applyLanguage(me.data);
          return 'ok';
        } catch {
          return 'error';
        }
      },
      async logout() {
        try {
          await api.POST('/auth/logout');
        } finally {
          setUser(null);
          setStatus('anon');
        }
      },
    }),
    [user, status],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
