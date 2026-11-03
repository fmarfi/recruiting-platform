import { Alert, Anchor, Badge, Button, Card, Group, Progress, Stack, Table, Text, Title } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import type { components } from '../../api';
import { SectionEmpty } from './SectionEmpty';

type Contact = components['schemas']['Contact'];
type Connection = components['schemas']['AccountConnection'];

const DAY = 86_400_000;
export const STALE_DAYS = 90;

const days = (iso?: string | null, now = Date.now()) => (iso ? Math.max(0, Math.floor((now - new Date(iso).getTime()) / DAY)) : undefined);

/** Coverage = share of active contacts we spoke to within STALE_DAYS; stalest first so the gaps are on top. */
export function coverage(contacts: Contact[], now = Date.now()) {
  const rows = contacts
    .filter((c) => c.active)
    .map((c) => ({ contact: c, days: days(c.person.lastContactAt, now) }))
    .sort((a, b) => (b.days ?? Infinity) - (a.days ?? Infinity));
  const covered = rows.filter((r) => r.days !== undefined && r.days <= STALE_DAYS).length;
  return { rows, covered, total: rows.length, gaps: rows.length - covered };
}

/** Relationships (A-13): which contacts have gone quiet, and who in our network knows them (warm introductions). */
export function RelationshipsTab({ contacts, connections, onAddContact }: { contacts: Contact[]; connections: Connection[] | undefined; onAddContact?: () => void }) {
  const { t } = useTranslation();
  const cov = coverage(contacts);
  if (cov.total === 0)
    return <SectionEmpty text={t('accounts.relationshipsEmpty')} action={onAddContact && <Button size="compact-sm" variant="light" onClick={onAddContact}>+ {t('accounts.addContact')}</Button>} />;
  const pct = Math.round((cov.covered / cov.total) * 100);
  const byContact = new Map<string, Connection[]>();
  (connections ?? []).forEach((c) => byContact.set(c.contact.id, [...(byContact.get(c.contact.id) ?? []), c]));

  return (
    <Stack gap="md" data-testid="relationships-tab">
      <Card withBorder padding="sm">
        <Group justify="space-between" mb={6}>
          <Text fw={600}>{t('accounts.rel.coverage')}</Text>
          <Text size="sm">{t('accounts.rel.coverageOf', { covered: cov.covered, total: cov.total })}</Text>
        </Group>
        <Progress value={pct} color={pct >= 70 ? 'green' : pct >= 40 ? 'yellow' : 'red'} aria-label={t('accounts.rel.coverage')} />
        {cov.gaps > 0 && (
          <Alert color="yellow" variant="light" mt="sm" data-testid="coverage-gap">
            {t('accounts.rel.gap', { count: cov.gaps, days: STALE_DAYS })}
          </Alert>
        )}
      </Card>

      <Table highlightOnHover>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>{t('accounts.rel.contact')}</Table.Th>
            <Table.Th>{t('accounts.owner')}</Table.Th>
            <Table.Th>{t('accounts.rel.lastContact')}</Table.Th>
            <Table.Th>{t('accounts.rel.knownBy')}</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {cov.rows.map(({ contact: c, days: d }) => {
            const stale = d === undefined || d > STALE_DAYS;
            return (
              <Table.Tr key={c.id}>
                <Table.Td>
                  <Anchor component={Link} to={`/people/${c.person.id}`} fw={600}>
                    {c.person.name}
                  </Anchor>{' '}
                  {c.primary && <Badge size="xs">{t('accounts.primary')}</Badge>}
                  <Text size="xs" c="dimmed">{c.role ?? c.person.title}</Text>
                </Table.Td>
                <Table.Td>{c.person.owner.name}</Table.Td>
                <Table.Td>
                  <Text size="sm" c={stale ? 'red' : undefined} data-stale={stale}>
                    {d === undefined ? t('accounts.rel.never') : t('accounts.rel.daysAgo', { count: d })}
                  </Text>
                </Table.Td>
                <Table.Td>{byContact.get(c.person.id)?.length ?? 0}</Table.Td>
              </Table.Tr>
            );
          })}
        </Table.Tbody>
      </Table>

      <section>
        <Title order={3} size="h5" mb={4}>
          {t('accounts.rel.warmPaths')}
        </Title>
        <Text size="sm" c="dimmed" mb="xs">{t('accounts.rel.warmHint')}</Text>
        {connections === undefined ? null : byContact.size === 0 ? (
          <Text size="sm" c="dimmed">{t('accounts.rel.noPaths')}</Text>
        ) : (
          <Stack gap="xs">
            {cov.rows
              .filter((r) => byContact.has(r.contact.person.id))
              .map(({ contact: c }) => (
                <Card key={c.id} withBorder padding="sm">
                  <Text size="sm" fw={600} mb={4}>{c.person.name}</Text>
                  <Group gap="xs">
                    {byContact.get(c.person.id)!.map((x, i) => (
                      <Badge key={i} variant="light" size="lg" component={Link} to={`/people/${x.person.id}`} style={{ cursor: 'pointer', textTransform: 'none' }}>
                        {x.person.name} · {t(`network.linkTypes.${x.linkType}`)}
                      </Badge>
                    ))}
                  </Group>
                </Card>
              ))}
          </Stack>
        )}
      </section>
    </Stack>
  );
}
