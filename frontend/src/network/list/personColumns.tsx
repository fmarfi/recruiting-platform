// Column definitions and phone card of the network grid. Sort keys are the ones /people documents.
import { ActionIcon, Anchor, Group, Stack, Text } from '@mantine/core';
import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { StatusBadge, useFormat, useLabel, type Schemas } from '../../pipeline/shared';
import type { ColumnDef } from '../../shell/grid';

export type Person = Schemas['PersonSummary'];
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

export function usePersonColumns(): ColumnDef<Person>[] {
  const { t } = useTranslation();
  const { label } = useLabel();
  return useMemo<ColumnDef<Person>[]>(
    () => [
      {
        id: 'name',
        header: t('network.fields.name'),
        sortable: true,
        locked: true,
        minWidth: 160,
        accessor: (p) => p.name,
        cell: (p) => (
          <Anchor component={Link} to={`/people/${p.id}`} fw={600}>
            {p.name}
          </Anchor>
        ),
      },
      { id: 'company', header: t('network.fields.titleCompany'), sortable: true, accessor: (p) => [p.title, p.company].filter(Boolean).join(', ') },
      { id: 'type', header: t('network.fields.type'), accessor: (p) => t(`network.types.${p.type}`) },
      { id: 'city', header: t('network.fields.city'), accessor: (p) => p.city },
      { id: 'owner', header: t('network.fields.owner'), accessor: (p) => p.owner.name },
      { id: 'lastContactAt', header: t('network.fields.lastContact'), sortable: true, firstSortDir: 'desc', accessor: (p) => p.lastContactAt?.slice(0, 10) ?? '', cell: (p) => <LastContact at={p.lastContactAt} /> },
      { id: 'consent', header: t('network.fields.consent'), accessor: (p) => label('consent', p.consentStatus), cell: (p) => <StatusBadge lifecycle="consent" value={p.consentStatus} /> },
      {
        id: 'email',
        header: t('network.fields.email'),
        defaultVisible: false,
        accessor: (p) => p.email,
        cell: (p) =>
          p.email ? (
            <ActionIcon component="a" href={`mailto:${p.email}`} variant="subtle" size="sm" aria-label={`${t('network.fields.email')}: ${p.name}`} title={p.email}>
              {'✉'}
            </ActionIcon>
          ) : (
            '-'
          ),
      },
    ],
    [t, label],
  );
}

export function PersonCard({ p }: { p: Person }) {
  const { t } = useTranslation();
  const fmt = useFormat();
  return (
    <Stack gap={2}>
      <Group gap="xs">
        <Anchor component={Link} to={`/people/${p.id}`} fw={600}>
          {p.name}
        </Anchor>
        <Text span size="xs" c="dimmed">
          {t(`network.types.${p.type}`)}
        </Text>
      </Group>
      <Text size="sm">{[p.title, p.company].filter(Boolean).join(', ')}</Text>
      <Text size="xs" c="dimmed">
        {[p.city, `${t('network.fields.lastContact')}: ${p.lastContactAt ? fmt.date(p.lastContactAt) : t('network.never')}`].filter(Boolean).join(' - ')}
      </Text>
      <StatusBadge lifecycle="consent" value={p.consentStatus} />
    </Stack>
  );
}
