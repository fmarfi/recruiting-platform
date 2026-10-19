import { api } from '../../../api';
import { useAsync } from '../../shared';

/** Job order header data and its applications (the board). */
export function useJobOrder(id: string) {
  const job = useAsync(() => api.GET('/job-orders/{id}', { params: { path: { id } } }), [id]);
  const apps = useAsync(() => api.GET('/job-orders/{id}/applications', { params: { path: { id } } }), [id]);
  return { job, apps };
}
