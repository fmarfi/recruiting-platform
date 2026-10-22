// Network list rows: table on desktop, stacked cards on phone. Last contact older than 90 days is shown in the warn colour (A-12).
import { Anchor, Card, Stack, Table, Text } from '@mantine/core';
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

export function PersonTable({ people, compact = false }: { people: Person[]; compact?: boolean }) {
  const { t } = useTranslation();
  return (
    <Table highlightOnHover verticalSpacing={compact ? 4 : 'sm'}>
      <Table.Thead>
        <Table.Tr>
          <Table.Th>{t('network.fields.name')}</Table.Th>
          <Table.Th>{t('network.fields.titleCompany')}</Table.Th>
          <Table.Th>{t('network.fields.type')}</Table.Th>
          <Table.Th>{t('network.fields.city')}</Table.Th>
          <Table.Th>{t('network.fields.owner')}</Table.Th>
          <Table.Th>{t('network.fields.lastContact')}</Table.Th>
          <Table.Th>{t('network.fields.consent')}</Table.Th>
        </Table.Tr>
      </Table.Thead>
      <Table.Tbody>
        {people.map((p) => (
          <Table.Tr key={p.id}>
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
          </Table.Tr>
        ))}
      </Table.Tbody>
    </Table>
  );
}

export function PersonCards({ people }: { people: Person[] }) {
  const { t } = useTranslation();
  const fmt = useFormat();
  return (
    <Stack gap="xs">
      {people.map((p) => (
        <Card key={p.id} withBorder padding="sm">
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
        </Card>
      ))}
    </Stack>
  );
}
