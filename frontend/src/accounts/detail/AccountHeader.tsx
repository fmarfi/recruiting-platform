// Client header (visual system v3, sections 5 and 6b): RecordHeader band content, same shape as the person header.
import { Alert, Anchor, Button, Menu, Text } from '@mantine/core';
import { IconDots } from '@tabler/icons-react';
import { useRef, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { api, type components } from '../../api';
import { ClientIcon, ICON_SIZE, ICON_STROKE } from '../../shell/components/objectIcons';
import { RecordHeader } from '../../shell/components/RecordHeader';
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
  onAddJob: () => void;
  onAddAction: () => void;
  onChanged: () => void;
  onEdit: () => void;
  tabs?: ReactNode;
}

/** Name, editable status badge, subtitle, fact strip, actions and tabs in the RecordLayout band (A-02, UX-06). */
export function AccountHeader({ account: a, onLog, onAddJob, onAddAction, onChanged, onEdit, tabs }: Props) {
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
    { label: t('accounts.owner'), value: a.owner.name },
    { label: t('accounts.openSearches'), value: a.openJobCount },
    { label: t('accounts.lastActivity'), value: fmtRelative(a.lastActivityAt, i18n.language) },
  ];
  const subline = [a.sector, a.city].filter(Boolean).join(', ');
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
      <RecordHeader
        breadcrumb={
          <Anchor component={Link} to="/accounts" size="sm">
            {t('nav.accounts')}
          </Anchor>
        }
        avatar={<ClientIcon size={32} stroke={ICON_STROKE} color="var(--color-ink3)" aria-hidden />}
        name={
          <InlineEditField
            label={t('accounts.edit.name')}
            value={a.name}
            validate={(v) => (v.trim() ? null : t('accounts.edit.nameRequired'))}
            display={(v) => (
              <Text span ff="var(--font-display)" fz="1.5rem" fw={600} c="var(--color-ink)">
                {v}
              </Text>
            )}
            onSave={(v) => rename(v, version.current)}
            onUndo={(prev) => rename(prev, renamed.current)}
          />
        }
        badge={<StatusMenu kind="account" value={a.status} onMove={change.move} />}
        subtitle={subline || undefined}
        actions={
          <>
            <Menu position="bottom-end" shadow="md" withinPortal>
              <Menu.Target>
                <Button variant="default" aria-label={t('accounts.record.moreActions')}>
                  <IconDots size={ICON_SIZE} stroke={ICON_STROKE} />
                </Button>
              </Menu.Target>
              <Menu.Dropdown>
                <Menu.Item onClick={onEdit}>{t('accounts.edit.action')}</Menu.Item>
                <Menu.Item onClick={onAddAction}>{t('accounts.addAction')}</Menu.Item>
              </Menu.Dropdown>
            </Menu>
            <Button variant="default" onClick={onAddJob}>
              {t('accounts.newJob')}
            </Button>
            <Button onClick={onLog}>{t('accounts.logActivity')}</Button>
          </>
        }
        facts={facts}
        tabs={tabs}
      />
      {change.failure && <StatusFailureAlert failure={change.failure} />}
      {a.status === 'CLOSED' && <Alert color="gray">{t('accounts.closedBanner')}</Alert>}
      <ReasonDialog opened={!!change.pending} target={change.pending ? ref.label('account', change.pending.to) : ''} onConfirm={change.confirm} onCancel={change.cancel} />
    </>
  );
}
