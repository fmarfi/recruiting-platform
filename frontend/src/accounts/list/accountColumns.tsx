import { Anchor, Group, Text } from '@mantine/core';
import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import type { components } from '../../api';
import { StatusBadge } from '../../shell/components/StatusBadge';
import type { ColumnDef } from '../../shell/grid';
import { fmtDate, fmtRelative } from '../../shell/lib/format';

export type Account = components['schemas']['Account'];

/** Affordance that the name opens the record: a small chevron in the token ink colour, decorative only. */
function OpenChevron() {
  return (
    <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden focusable="false" style={{ color: 'var(--color-ink3)', flexShrink: 0 }}>
      <path d="M5 3l4 4-4 4" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

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
          <Anchor component={Link} to={`/accounts/${a.id}`} fw={600} underline="hover" title={t('accounts.openRecord')}>
            <Group component="span" gap="xs" wrap="nowrap" display="inline-flex" align="center">
              {a.name}
              <OpenChevron />
            </Group>
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
        cell: (a) => (
          <Text component="span" size="sm" c="dimmed" title={a.lastActivityAt ? fmtDate(a.lastActivityAt, lng) : undefined}>
            {fmtRelative(a.lastActivityAt, lng)}
          </Text>
        ),
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
