import { Alert, Anchor, Avatar, Button, Group, Text } from '@mantine/core';
import { useRef, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { api, type components } from '../../api';
import { PageHeader } from '../../shell/components/PageHeader';
import { ClientIcon, ICON_SIZE_NAV, ICON_STROKE } from '../../shell/components/objectIcons';
import { StatusMenu } from '../../shell/components/StatusMenu';
import { InlineEditField, useUndoToast } from '../../shell/edit';
import { fmtRelative } from '../../shell/lib/format';
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

/** Name, editable status badge, the fact row and the actions, on the canvas via PageHeader (A-02, UX-06). */
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
  const facts: { label: string; value: ReactNode }[] = [
    { label: t('accounts.sector'), value: a.sector },
    { label: t('accounts.city'), value: a.city },
    { label: t('accounts.owner'), value: a.owner.name },
    { label: t('accounts.openSearches'), value: a.openJobCount },
    { label: t('accounts.lastActivity'), value: fmtRelative(a.lastActivityAt, i18n.language) },
  ];
  const renamed = useRef(a.version);
  const rename = async (name: string, expect: number) => {
    const r = await api.PATCH('/accounts/{id}', { ...statusPath, body: { name: name.trim(), version: expect } });
    if (r.error) throw r.error;
    version.current = r.data!.version;
    renamed.current = r.data!.version;
    onChanged();
  };
  return (
    <>
      <PageHeader
        breadcrumb={
          <Anchor component={Link} to="/accounts" underline="hover" c="var(--color-ink3)">
            {t('nav.accounts')}
          </Anchor>
        }
        title={
          <Group gap="sm" wrap="nowrap" align="center">
            <Avatar size={40} radius="xl" aria-hidden style={{ background: 'var(--color-surface2)', color: 'var(--color-ink2)', flex: 'none' }}>
              <ClientIcon size={ICON_SIZE_NAV} stroke={ICON_STROKE} />
            </Avatar>
            <InlineEditField
              label={t('accounts.edit.name')}
              value={a.name}
              validate={(v) => (v.trim() ? null : t('accounts.edit.nameRequired'))}
              display={(v) => (
                <Text component="span" fw={600} style={{ fontSize: 'var(--mantine-h1-font-size)', lineHeight: 1.25 }}>
                  {v}
                </Text>
              )}
              onSave={(v) => rename(v, version.current)}
              onUndo={(prev) => rename(prev, renamed.current)}
            />
          </Group>
        }
        badge={<StatusMenu kind="account" value={a.status} onMove={change.move} />}
        actions={
          <>
            <Button variant="default" onClick={onEdit}>
              {t('accounts.edit.action')}
            </Button>
            <Button variant="default" onClick={onAddAction}>
              {t('accounts.addAction')}
            </Button>
            <Button onClick={onLog}>{t('accounts.logActivity')}</Button>
          </>
        }
        subtitle={
          <span style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(11rem, 1fr))', gap: 'var(--space-3) var(--space-5)' }}>
            {facts.map((f) => (
              <span key={f.label} style={{ minWidth: 0 }}>
                <Text span display="block" size="xs" c="dimmed" fw={500} style={{ marginBottom: 'var(--space-2)' }}>
                  {f.label}
                </Text>
                <Text span display="block" c="var(--color-ink)" fw={500} style={{ overflowWrap: 'anywhere' }}>
                  {f.value ?? '-'}
                </Text>
              </span>
            ))}
          </span>
        }
      />
      {change.failure && <StatusFailureAlert failure={change.failure} />}
      {a.status === 'CLOSED' && <Alert color="gray">{t('accounts.closedBanner')}</Alert>}
      <ReasonDialog opened={!!change.pending} target={change.pending ? ref.label('account', change.pending.to) : ''} onConfirm={change.confirm} onCancel={change.cancel} />
    </>
  );
}
