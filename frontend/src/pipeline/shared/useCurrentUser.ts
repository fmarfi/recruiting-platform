import { useEffect, useState } from 'react';
import { api } from '../../api';
import type { Schemas } from './types';

export function useCurrentUser() {
  const [me, setMe] = useState<Schemas['CurrentUser']>();
  useEffect(() => {
    let live = true;
    api
      .GET('/auth/me')
      .then((r) => live && setMe(r.data))
      .catch(() => undefined);
    return () => {
      live = false;
    };
  }, []);
  return me;
}

