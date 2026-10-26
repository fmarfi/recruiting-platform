import { Alert, Badge, Button, Group, Modal, SegmentedControl, Select, Stack, Table, Text, TextInput, Textarea, VisuallyHidden } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { AccountPicker } from '../accounts/components/AccountPicker';
import { useAuth } from '../shell/auth/useAuth';
import { EmptyState } from '../shell/components/EmptyState';
import { ErrorBanner } from '../shell/components/ErrorBanner';
import { SkeletonRows } from '../shell/components/SkeletonRows';
import { fmtDate } from '../shell/lib/format';
import { emptyForm, endDateFor, endsToday, OFF_LIMITS_REASONS, offLimitsErrorKey, toCreateBody, todayIso, useOffLimitsRules, validateForm, type OffLimitsForm, type OffLimitsRule } from './offLimits';

/** Off-limits companies (modules 5 + 9, ADR 0011): list, add with reason and optional end date, end or delete. MANAGER/ADMIN write. */
export function OffLimitsSection() {
  const { t, i18n } = useTranslation();
  const { isManager } = useAuth();
  const [filter, setFilter] = useState<'active' | 'all'>('active');
  const rules = useOffLimitsRules(filter === 'active' ? true : undefined);
  const [creating, setCreating] = useState(false);
  const [form, setForm] = useState<OffLimitsForm>(emptyForm());
  const [msg, setMsg] = useState<string>();
  const [busy, setBusy] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState<OffLimitsRule | null>(null);
  const lng = i18n.language;

  const fail = (status: number | undefined, error: unknown) => setMsg(t(`settings.offLimits.err.${offLimitsErrorKey(status, error)}`));
  const open = () => {
    setForm(emptyForm());
    setMsg(undefined);
    setCreating(true);
  };
  const create = async () => {
    const bad = validateForm(form, todayIso());
    if (bad) return setMsg(t(`settings.offLimits.err.${bad}`));
    setBusy(true);
    const r = await api.POST('/off-limits', { body: toCreateBody(form) });
    setBusy(false);
    if (r.error !== undefined) return fail(r.response.status, r.error);
    setCreating(false);
    rules.reload();
  };
  const [ending, setEnding] = useState<string | null>(null);
  const end = async (rule: OffLimitsRule) => {
    if (ending) return; // one request at a time
    setEnding(rule.id);
    const r = await api.PATCH('/off-limits/{id}', { params: { path: { id: rule.id } }, body: { endsOn: endDateFor(rule, todayIso()), version: rule.version } });
    setEnding(null);
    if (r.error !== undefined) fail(r.response.status, r.error);
    else setMsg(undefined);
    rules.reload();
  };
  const remove = async () => {
    if (!confirmDelete) return;
    const r = await api.DELETE('/off-limits/{id}', { params: { path: { id: confirmDelete.id } } });
    if (r.error !== undefined) fail(r.response.status, r.error);
    setConfirmDelete(null);
    rules.reload();
  };

  if (rules.error !== undefined) return <ErrorBanner onRetry={rules.reload} />;
  const set = (patch: Partial<OffLimitsForm>) => setForm((f) => ({ ...f, ...patch }));

  return (
    <Stack>
      <Text size="sm" c="dimmed">{t('settings.offLimits.hint')}</Text>
      {!isManager && <Text c="dimmed" size="sm">{t('settings.offLimits.managerOnly')}</Text>}
      <Group justify="space-between">
        <SegmentedControl
          aria-label={t('settings.offLimits.filter')}
          value={filter}
          onChange={(v) => setFilter(v as 'active' | 'all')}
          data={[{ value: 'active', label: t('settings.offLimits.activeOnly') }, { value: 'all', label: t('settings.offLimits.allRules') }]}
        />
        {isManager && <Button onClick={open}>{t('settings.offLimits.add')}</Button>}
      </Group>
      {msg && !creating && <Alert color="red" role="alert">{msg}</Alert>}
      {rules.loading && !rules.data && <SkeletonRows n={3} />}
      {rules.data && rules.data.content.length === 0 && <EmptyState title={t('settings.offLimits.empty')} hint={t('settings.offLimits.emptyHint')} />}
      {rules.data && rules.data.content.length > 0 && (
        <Table.ScrollContainer minWidth={640}>
          <Table withTableBorder>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>{t('settings.offLimits.company')}</Table.Th>
                <Table.Th>{t('settings.offLimits.reason')}</Table.Th>
                <Table.Th>{t('settings.offLimits.period')}</Table.Th>
                <Table.Th>{t('settings.offLimits.state')}</Table.Th>
                {isManager && <Table.Th><VisuallyHidden>{t('settings.offLimits.actions')}</VisuallyHidden></Table.Th>}
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {rules.data.content.map((r) => (
                <Table.Tr key={r.id}>
                  <Table.Td>
                    {r.account ? <Link to={`/accounts/${r.account.id}`}>{r.account.name}</Link> : r.companyName}
                    {r.note && <Text size="xs" c="dimmed">{r.note}</Text>}
                  </Table.Td>
                  <Table.Td>{t(`settings.offLimits.reasons.${r.reason}`)}</Table.Td>
                  <Table.Td>{fmtDate(r.startsOn, lng)} - {r.endsOn ? fmtDate(r.endsOn, lng) : t('settings.offLimits.openEnded')}</Table.Td>
                  <Table.Td><Badge color={r.active ? 'red' : 'gray'} variant="light">{t(endsToday(r, todayIso()) ? 'settings.offLimits.endsToday' : r.active ? 'settings.offLimits.active' : 'settings.offLimits.ended')}</Badge></Table.Td>
                  {isManager && (
                    <Table.Td>
                      <Group gap="xs" wrap="nowrap">
                        {r.active && !endsToday(r, todayIso()) && <Button size="xs" variant="default" loading={ending === r.id} disabled={!!ending} onClick={() => void end(r)}>{t('settings.offLimits.end')}</Button>}
                        <Button size="xs" variant="subtle" color="red" onClick={() => setConfirmDelete(r)}>{t('settings.offLimits.delete')}</Button>
                      </Group>
                    </Table.Td>
                  )}
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      )}

      <Modal opened={creating} onClose={() => setCreating(false)} title={t('settings.offLimits.add')}>
        <Stack>
          <SegmentedControl
            aria-label={t('settings.offLimits.targetKind')}
            value={form.target}
            onChange={(v) => set({ target: v as 'account' | 'company' })}
            data={[{ value: 'account', label: t('settings.offLimits.targetAccount') }, { value: 'company', label: t('settings.offLimits.targetCompany') }]}
          />
          {form.target === 'account' ? (
            <AccountPicker label={t('settings.offLimits.account')} value={form.accountId} onChange={(id) => set({ accountId: id })} />
          ) : (
            <TextInput label={t('settings.offLimits.companyName')} required maxLength={200} value={form.companyName} onChange={(e) => set({ companyName: e.currentTarget.value })} />
          )}
          <Select
            label={t('settings.offLimits.reason')}
            allowDeselect={false}
            value={form.reason}
            onChange={(v) => v && set({ reason: v as OffLimitsForm['reason'] })}
            data={OFF_LIMITS_REASONS.map((r) => ({ value: r, label: t(`settings.offLimits.reasons.${r}`) }))}
          />
          <Textarea label={t('settings.offLimits.note')} description={t('settings.offLimits.noteHint')} maxLength={500} value={form.note} onChange={(e) => set({ note: e.currentTarget.value })} />
          <Group grow align="flex-start">
            <TextInput type="date" label={t('settings.offLimits.startsOn')} description={t('settings.offLimits.startsHint')} value={form.startsOn} onChange={(e) => set({ startsOn: e.currentTarget.value })} />
            <TextInput type="date" label={t('settings.offLimits.endsOn')} description={t('settings.offLimits.endsHint')} value={form.endsOn} onChange={(e) => set({ endsOn: e.currentTarget.value })} />
          </Group>
          {msg && <Alert color="red" role="alert">{msg}</Alert>}
          <Group justify="flex-end">
            <Button variant="default" onClick={() => setCreating(false)}>{t('shell.cancel')}</Button>
            <Button loading={busy} onClick={() => void create()}>{t('shell.save')}</Button>
          </Group>
        </Stack>
      </Modal>
      <Modal opened={!!confirmDelete} onClose={() => setConfirmDelete(null)} title={t('settings.offLimits.delete')} size="sm">
        <Stack>
          <Text size="sm">{t('settings.offLimits.deleteConfirm')}</Text>
          <Group justify="flex-end">
            <Button variant="default" onClick={() => setConfirmDelete(null)}>{t('shell.cancel')}</Button>
            <Button color="red" onClick={() => void remove()}>{t('settings.offLimits.delete')}</Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  );
}
