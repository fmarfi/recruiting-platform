import { Alert, Button, Card, Group, Stack, Text, Title } from '@mantine/core';
import { useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { api, type components } from '../../api';
import { StatusMenu } from '../../shell/components/StatusMenu';
import { InlineEditField, useUndoToast } from '../../shell/edit';
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
  onEdit: () => void;
}

/** Name, editable status badge, owner line and the two quick actions (A-02, UX-06). */
export function AccountHeader({ account: a, onLog, onAddAction, onChanged, onEdit }: Props) {
  const { t, i18n } = useTranslation();
  const ref = useReference();
  const showUndo = useUndoToast();
  const version = useRef(a.version);
  version.current = a.version;
  const statusPath = { params: { path: { id: a.id } } };
  const change = useStatusChange(
    async (status, reason) => {
      const previous = a.status;
      const r = await api.PATCH('/accounts/{id}/status', { ...statusPath, body: { status: status as Account['status'], reason, version: a.version } });
      const back = ref.def('account', status)?.next.find((n) => n.to === previous);
      if (!r.error && r.data && back && !back.reasonRequired)
        showUndo({
          message: t('accounts.edit.statusChanged', { status: ref.label('account', status) }),
          onUndo: async () => {
            const u = await api.PATCH('/accounts/{id}/status', { ...statusPath, body: { status: previous, version: r.data!.version } });
            if (u.error) throw u.error;
            onChanged();
          },
        });
      return r;
    },
    onChanged,
    t('shell.saveError'),
  );
  const renamed = useRef(a.version);
  const rename = async (name: string, expect: number) => {
    const r = await api.PATCH('/accounts/{id}', { ...statusPath, body: { name: name.trim(), version: expect } });
    if (r.error) throw r.error;
    version.current = r.data!.version;
    renamed.current = r.data!.version;
    onChanged();
  };
  return (
    <Card withBorder>
      <Stack gap="xs">
        <Group justify="space-between" align="flex-start">
          <Group>
            <InlineEditField
              label={t('accounts.edit.name')}
              value={a.name}
              validate={(v) => (v.trim() ? null : t('accounts.edit.nameRequired'))}
              display={(v) => (
                <Title order={1} data-testid="screen-title">
                  {v}
                </Title>
              )}
              onSave={(v) => rename(v, version.current)}
              onUndo={(prev) => rename(prev, renamed.current)}
            />
            <StatusMenu kind="account" value={a.status} onMove={change.move} />
          </Group>
          <Button variant="subtle" onClick={onEdit}>
            {t('accounts.edit.action')}
          </Button>
          <Group visibleFrom="sm">
            <Button onClick={onLog}>
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
