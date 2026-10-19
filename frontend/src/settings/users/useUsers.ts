import { api } from '../../api';
import type { components } from '../../api';
import { useAsync } from '../../shell/hooks/useAsync';

export type User = components['schemas']['User'];

/** Users list (GET /users); search is server-side. */
export function useUsers(q: string) {
  return useAsync(() => api.GET('/users', { params: { query: { q: q || undefined, size: 100 } } }), [q]);
}

export function patchUser(u: User, body: { role?: User['role']; active?: boolean }) {
  return api.PATCH('/users/{id}', { params: { path: { id: u.id } }, body: { ...body, version: u.version } });
}
