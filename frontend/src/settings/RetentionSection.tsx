import { Alert, Button, Group, NumberInput, Stack, Switch, Table, Text } from '@mantine/core';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api, type components } from '../api';
import { useAuth } from '../shell/auth/useAuth';
import { ErrorBanner } from '../shell/components/ErrorBanner';
import { SkeletonRows } from '../shell/components/SkeletonRows';
import { useAsync } from '../shell/hooks/useAsync';
import { fmtDate } from '../shell/lib/format';
import { problemText } from '../shell/lib/problem';

type Settings = components['schemas']['RetentionSettings'];

/** Retention (NFR-03): period, warning, notice automation, manual runs and run history. Admin edits; managers read. */
export function RetentionSection() {
  const { t, i18n } = useTranslation();
  const { isAdmin } = useAuth();
  const settings = useAsync(() => api.GET('/retention/settings'), []);
  const runs = useAsync(() => api.GET('/retention/runs', { params: { query: { size: 10 } } }), []);
  const flagged = useAsync(() => api.GET('/retention/flagged', { params: { query: { size: 5 } } }), []);
  const [form, setForm] = useState<Settings>();
  const [msg, setMsg] = useState<{ ok: boolean; text: string }>();
  useEffect(() => setForm(settings.data), [settings.data]);

  const save = async () => {
    if (!form) return;
    const r = await api.PUT('/retention/settings', { body: form });
    if (r.error) setMsg({ ok: false, text: problemText(r.error, t('shell.saveError')) });
    else {
      setMsg({ ok: true, text: t('settings.retention.saved') });
      settings.reload();
    }
  };
  const start = async (dryRun: boolean) => {
    const r = await api.POST('/retention/runs', { body: { dryRun } });
    setMsg(r.error ? { ok: false, text: problemText(r.error, t('shell.saveError')) } : undefined);
    runs.reload();
    flagged.reload();
  };

  if (settings.error !== undefined) return <ErrorBanner onRetry={settings.reload} />;
  if (!form) return <SkeletonRows n={3} />;
  return (
    <Stack>
      {!isAdmin && <Text c="dimmed" size="sm">{t('settings.common.adminOnly')}</Text>}
      <Group align="flex-end" wrap="wrap">
        <NumberInput label={t('settings.retention.months')} min={1} max={120} disabled={!isAdmin} value={form.retentionMonths} onChange={(v) => setForm({ ...form, retentionMonths: Number(v) || 1 })} />
        <NumberInput label={t('settings.retention.warning')} min={0} max={365} disabled={!isAdmin} value={form.warningDays} onChange={(v) => setForm({ ...form, warningDays: Number(v) || 0 })} />
      </Group>
      <Switch label={t('settings.retention.autoNotice')} disabled={!isAdmin} checked={form.autoSendNotice} onChange={(e) => setForm({ ...form, autoSendNotice: e.currentTarget.checked })} />
      <Switch label={t('settings.retention.renew')} disabled={!isAdmin} checked={form.renewOnConsent ?? true} onChange={(e) => setForm({ ...form, renewOnConsent: e.currentTarget.checked })} />
      {msg && <Alert color={msg.ok ? 'green' : 'red'} role={msg.ok ? 'status' : 'alert'}>{msg.text}</Alert>}
      {isAdmin && (
        <Group>
          <Button onClick={() => void save()}>{t('settings.retention.save')}</Button>
          <Button variant="default" onClick={() => void start(true)}>{t('settings.retention.dryRun')}</Button>
          <Button variant="default" color="red" onClick={() => void start(false)}>{t('settings.retention.runNow')}</Button>
        </Group>
      )}
      <Text fw={600}>{t('settings.retention.flaggedPeople')}</Text>
      {flagged.data?.content.length === 0 && <Text c="dimmed" size="sm">{t('settings.common.none')}</Text>}
      {flagged.data?.content.map((i) => (
        <Text key={i.person.id} size="sm">{i.person.name} - {t('settings.retention.until')} {fmtDate(i.retention.retainUntil, i18n.language)}</Text>
      ))}
      <Text fw={600}>{t('settings.retention.runs')}</Text>
      {runs.data?.content.length === 0 && <Text c="dimmed" size="sm">{t('settings.common.none')}</Text>}
      {runs.data && runs.data.content.length > 0 && (
        <Table withTableBorder>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>{t('settings.retention.started')}</Table.Th><Table.Th>{t('settings.retention.kind')}</Table.Th>
              <Table.Th>{t('settings.retention.flagged')}</Table.Th><Table.Th>{t('settings.retention.anonymised')}</Table.Th><Table.Th>{t('settings.retention.deleted')}</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {runs.data.content.map((r) => (
              <Table.Tr key={r.id}>
                <Table.Td>{fmtDate(r.startedAt, i18n.language)}</Table.Td>
                <Table.Td>{t(r.triggeredBy ? 'settings.retention.manual' : 'settings.retention.nightly')} / {t(r.dryRun ? 'settings.retention.dry' : 'settings.retention.real')}</Table.Td>
                <Table.Td>{r.flaggedCount}</Table.Td><Table.Td>{r.anonymisedCount}</Table.Td><Table.Td>{r.deletedCount}</Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      )}
    </Stack>
  );
}
