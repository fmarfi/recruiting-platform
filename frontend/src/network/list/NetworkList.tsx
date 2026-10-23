// Network list (A-12): who do we know and who should I contact? Smart lists, sortable columns, CSV export and bulk consent requests.
import { Alert, Button, Chip, Container, Group, Pagination, Select, SegmentedControl, Skeleton, Text, TextInput, Title } from '@mantine/core';
import { useMediaQuery } from '@mantine/hooks';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api';
import { useCurrentUser, useLabel, type Schemas } from '../../pipeline/shared';
import { useAuth } from '../../shell/auth/useAuth';
import { PersonCards, PersonTable } from '../components/PersonRows';
import { downloadCsv, toCsv } from './csv';
import { NewPersonDialog } from './NewPersonDialog';
import { SavedViewsBar } from './SavedViewsBar';
import { DEFAULT_FILTERS, loadViews, storeViews, type SavedView, type ViewFilters } from './savedViews';
import { EXPORT_LIMIT, fetchAllPeople, usePeople, type PeopleFilters } from './usePeople';

const TYPES: Schemas['PersonType'][] = ['EXECUTIVE', 'CLIENT_CONTACT', 'REFERRER', 'CANDIDATE'];

export function NetworkList() {
  const { t, i18n } = useTranslation();
  const phone = useMediaQuery('(max-width: 48em)');
  const me = useCurrentUser();
  const { isManager } = useAuth();
  const { ref, label } = useLabel();
  const [f, setF] = useState<ViewFilters>(DEFAULT_FILTERS);
  const [page, setPage] = useState(0);
  const [newOpen, setNewOpen] = useState(false);
  const [views, setViews] = useState<SavedView[]>(loadViews);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [notice, setNotice] = useState<{ color: string; text: string }>();
  const [busy, setBusy] = useState(false);
  const filters: PeopleFilters = { q: f.q, types: f.types, ownerId: f.mine ? me?.id : undefined, consent: f.consent, staleDays: f.staleDays, sort: f.sort, page };
  const res = usePeople(filters);
  const people = res.data?.content ?? [];
  const change = (patch: Partial<ViewFilters>) => {
    setF((x) => ({ ...x, ...patch }));
    setPage(0);
    setSelected(new Set());
  };
  const apply = (v: ViewFilters) => change({ ...DEFAULT_FILTERS, ...v });
  const filtered = !!(f.q || f.types.length || f.mine || f.consent || f.staleDays);

  const saveView = (name: string) => {
    const next = [...views, { id: crypto.randomUUID(), name, filters: f }];
    setViews(next);
    storeViews(next);
  };
  const deleteView = (id: string) => {
    const next = views.filter((v) => v.id !== id);
    setViews(next);
    storeViews(next);
  };
  const sortBy = (field: string) => change({ sort: f.sort.startsWith(`${field},`) ? `${field},${f.sort.endsWith('asc') ? 'desc' : 'asc'}` : `${field},asc` });

  const selection = {
    selected,
    onToggle: (id: string) =>
      setSelected((s) => {
        const n = new Set(s);
        if (!n.delete(id)) n.add(id);
        return n;
      }),
    onToggleAll: (ids: string[], on: boolean) =>
      setSelected((s) => {
        const n = new Set(s);
        ids.forEach((id) => (on ? n.add(id) : n.delete(id)));
        return n;
      }),
  };

  const exportCsv = async (onlySelected: boolean) => {
    setBusy(true);
    setNotice(undefined);
    try {
      const { people: all, total } = onlySelected ? { people: people.filter((p) => selected.has(p.id)), total: selected.size } : await fetchAllPeople(filters);
      const csv = toCsv(
        [t('network.fields.name'), t('network.fields.title'), t('network.fields.company'), t('network.fields.type'), t('network.fields.city'), t('network.fields.email'), t('network.fields.owner'), t('network.fields.lastContact'), t('network.fields.consent')],
        all.map((p) => [p.name, p.title, p.company, t(`network.types.${p.type}`), p.city, p.email, p.owner.name, p.lastContactAt?.slice(0, 10), label('consent', p.consentStatus)]),
      );
      downloadCsv(`network-${new Date().toISOString().slice(0, 10)}.csv`, csv);
      if (total > all.length) setNotice({ color: 'yellow', text: t('network.exportCapped', { count: all.length, limit: EXPORT_LIMIT }) });
    } catch {
      setNotice({ color: 'red', text: t('network.exportFailed') });
    } finally {
      setBusy(false);
    }
  };

  const requestConsent = async () => {
    setBusy(true);
    setNotice(undefined);
    const r = await api.POST('/consent-requests/bulk', { body: { personIds: [...selected], language: i18n.language === 'en' ? 'EN' : 'TR' } });
    setBusy(false);
    if (r.data) {
      setNotice({ color: 'green', text: t('network.bulkConsentDone', { queued: r.data.queued, skipped: r.data.skipped }) });
      setSelected(new Set());
      res.reload();
    } else setNotice({ color: 'red', text: t('network.consentFailed') });
  };

  return (
    <Container maw="var(--content-max-width)" fluid>
      <Group justify="space-between" mb="md">
        <Title order={1} data-testid="screen-title">
          {t('nav.network')}
          {res.data ? ` (${res.data.page.totalElements})` : ''}
        </Title>
        <Group>
          <Button variant="default" loading={busy} disabled={!res.data?.page.totalElements} onClick={() => void exportCsv(false)}>
            {t('network.exportCsv')}
          </Button>
          <Button onClick={() => setNewOpen(true)}>+ {t('network.newPerson')}</Button>
        </Group>
      </Group>
      <SavedViewsBar current={f} views={views} onApply={apply} onSave={saveView} onDelete={deleteView} />
      <Group mb="sm" align="flex-end">
        <TextInput label={t('network.search')} value={f.q} onChange={(e) => change({ q: e.currentTarget.value })} />
        <SegmentedControl
          value={f.mine ? 'me' : 'all'}
          onChange={(v) => change({ mine: v === 'me' })}
          data={[
            { value: 'all', label: t('network.all') },
            { value: 'me', label: t('network.mine') },
          ]}
        />
        <Select
          label={t('network.filterNotContacted')}
          clearable
          value={f.staleDays ? String(f.staleDays) : null}
          onChange={(v) => change({ staleDays: v ? Number(v) : 0 })}
          data={[30, 60, 90, 180].map((d) => ({ value: String(d), label: t('network.daysAgo', { count: d }) }))}
        />
        <Select
          label={t('network.fields.consent')}
          clearable
          value={f.consent ?? null}
          onChange={(v) => change({ consent: (v as Schemas['ConsentStatus']) ?? undefined })}
          data={(ref?.lifecycles.consent.statuses ?? []).map((s) => ({ value: s.value, label: label('consent', s.value) }))}
        />
      </Group>
      <Chip.Group multiple value={f.types} onChange={(v) => change({ types: v as Schemas['PersonType'][] })}>
        <Group gap="xs" mb="md" style={{ overflowX: 'auto' }} wrap="nowrap">
          {TYPES.map((x) => (
            <Chip key={x} value={x} size="sm">
              {t(`network.types.${x}`)}
            </Chip>
          ))}
        </Group>
      </Chip.Group>

      {notice && (
        <Alert color={notice.color} mb="sm" withCloseButton onClose={() => setNotice(undefined)} data-testid="network-notice">
          {notice.text}
        </Alert>
      )}
      {selected.size > 0 && (
        <Group mb="sm" p="xs" gap="sm" bg="var(--mantine-primary-color-light)" style={{ borderRadius: 'var(--mantine-radius-sm)' }} data-testid="bulk-bar">
          <Text size="sm" fw={600}>
            {t('network.selected', { count: selected.size })}
          </Text>
          <Button size="compact-sm" variant="default" loading={busy} onClick={() => void exportCsv(true)}>
            {t('network.exportSelected')}
          </Button>
          {isManager && (
            <Button size="compact-sm" loading={busy} onClick={() => void requestConsent()}>
              {t('network.requestConsent')}
            </Button>
          )}
          <Button size="compact-sm" variant="subtle" onClick={() => setSelected(new Set())}>
            {t('network.clearSelection')}
          </Button>
        </Group>
      )}

      {res.error ? (
        <Alert color="red" role="alert" title={t('network.loadFailed')}>
          <Button variant="light" onClick={res.reload}>
            {t('common.retry')}
          </Button>
        </Alert>
      ) : res.loading && !res.data ? (
        <Skeleton height={200} data-testid="list-loading" />
      ) : people.length === 0 ? (
        <Group data-testid="list-empty">
          <Text c="dimmed">{t(filtered ? 'network.emptyFiltered' : 'network.empty')}</Text>
          {filtered && (
            <Button variant="light" onClick={() => change({ ...DEFAULT_FILTERS })}>
              {t('network.clear')}
            </Button>
          )}
        </Group>
      ) : phone ? (
        <PersonCards people={people} selection={selection} />
      ) : (
        <PersonTable people={people} selection={selection} sort={{ sort: f.sort, onSort: sortBy }} />
      )}
      {(res.data?.page.totalPages ?? 0) > 1 && (
        <Pagination
          mt="md"
          total={res.data!.page.totalPages}
          value={page + 1}
          onChange={(p) => {
            setPage(p - 1);
            setSelected(new Set());
          }}
        />
      )}
      <NewPersonDialog opened={newOpen} onClose={() => setNewOpen(false)} onCreated={res.reload} />
    </Container>
  );
}
