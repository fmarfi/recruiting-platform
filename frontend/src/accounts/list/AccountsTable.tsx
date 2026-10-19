import { Anchor, Card, Group, Stack, Table, Text, UnstyledButton } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import type { components } from '../../api';
import { StatusBadge } from '../../shell/components/StatusBadge';
import { fmtDate, fmtRelative } from '../../shell/lib/format';

type Account = components['schemas']['Account'];

interface Props {
  rows: Account[];
  sort: string;
  onSort: (field: string) => void;
  phone: boolean;
  /** Column keys to show (name is always shown). */
  columns: string[];
  compact: boolean;
}

/** Table on laptop, cards on phone (UX-09). */
export function AccountsTable({ rows, sort, onSort, phone, columns, compact }: Props) {
  const { t, i18n } = useTranslation();
  const [field, dir] = sort.split(',');
  const show = (c: string) => columns.includes(c);
  const head = (f: string, label: string) => (
    <UnstyledButton onClick={() => onSort(f)} fw={600} aria-label={t('accounts.sortBy', { col: label })}>
      {label} {field === f ? (dir === 'asc' ? '▲' : '▼') : ''}
    </UnstyledButton>
  );

  if (phone)
    return (
      <Stack gap="xs">
        {rows.map((a) => (
          <Card key={a.id} withBorder component={Link} to={`/accounts/${a.id}`} style={{ textDecoration: 'none', color: 'inherit' }}>
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
          </Card>
        ))}
      </Stack>
    );

  return (
    <Table highlightOnHover withTableBorder verticalSpacing={compact ? 4 : 'sm'} stickyHeader>
      <Table.Thead>
        <Table.Tr>
          <Table.Th>{head('name', t('accounts.name'))}</Table.Th>
          {show('sector') && <Table.Th>{t('accounts.sector')}</Table.Th>}
          {show('city') && <Table.Th>{t('accounts.city')}</Table.Th>}
          {show('owner') && <Table.Th>{t('accounts.owner')}</Table.Th>}
          {show('status') && <Table.Th>{head('status', t('accounts.status'))}</Table.Th>}
          {show('jobs') && <Table.Th>{head('openJobCount', t('accounts.jobs'))}</Table.Th>}
          {show('lastActivity') && <Table.Th>{head('lastActivityAt', t('accounts.lastActivity'))}</Table.Th>}
        </Table.Tr>
      </Table.Thead>
      <Table.Tbody>
        {rows.map((a) => (
          <Table.Tr key={a.id}>
            <Table.Td>
              <Anchor component={Link} to={`/accounts/${a.id}`} fw={600}>
                {a.name}
              </Anchor>
            </Table.Td>
            {show('sector') && <Table.Td>{a.sector}</Table.Td>}
            {show('city') && <Table.Td>{a.city ?? '-'}</Table.Td>}
            {show('owner') && <Table.Td>{a.owner.name}</Table.Td>}
            {show('status') && (
              <Table.Td>
                <StatusBadge kind="account" value={a.status} size="sm" />
              </Table.Td>
            )}
            {show('jobs') && <Table.Td>{a.openJobCount}</Table.Td>}
            {show('lastActivity') && (
              <Table.Td title={a.lastActivityAt ? fmtDate(a.lastActivityAt, i18n.language) : undefined}>{fmtRelative(a.lastActivityAt, i18n.language)}</Table.Td>
            )}
          </Table.Tr>
        ))}
      </Table.Tbody>
    </Table>
  );
}
