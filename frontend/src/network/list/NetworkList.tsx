// Network list (A-12): who do we know and who should I contact? Smart lists, DataGrid (columns, sort, density, saved views, CSV) and bulk consent requests.
import { Alert, Badge, Button, Chip, CloseButton, Container, Group, Pagination, SegmentedControl, Select, Stack, Text, TextInput, Title } from '@mantine/core';
import { useMediaQuery } from '@mantine/hooks';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import { api } from '../../api';
import { useCurrentUser, useLabel, type Schemas } from '../../pipeline/shared';
import { useAuth } from '../../shell/auth/useAuth';
import { EmptyState } from '../../shell/components/EmptyState';
import { DataGrid, ViewsMenu, formatSort, localViewStore, useGridColumns } from '../../shell/grid';
import { tokens } from '../../theme/tokens';
import { downloadCsv, toCsv, visibleCsv } from './csv';
import { NewPersonDialog } from './NewPersonDialog';
import { PresetsBar } from './PresetsBar';
import { clientFilter, DEFAULT_FILTERS, filtersFromParams, paramsFromFilters, PERSON_TYPES, viewSort, type ViewFilters } from './networkFilters';
import { PersonCard, usePersonColumns, type Person } from './personColumns';
import { EXPORT_LIMIT, fetchAllPeople, usePeople, type PeopleFilters } from './usePeople';

const GRID_ID = 'network';

export function NetworkList() {
  const { t, i18n } = useTranslation();
  const phone = useMediaQuery(`(max-width: ${tokens.breakpoints.phone}px)`);
  const me = useCurrentUser();
  const { isManager } = useAuth();
  const { ref, label } = useLabel();
  // Filters, page and sort are the URL (command bar links, Back, reload). Replace avoids a history entry per keystroke.
  const [params, setParams] = useSearchParams();
  const { filters: f, page } = useMemo(() => filtersFromParams(params), [params]);
  const [newOpen, setNewOpen] = useState(false);
  const [notice, setNotice] = useState<{ color: string; text: string }>();
  const [busy, setBusy] = useState(false);
  const [viewWarn, setViewWarn] = useState(false);
  const defs = usePersonColumns();
  const columns = useGridColumns<Person>(GRID_ID, defs);
  const views = useMemo(() => localViewStore<ViewFilters>(GRID_ID, () => setViewWarn(true)), []);

  const filters: PeopleFilters = { q: f.q, types: f.types, ownerId: f.mine ? me?.id : undefined, consent: f.consent, staleDays: f.staleDays, sort: f.sort, page };
  const res = usePeople(filters);
  const people = useMemo(() => clientFilter(res.data?.content ?? [], f), [res.data, f.city, f.company]);
  const set = (next: ViewFilters, p = 0) => setParams(paramsFromFilters(next, p), { replace: true });
  const change = (patch: Partial<ViewFilters>) => set({ ...f, ...patch });
  const filtered = !!(f.q || f.types.length || f.mine || f.consent || f.staleDays || f.city || f.company);
  const sort = viewSort(f);

  const exportCsv = async () => {
    setBusy(true);
    setNotice(undefined);
    try {
      const { people: all, total } = await fetchAllPeople(filters);
      const { header, rows } = visibleCsv(columns.visible, clientFilter(all, f));
      downloadCsv(`network-${new Date().toISOString().slice(0, 10)}.csv`, toCsv(header, rows));
      if (total > all.length) setNotice({ color: 'yellow', text: t('network.exportCapped', { count: all.length, limit: EXPORT_LIMIT }) });
    } catch {
      setNotice({ color: 'red', text: t('network.exportFailed') });
    } finally {
      setBusy(false);
    }
  };
  const exportSelected = (ids: string[]) => {
    const { header, rows } = visibleCsv(columns.visible, people.filter((p) => ids.includes(p.id)));
    downloadCsv(`network-selected-${new Date().toISOString().slice(0, 10)}.csv`, toCsv(header, rows));
  };

  const requestConsent = async (ids: string[], clear: () => void) => {
    setBusy(true);
    setNotice(undefined);
    const r = await api.POST('/consent-requests/bulk', { body: { personIds: ids, language: i18n.language === 'en' ? 'EN' : 'TR' } });
    setBusy(false);
    if (r.data) {
      setNotice({ color: 'green', text: t('network.bulkConsentDone', { queued: r.data.queued, skipped: r.data.skipped }) });
      clear();
      res.reload();
    } else setNotice({ color: 'red', text: t('network.consentFailed') });
  };

  const pending = [f.city && `${t('network.fields.city')}: ${f.city}`, f.company && `${t('network.fields.company')}: ${f.company}`].filter(Boolean) as string[];

  return (
    <Container maw="var(--content-max-width)" fluid>
      <Group justify="space-between" mb="md">
        <Title order={1} data-testid="screen-title">
          {t('nav.network')}
          {res.data ? ` (${res.data.page.totalElements})` : ''}
        </Title>
        <Group>
          <Button variant="default" loading={busy} disabled={!res.data?.page.totalElements} onClick={() => void exportCsv()}>
            {t('network.exportAll')}
          </Button>
          <Button onClick={() => setNewOpen(true)}>+ {t('network.newPerson')}</Button>
        </Group>
      </Group>
      <PresetsBar current={f} onApply={(v) => set({ ...DEFAULT_FILTERS, ...v })} />
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
          data={[...new Set([30, 60, 90, 180, f.staleDays].filter(Boolean))].sort((a, b) => a - b).map((d) => ({ value: String(d), label: t('network.daysAgo', { count: d }) }))}
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
        <Group gap="xs" mb="sm" style={{ overflowX: 'auto' }} wrap="nowrap">
          {PERSON_TYPES.map((x) => (
            <Chip key={x} value={x} size="sm">
              {t(`network.types.${x}`)}
            </Chip>
          ))}
        </Group>
      </Chip.Group>
      {pending.length > 0 && (
        <Group gap="xs" mb="sm" data-testid="pending-chips">
          {(['city', 'company'] as const).map(
            (k) =>
              f[k] && (
                <Badge key={k} variant="outline" color="gray" tt="none" style={{ opacity: 0.7 }} title={t('network.pageOnlyHint')} rightSection={<CloseButton size="xs" aria-label={t('network.removeFilter')} onClick={() => change({ [k]: '' })} />}>
                  {t(`network.fields.${k}`)}: {f[k]} ({t('network.pageOnly')})
                </Badge>
              ),
          )}
        </Group>
      )}

      {notice && (
        <Alert color={notice.color} mb="sm" withCloseButton onClose={() => setNotice(undefined)} data-testid="network-notice">
          {notice.text}
        </Alert>
      )}
      <Stack>
        <DataGrid<Person>
          gridId={GRID_ID}
          columns={defs}
          columnsController={columns}
          rows={res.error ? undefined : res.data ? people : undefined}
          rowId={(p) => p.id}
          loading={res.loading}
          error={!!res.error}
          onRetry={res.reload}
          sort={sort}
          onSort={(s) => change({ sort: formatSort(s) })}
          selectable
          exportName="network"
          phone={!!phone}
          renderCard={(p) => <PersonCard p={p} />}
          bulkActions={({ selectedIds, clear }) => (
            <>
              <Button size="compact-sm" variant="default" onClick={() => exportSelected(selectedIds)}>
                {t('network.exportSelected')}
              </Button>
              {isManager && (
                <Button size="compact-sm" loading={busy} onClick={() => void requestConsent(selectedIds, clear)}>
                  {t('network.requestConsent')}
                </Button>
              )}
            </>
          )}
          toolbar={
            <>
              <ViewsMenu<ViewFilters>
                store={views}
                current={{ filters: f, columns: columns.state, sort }}
                onApply={(v) => {
                  set({ ...DEFAULT_FILTERS, ...v.filters, sort: v.sort ? formatSort(v.sort) : v.filters.sort ?? '' });
                  if (v.columns) columns.set(v.columns);
                }}
              />
              {viewWarn && (
                <Text size="xs" c="red" role="status">
                  {t('grid.viewsNotPersisted')}
                </Text>
              )}
            </>
          }
          empty={
            <EmptyState
              title={t(filtered ? 'network.emptyFiltered' : 'network.empty')}
              actions={
                filtered ? (
                  <Button variant="light" onClick={() => set(DEFAULT_FILTERS)}>
                    {t('network.clear')}
                  </Button>
                ) : undefined
              }
            />
          }
        />
        {(res.data?.page.totalPages ?? 0) > 1 && <Pagination total={res.data!.page.totalPages} value={page + 1} onChange={(p) => set(f, p - 1)} />}
      </Stack>
      <NewPersonDialog opened={newOpen} onClose={() => setNewOpen(false)} onCreated={res.reload} />
    </Container>
  );
}
