import { api } from '../../../api';
import { useAsync } from '../../shared';

/** Job order header data, its applications (the board) and its activity feed. */
export function useJobOrder(id: string) {
  const job = useAsync(() => api.GET('/job-orders/{id}', { params: { path: { id } } }), [id]);
  const apps = useAsync(() => api.GET('/job-orders/{id}/applications', { params: { path: { id } } }), [id]);
  const activities = useAsync(() => api.GET('/activities', { params: { query: { jobOrderId: id, size: 50 } } }), [id]);
  const actions = useAsync(() => api.GET('/next-actions', { params: { query: { jobOrderId: id, done: false } } }), [id]);
  return { job, apps, activities, actions };
}
