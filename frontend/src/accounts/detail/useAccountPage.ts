import { useState } from 'react';
import { api } from '../../api';
import { useAsync } from '../../shell/hooks/useAsync';

type Kind = 'ACCOUNT' | 'ACCOUNT_STATUS' | 'CONTACT' | 'ACTIVITY' | 'PROPOSAL' | 'JOB_ORDER' | 'CANDIDATE';

/** Everything the account page shows (UX-05): one hook, one reload. */
export function useAccountPage(id: string) {
  const [showEnded, setShowEnded] = useState(false);
  const [kind, setKind] = useState<Kind | ''>('');
  const path = { params: { path: { id } } };
  const account = useAsync(() => api.GET('/accounts/{id}', path), [id]);
  const dashboard = useAsync(() => api.GET('/accounts/{id}/dashboard', path), [id]);
  const contacts = useAsync(() => api.GET('/accounts/{id}/contacts', { params: { path: { id }, query: { active: !showEnded } } }), [id, showEnded]);
  const jobs = useAsync(() => api.GET('/job-orders', { params: { query: { accountId: id, size: 50 } } }), [id]);
  const proposals = useAsync(() => api.GET('/proposals', { params: { query: { accountId: id } } }), [id]);
  const timeline = useAsync(() => api.GET('/accounts/{id}/timeline', { params: { path: { id }, query: { kind: kind ? [kind] : undefined, size: 30 } } }), [id, kind]);

  const reloadAll = () => {
    account.reload();
    dashboard.reload();
    contacts.reload();
    jobs.reload();
    proposals.reload();
    timeline.reload();
  };
  return { account, dashboard, contacts, jobs, proposals, timeline, showEnded, setShowEnded, kind, setKind, reloadAll };
}
