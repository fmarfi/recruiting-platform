import { Anchor, Group, Text } from '@mantine/core';
import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import type { components } from '../../api';
import { StatusBadge } from '../../shell/components/StatusBadge';
import type { ColumnDef } from '../../shell/grid';
import { fmtDate, fmtRelative } from '../../shell/lib/format';

export type Account = components['schemas']['Account'];

/** Column definitions of the accounts grid. Sort keys are the ones /accounts documents. */
export function useAccountColumns(): ColumnDef<Account>[] {
  const { t, i18n } = useTranslation();
  const lng = i18n.language;
  return useMemo<ColumnDef<Account>[]>(
    () => [
      {
        id: 'name',
        header: t('accounts.name'),
        sortable: true,
        locked: true,
        minWidth: 180,
        accessor: (a) => a.name,
        cell: (a) => (
          <Anchor component={Link} to={`/accounts/${a.id}`} fw={600}>
            {a.name}
          </Anchor>
        ),
      },
      { id: 'sector', header: t('accounts.sector'), accessor: (a) => a.sector },
      { id: 'city', header: t('accounts.city'), accessor: (a) => a.city },
      { id: 'owner', header: t('accounts.owner'), accessor: (a) => a.owner.name },
      { id: 'status', header: t('accounts.status'), sortable: true, accessor: (a) => a.status, cell: (a) => <StatusBadge kind="account" value={a.status} size="sm" /> },
      { id: 'openJobCount', header: t('accounts.jobs'), numeric: true, sortable: true, firstSortDir: 'desc', accessor: (a) => a.openJobCount },
      {
        id: 'lastActivityAt',
        header: t('accounts.lastActivity'),
        numeric: true,
        sortable: true,
        firstSortDir: 'desc',
        accessor: (a) => a.lastActivityAt ?? '',
        cell: (a) => <span title={a.lastActivityAt ? fmtDate(a.lastActivityAt, lng) : undefined}>{fmtRelative(a.lastActivityAt, lng)}</span>,
      },
      { id: 'openedOn', header: t('accounts.openedOn'), numeric: true, sortable: true, firstSortDir: 'desc', defaultVisible: false, accessor: (a) => a.openedOn, cell: (a) => fmtDate(a.openedOn, lng) },
      { id: 'website', header: t('accounts.website'), defaultVisible: false, accessor: (a) => a.website },
    ],
    [t, lng],
  );
}

/** Phone layout (UX-09): one card per account. */
export function AccountCard({ a }: { a: Account }) {
  const { t, i18n } = useTranslation();
  return (
    <Anchor component={Link} to={`/accounts/${a.id}`} underline="never" c="inherit">
      <Group justify="space-between" wrap="nowrap">
        <Text fw={600}>{a.name}</Text>
        <StatusBadge kind="account" value={a.status} size="sm" />
      </Group>
      <Text size="sm" c="dimmed">
        {[a.sector, a.city].filter(Boolean).join(' - ')}
      </Text>
      <Text size="sm" c="dimmed">
        {a.owner.name} - {t('accounts.jobsCount', { n: a.openJobCount })} - {fmtRelative(a.lastActivityAt, i18n.language)}
      </Text>
    </Anchor>
  );
}
