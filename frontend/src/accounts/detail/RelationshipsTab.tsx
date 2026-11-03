import { Anchor, Group, Progress, Stack, Table, Text, Title } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import type { components } from '../../api';
import { AddButton } from './AddButton';
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

/** Relationships (A-13): which contacts have gone quiet, and who in our network knows them (warm introductions). Sections are headed, not boxed. */
export function RelationshipsTab({ contacts, connections, onAddContact }: { contacts: Contact[]; connections: Connection[] | undefined; onAddContact?: () => void }) {
  const { t } = useTranslation();
  const cov = coverage(contacts);
  if (cov.total === 0)
    return <SectionEmpty text={t('accounts.relationshipsEmpty')} action={onAddContact && <AddButton label={t('accounts.addContact')} onClick={onAddContact} />} />;
  const pct = Math.round((cov.covered / cov.total) * 100);
  const byContact = new Map<string, Connection[]>();
  (connections ?? []).forEach((c) => byContact.set(c.contact.id, [...(byContact.get(c.contact.id) ?? []), c]));

  return (
    <Stack gap="lg" data-testid="relationships-tab">
      <Stack gap="xs">
        <Group justify="space-between" align="baseline">
          <Text fw={500}>{t('accounts.rel.coverage')}</Text>
          <Text size="sm" style={{ fontVariantNumeric: 'tabular-nums' }}>
            {t('accounts.rel.coverageOf', { covered: cov.covered, total: cov.total })}
          </Text>
        </Group>
        <Progress value={pct} aria-label={t('accounts.rel.coverage')} />
        {cov.gaps > 0 && (
          <Text size="sm" c="red" data-testid="coverage-gap">
            {t('accounts.rel.gap', { count: cov.gaps, days: STALE_DAYS })}
          </Text>
        )}
      </Stack>

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
                  <Anchor component={Link} to={`/people/${c.person.id}`} fw={500} underline="never" c="var(--color-ink)">
                    {c.person.name}
                  </Anchor>
                  {c.primary && (
                    <Text span size="xs" c="dimmed" ml={4}>
                      {t('accounts.primary')}
                    </Text>
                  )}
                  <Text size="xs" c="dimmed">{c.role ?? c.person.title}</Text>
                </Table.Td>
                <Table.Td>{c.person.owner.name}</Table.Td>
                <Table.Td>
                  <Text size="sm" c={stale ? 'red' : undefined} data-stale={stale}>
                    {d === undefined ? t('accounts.rel.never') : t('accounts.rel.daysAgo', { count: d })}
                  </Text>
                </Table.Td>
                <Table.Td style={{ fontVariantNumeric: 'tabular-nums' }}>{byContact.get(c.person.id)?.length ?? 0}</Table.Td>
              </Table.Tr>
            );
          })}
        </Table.Tbody>
      </Table>

      <section>
        <Title order={2} size="h3" mb={4}>
          {t('accounts.rel.warmPaths')}
        </Title>
        <Text size="sm" c="dimmed" mb="sm">{t('accounts.rel.warmHint')}</Text>
        {connections === undefined ? null : byContact.size === 0 ? (
          <Text size="sm" c="dimmed">{t('accounts.rel.noPaths')}</Text>
        ) : (
          <Stack gap="md">
            {cov.rows
              .filter((r) => byContact.has(r.contact.person.id))
              .map(({ contact: c }) => (
                <div key={c.id}>
                  <Text size="sm" fw={500} mb={4}>{c.person.name}</Text>
                  <Stack gap={4}>
                    {byContact.get(c.person.id)!.map((x, i) => (
                      <Group key={i} gap="xs">
                        <Anchor component={Link} to={`/people/${x.person.id}`} size="sm" underline="hover" c="var(--color-ink)">
                          {x.person.name}
                        </Anchor>
                        <Text span size="sm" c="dimmed">
                          {t(`network.linkTypes.${x.linkType}`)}
                        </Text>
                      </Group>
                    ))}
                  </Stack>
                </div>
              ))}
          </Stack>
        )}
      </section>
    </Stack>
  );
}
