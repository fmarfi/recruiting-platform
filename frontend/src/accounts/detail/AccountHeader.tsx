import { Alert, Box, Button } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { api, type components } from '../../api';
import { Path } from '../../shell/ui/Path';
import { PageHeader } from '../../shell/ui/PageHeader';
import { useReference } from '../../shell/refdata/useReference';
import { ReasonDialog } from '../components/ReasonDialog';
import { StatusFailureAlert } from '../components/StatusFailureAlert';
import { useStatusChange } from './useStatusChange';

type Account = components['schemas']['Account'];

interface Props {
  account: Account;
  onLog: () => void;
  onAddAction: () => void;
  onChanged: () => void;
}

/** Breadcrumb, title, quick actions and the status Path (A-02, UX-06). A step click is the same move as the old badge menu. */
export function AccountHeader({ account: a, onLog, onAddAction, onChanged }: Props) {
  const { t } = useTranslation();
  const ref = useReference();
  const change = useStatusChange(
    (status, reason) => api.PATCH('/accounts/{id}/status', { params: { path: { id: a.id } }, body: { status: status as Account['status'], reason, version: a.version } }),
    onChanged,
    t('shell.saveError'),
  );
  return (
    <>
      <PageHeader
        title={a.name}
        glyph="A"
        crumbs={[{ label: t('nav.accounts'), to: '/accounts' }, { label: a.name }]}
        actions={
          <>
            <Button variant="default" visibleFrom="sm" onClick={onLog}>{t('accounts.logActivity')}</Button>
            <Button variant="default" visibleFrom="sm" onClick={onAddAction}>{t('accounts.addAction')}</Button>
          </>
        }
      />
      <Box mb="md">
        <Path kind="account" value={a.status} onMove={change.move} />
        {change.failure && <StatusFailureAlert failure={change.failure} />}
        {a.status === 'CLOSED' && <Alert color="gray" mt="xs">{t('accounts.closedBanner')}</Alert>}
      </Box>
      <ReasonDialog opened={!!change.pending} target={change.pending ? ref.label('account', change.pending.to) : ''} onConfirm={change.confirm} onCancel={change.cancel} />
    </>
  );
}
