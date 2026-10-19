import { api } from '../../api';
import { useAsync } from '../../pipeline/shared';

/** Everything the person page shows. Related lists load separately so one failing list does not blank the page. */
export function usePerson(id: string) {
  const person = useAsync(() => api.GET('/people/{id}', { params: { path: { id } } }), [id]);
  const links = useAsync(() => api.GET('/people/{id}/links', { params: { path: { id } } }), [id]);
  const applications = useAsync(() => api.GET('/people/{id}/applications', { params: { path: { id } } }), [id]);
  const roles = useAsync(() => api.GET('/people/{id}/contact-roles', { params: { path: { id } } }), [id]);
  const activities = useAsync(() => api.GET('/activities', { params: { query: { personId: id, size: 50 } } }), [id]);
  const actions = useAsync(() => api.GET('/next-actions', { params: { query: { personId: id, done: false } } }), [id]);
  const consent = useAsync(() => api.GET('/people/{id}/consent', { params: { path: { id } } }), [id]);
  const reloadAll = () => {
    person.reload();
    links.reload();
    applications.reload();
    roles.reload();
    activities.reload();
    actions.reload();
    consent.reload();
  };
  return { person, links, applications, roles, activities, actions, consent, reloadAll };
}
