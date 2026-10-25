// Flat list of job orders for 3-click access (UX-03): defaults to open jobs owned by me. DataGrid with column chooser, sort, density, views and CSV.
import { Button, Chip, Container, Group, Pagination, SegmentedControl, Stack, Text, TextInput, Title } from '@mantine/core';
import { useMediaQuery } from '@mantine/hooks';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import { EmptyState } from '../../../shell/components/EmptyState';
import { DataGrid, ViewsMenu, formatSort, localViewStore, parseSort, useGridColumns } from '../../../shell/grid';
import { tokens } from '../../../theme/tokens';
import { useCurrentUser, useLabel, type Schemas } from '../../shared';
import { JobCard, useJobColumns, type JobOrderRow } from './jobColumns';
import { DEFAULT_JOB_SORT, jobParamsFromState, jobStateFromParams, type JobListState } from './jobFilters';
import { useJobOrders } from './useJobOrders';

const GRID_ID = 'job-orders';
type JobView = Pick<JobListState, 'q' | 'status' | 'mine'>;

export function JobOrdersList() {
  const { t } = useTranslation();
  const me = useCurrentUser();
  const { ref, label } = useLabel();
  const phone = useMediaQuery(`(max-width: ${tokens.breakpoints.phone}px)`);
  // Filters, page and sort are the URL (command bar links, Back, reload); replace keeps history clean.
  const [params, setParams] = useSearchParams();
  const s = useMemo(() => jobStateFromParams(params), [params]);
  const set = (patch: Partial<JobListState>, resetPage = true) => setParams(jobParamsFromState({ ...s, ...(resetPage ? { page: 0 } : {}), ...patch }), { replace: true });
  const [viewWarn, setViewWarn] = useState(false);
  const defs = useJobColumns();
  const columns = useGridColumns<JobOrderRow>(GRID_ID, defs);
  const views = useMemo(() => localViewStore<JobView>(GRID_ID, () => setViewWarn(true)), []);
  // Wait for /auth/me before the first query so "mine" is applied from the start.
  const ownerId = s.mine ? me?.id : undefined;
  const res = useJobOrders({ q: s.q, status: s.status, openOnly: true, ownerId, page: s.page, sort: s.sort });
  const statuses = ref?.lifecycles.jobOrder.statuses ?? [];
  const sort = parseSort(s.sort) ?? parseSort(DEFAULT_JOB_SORT);
  const filtered = !!(s.q || s.status.length || !s.mine);

  return (
    <Container maw="var(--content-max-width)" fluid>
      <Title order={1} data-testid="screen-title" mb="md">
        {t('pipeline.list.title')}
      </Title>
      <Group mb="sm" align="flex-end">
        <TextInput label={t('pipeline.list.client')} placeholder={t('pipeline.list.searchPlaceholder')} value={s.q} onChange={(e) => set({ q: e.currentTarget.value })} />
        <SegmentedControl
          value={s.mine ? 'me' : 'all'}
          onChange={(v) => set({ mine: v === 'me' })}
          data={[
            { value: 'me', label: t('pipeline.list.mine') },
            { value: 'all', label: t('pipeline.list.all') },
          ]}
        />
      </Group>
      <Chip.Group multiple value={s.status} onChange={(v) => set({ status: v as Schemas['JobOrderStatus'][] })}>
        <Group gap="xs" mb="md">
          {statuses.map((x) => (
            <Chip key={x.value} value={x.value} size="sm">
              {label('jobOrder', x.value)}
            </Chip>
          ))}
        </Group>
      </Chip.Group>
      <Stack>
        <DataGrid<JobOrderRow>
          gridId={GRID_ID}
          columns={defs}
          columnsController={columns}
          rows={res.error ? undefined : res.data?.content}
          rowId={(j) => j.id}
          loading={res.loading}
          error={!!res.error}
          onRetry={res.reload}
          sort={sort}
          onSort={(next) => set({ sort: formatSort(next) })}
          exportName="job-orders"
          phone={!!phone}
          renderCard={(j) => <JobCard j={j} />}
          toolbar={
            <>
              <ViewsMenu<JobView>
                store={views}
                current={{ filters: { q: s.q, status: s.status, mine: s.mine }, columns: columns.state, sort }}
                onApply={(v) => {
                  set({ q: v.filters.q ?? '', status: v.filters.status ?? [], mine: v.filters.mine ?? true, sort: v.sort ? formatSort(v.sort) : s.sort });
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
            <div data-testid="list-empty">
              <EmptyState
                title={t('pipeline.list.empty')}
                actions={
                  filtered ? (
                    <Button variant="light" onClick={() => setParams(new URLSearchParams(), { replace: true })}>
                      {t('pipeline.list.clear')}
                    </Button>
                  ) : undefined
                }
              />
            </div>
          }
        />
        {(res.data?.page.totalPages ?? 0) > 1 && <Pagination total={res.data!.page.totalPages} value={s.page + 1} onChange={(p) => set({ page: p - 1 }, false)} />}
      </Stack>
    </Container>
  );
}
