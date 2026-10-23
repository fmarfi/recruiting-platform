// Network list rows: table on desktop, stacked cards on phone. Last contact older than 90 days is shown in the warn colour (A-12).
import { ActionIcon, Anchor, Card, Checkbox, Group, Stack, Table, Text, UnstyledButton } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { StatusBadge, useFormat, type Schemas } from '../../pipeline/shared';

type Person = Schemas['PersonSummary'];
const STALE_MS = 90 * 86_400_000;

function LastContact({ at }: { at?: string | null }) {
  const { t } = useTranslation();
  const fmt = useFormat();
  if (!at) return <Text size="sm" c="dimmed">{t('network.never')}</Text>;
  const stale = Date.now() - new Date(at).getTime() > STALE_MS;
  return (
    <Text size="sm" data-stale={stale} style={stale ? { color: 'var(--tone-warn-fg)' } : undefined}>
      {fmt.dateLong(at)}
    </Text>
  );
}

export type Selection = { selected: Set<string>; onToggle: (id: string) => void; onToggleAll: (ids: string[], on: boolean) => void };
export type SortState = { sort: string; onSort: (field: string) => void };

function SortHead({ field, label, sort }: { field: string; label: string; sort: SortState }) {
  const active = sort.sort.startsWith(`${field},`);
  return (
    <Table.Th aria-sort={active ? (sort.sort.endsWith('asc') ? 'ascending' : 'descending') : 'none'}>
      <UnstyledButton onClick={() => sort.onSort(field)} fw={600} fz="inherit">
        {label} {active ? (sort.sort.endsWith('asc') ? '▲' : '▼') : ''}
      </UnstyledButton>
    </Table.Th>
  );
}

export function PersonTable({ people, selection, sort }: { people: Person[]; selection?: Selection; sort?: SortState }) {
  const { t } = useTranslation();
  const ids = people.map((p) => p.id);
  const all = !!selection && ids.length > 0 && ids.every((id) => selection.selected.has(id));
  const some = !!selection && ids.some((id) => selection.selected.has(id));
  const head = (field: string, label: string) => (sort ? <SortHead field={field} label={label} sort={sort} /> : <Table.Th>{label}</Table.Th>);
  return (
    <Table highlightOnHover>
      <Table.Thead>
        <Table.Tr>
          {selection && (
            <Table.Th w={36}>
              <Checkbox aria-label={t('network.selectAll')} checked={all} indeterminate={some && !all} onChange={(e) => selection.onToggleAll(ids, e.currentTarget.checked)} />
            </Table.Th>
          )}
          {head('name', t('network.fields.name'))}
          {head('company', t('network.fields.titleCompany'))}
          <Table.Th>{t('network.fields.type')}</Table.Th>
          <Table.Th>{t('network.fields.city')}</Table.Th>
          <Table.Th>{t('network.fields.owner')}</Table.Th>
          {head('lastContactAt', t('network.fields.lastContact'))}
          <Table.Th>{t('network.fields.consent')}</Table.Th>
          <Table.Th w={64} />
        </Table.Tr>
      </Table.Thead>
      <Table.Tbody>
        {people.map((p) => (
          <Table.Tr key={p.id} bg={selection?.selected.has(p.id) ? 'var(--mantine-primary-color-light)' : undefined}>
            {selection && (
              <Table.Td>
                <Checkbox aria-label={p.name} checked={selection.selected.has(p.id)} onChange={() => selection.onToggle(p.id)} />
              </Table.Td>
            )}
            <Table.Td>
              <Anchor component={Link} to={`/people/${p.id}`} fw={600}>
                {p.name}
              </Anchor>
            </Table.Td>
            <Table.Td>{[p.title, p.company].filter(Boolean).join(', ')}</Table.Td>
            <Table.Td>{t(`network.types.${p.type}`)}</Table.Td>
            <Table.Td>{p.city}</Table.Td>
            <Table.Td>{p.owner.name}</Table.Td>
            <Table.Td>
              <LastContact at={p.lastContactAt} />
            </Table.Td>
            <Table.Td>
              <StatusBadge lifecycle="consent" value={p.consentStatus} />
            </Table.Td>
            <Table.Td>
              {p.email && (
                <ActionIcon component="a" href={`mailto:${p.email}`} variant="subtle" size="sm" aria-label={`${t('network.fields.email')}: ${p.name}`} title={p.email}>
                  ✉
                </ActionIcon>
              )}
            </Table.Td>
          </Table.Tr>
        ))}
      </Table.Tbody>
    </Table>
  );
}

export function PersonCards({ people, selection }: { people: Person[]; selection?: Selection }) {
  const { t } = useTranslation();
  const fmt = useFormat();
  return (
    <Stack gap="xs">
      {people.map((p) => (
        <Card key={p.id} withBorder padding="sm">
          <Group gap="xs" wrap="nowrap" align="flex-start">
            {selection && <Checkbox aria-label={p.name} mt={3} checked={selection.selected.has(p.id)} onChange={() => selection.onToggle(p.id)} />}
            <div>
              <Anchor component={Link} to={`/people/${p.id}`} fw={600}>
                {p.name}
              </Anchor>{' '}
              <Text span size="xs" c="dimmed">
                {t(`network.types.${p.type}`)}
              </Text>
              <Text size="sm">{[p.title, p.company].filter(Boolean).join(', ')}</Text>
              <Text size="xs" c="dimmed">
                {[p.city, `${t('network.fields.lastContact')}: ${p.lastContactAt ? fmt.date(p.lastContactAt) : t('network.never')}`].filter(Boolean).join(' - ')}
              </Text>
              <StatusBadge lifecycle="consent" value={p.consentStatus} />
            </div>
          </Group>
        </Card>
      ))}
    </Stack>
  );
}
