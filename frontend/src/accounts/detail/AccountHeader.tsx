import { Alert, Button, Card, Group, Stack, Text, Title } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { api, type components } from '../../api';
import { StatusMenu } from '../../shell/components/StatusMenu';
import { fmtDate } from '../../shell/lib/format';
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

/** Name, editable status badge, owner line and the two quick actions (A-02, UX-06). */
export function AccountHeader({ account: a, onLog, onAddAction, onChanged }: Props) {
  const { t, i18n } = useTranslation();
  const ref = useReference();
  const change = useStatusChange(
    (status, reason) => api.PATCH('/accounts/{id}/status', { params: { path: { id: a.id } }, body: { status: status as Account['status'], reason, version: a.version } }),
    onChanged,
    t('shell.saveError'),
  );
  return (
    <Card withBorder>
      <Stack gap="xs">
        <Group justify="space-between" align="flex-start">
          <Group>
            <Title order={1} data-testid="screen-title">
              {a.name}
            </Title>
            <StatusMenu kind="account" value={a.status} onMove={change.move} />
          </Group>
          <Group visibleFrom="sm">
            <Button variant="default" onClick={onLog}>
              {t('accounts.logActivity')}
            </Button>
            <Button variant="default" onClick={onAddAction}>
              {t('accounts.addAction')}
            </Button>
          </Group>
        </Group>
        <Text c="dimmed">
          {[a.sector, a.city].filter(Boolean).join(' - ')} - {t('accounts.ownerIs', { name: a.owner.name })} - {t('accounts.clientSince', { date: fmtDate(a.openedOn, i18n.language) })}
        </Text>
        {change.failure && <StatusFailureAlert failure={change.failure} />}
        {a.status === 'CLOSED' && <Alert color="gray">{t('accounts.closedBanner')}</Alert>}
      </Stack>
      <ReasonDialog opened={!!change.pending} target={change.pending ? ref.label('account', change.pending.to) : ''} onConfirm={change.confirm} onCancel={change.cancel} />
    </Card>
  );
}
