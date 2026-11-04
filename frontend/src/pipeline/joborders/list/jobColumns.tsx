// Column definitions and phone card of the job orders grid.
import { Anchor, Group, Stack, Text } from '@mantine/core';
import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import type { ColumnDef } from '../../../shell/grid';
import { StatusBadge, useLabel, type Schemas } from '../../shared';

export type JobOrderRow = Schemas['JobOrder'];

export function useJobColumns(): ColumnDef<JobOrderRow>[] {
  const { t } = useTranslation();
  const { label } = useLabel();
  return useMemo<ColumnDef<JobOrderRow>[]>(
    () => [
      {
        id: 'title',
        header: t('pipeline.list.title'),
        sortable: true,
        locked: true,
        minWidth: 180,
        accessor: (j) => j.title,
        cell: (j) => (
          <Anchor component={Link} to={`/job-orders/${j.id}`} fw={500} c="var(--color-ink)" underline="never">
            {j.title}
          </Anchor>
        ),
      },
      { id: 'client', header: t('pipeline.list.client'), accessor: (j) => j.account.name },
      { id: 'status', header: t('pipeline.list.status'), sortable: true, accessor: (j) => label('jobOrder', j.status), cell: (j) => <StatusBadge lifecycle="jobOrder" value={j.status} /> },
      { id: 'owner', header: t('pipeline.owner'), accessor: (j) => j.owner.name },
      { id: 'daysOpen', header: t('pipeline.list.daysOpen'), numeric: true, sortable: true, firstSortDir: 'desc', accessor: (j) => j.daysOpen },
      { id: 'openings', header: t('pipeline.list.openings'), numeric: true, accessor: (j) => j.openings },
      { id: 'activeCandidates', header: t('pipeline.list.active'), numeric: true, accessor: (j) => j.activeCandidates },
    ],
    [t, label],
  );
}

export function JobCard({ j }: { j: JobOrderRow }) {
  const { t } = useTranslation();
  return (
    <Stack gap={2}>
      <Group justify="space-between" wrap="nowrap">
        <Anchor component={Link} to={`/job-orders/${j.id}`} fw={500} c="var(--color-ink)" underline="never">
          {j.title}
        </Anchor>
        <StatusBadge lifecycle="jobOrder" value={j.status} />
      </Group>
      <Text size="sm" c="dimmed">
        {j.account.name} - {j.owner.name}
      </Text>
      <Text size="xs" c="dimmed">
        {t('pipeline.list.daysOpen')}: {j.daysOpen} - {t('pipeline.list.active')}: {j.activeCandidates ?? 0}
      </Text>
    </Stack>
  );
}

