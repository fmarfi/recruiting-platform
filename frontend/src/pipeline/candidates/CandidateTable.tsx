// Search page table (M2-UI1): every candidate of the search in one dense DataGrid. Stage is a column and is changed from the row.
// Replaces the kanban board on the page (the board files stay in the code base, unused).
import { ActionIcon, Anchor, Chip, Group, Select, Text, TextInput } from '@mantine/core';
import { useMediaQuery } from '@mantine/hooks';
import { IconNotes, IconSearch, IconShieldCheck } from '@tabler/icons-react';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { EmptyState } from '../../shell/components/EmptyState';
import { ToneBadge } from '../../shell/components/ToneBadge';
import { DataGrid } from '../../shell/grid';
import type { ColumnDef, SortState } from '../../shell/grid/types';
import { tokens } from '../../theme/tokens';
import type { Stage } from '../board/boardRules';
import type { App } from '../longlist/useLonglist';
import { useFormat, useLabel } from '../shared';
import { countByStage, filterByStage, sortApps, STAGE_ORDER } from './candidateRows';
import { StageSelect } from './StageSelect';

const CONSENT_PROBLEM = ['NOT_SENT', 'REFUSED', 'WITHDRAWN'];

export function CandidateTable({
  apps,
  readOnly,
  onStage,
  onRate,
  onLog,
}: {
  apps: App[];
  readOnly: boolean;
  onStage: (app: App, to: Stage) => void;
  onRate: (app: App, rating: number | null) => void;
  onLog: (app: App) => void;
}) {
  const { t } = useTranslation();
  const { label, def } = useLabel();
  const fmt = useFormat();
  const phone = useMediaQuery(`(max-width: ${tokens.breakpoints.phone}px)`);
  const [stages, setStages] = useState<string[]>([]);
  const [q, setQ] = useState('');
  const [sort, setSort] = useState<SortState | null>(null);
  const counts = useMemo(() => countByStage(apps), [apps]);
  const rows = useMemo(() => {
    const needle = q.trim().toLocaleLowerCase();
    const byName = needle ? apps.filter((a) => `${a.person.name} ${a.person.company ?? ''}`.toLocaleLowerCase().includes(needle)) : apps;
    return sortApps(filterByStage(byName, stages), sort);
  }, [apps, stages, q, sort]);

  const columns: ColumnDef<App>[] = [
    {
      id: 'name',
      header: t('pipeline.ll.name'),
      sortable: true,
      locked: true,
      minWidth: 160,
      accessor: (a) => a.person.name,
      cell: (a) => (
        <Anchor component={Link} to={`/people/${a.person.id}`} size="sm" c="var(--color-navy)">
          {a.person.name}
        </Anchor>
      ),
    },
    {
      id: 'role',
      header: t('pipeline.ll.roleCompany'),
      sortable: true,
      minWidth: 200,
      accessor: (a) => [a.person.title, a.person.company].filter(Boolean).join(', '),
      cell: (a) => (
        <Text size="sm" c="var(--color-ink2)">
          {[a.person.title, a.person.company].filter(Boolean).join(', ') || '–'}
        </Text>
      ),
    },
    {
      id: 'stage',
      header: t('pipeline.cand.stage'),
      sortable: true,
      minWidth: 170,
      accessor: (a) => label('pipelineStage', a.stage),
      cell: (a) => <StageSelect app={a} readOnly={readOnly} onChange={onStage} />,
    },
    {
      id: 'list',
      header: t('pipeline.cand.list'),
      sortable: true,
      accessor: (a) => (a.list ? t(`pipeline.cand.lists.${a.list}`) : ''),
    },
    {
      id: 'rating',
      header: t('pipeline.ll.rating'),
      sortable: true,
      minWidth: 90,
      accessor: (a) => a.rating ?? '',
      cell: (a) => (
        <Select
          size="xs"
          w="4.5rem"
          aria-label={t('pipeline.cand.ratingOf', { name: a.person.name })}
          data={['1', '2', '3', '4', '5']}
          value={a.rating ? String(a.rating) : null}
          placeholder="–"
          clearable
          disabled={readOnly}
          onChange={(v) => onRate(a, v ? Number(v) : null)}
        />
      ),
    },
    {
      id: 'consent',
      header: t('pipeline.consentLabel'),
      accessor: (a) => label('consent', a.person.consentStatus),
      cell: (a) => (
        <ToneBadge
          tone={def('consent', a.person.consentStatus)?.tone ?? 'NEUTRAL'}
          label={label('consent', a.person.consentStatus)}
          icon={CONSENT_PROBLEM.includes(a.person.consentStatus) ? <IconShieldCheck size={14} stroke={1.75} aria-hidden /> : undefined}
        />
      ),
    },
    {
      id: 'lastActivity',
      header: t('pipeline.cand.lastActivity'),
      sortable: true,
      firstSortDir: 'desc',
      accessor: (a) => fmt.date(a.person.lastContactAt),
    },
    {
      id: 'log',
      header: t('pipeline.cand.log'),
      minWidth: 60,
      cell: (a) => (
        <ActionIcon variant="subtle" size="md" aria-label={t('pipeline.logWithPerson') + ': ' + a.person.name} onClick={() => onLog(a)}>
          <IconNotes size={16} stroke={1.75} aria-hidden />
        </ActionIcon>
      ),
    },
  ];

  return (
    <DataGrid<App>
      gridId="search-candidates"
      columns={columns}
      rows={apps.length === 0 ? [] : rows}
      rowId={(a) => a.id}
      sort={sort}
      onSort={setSort}
      exportName="candidates"
      phone={!!phone}
      renderCard={(a) => (
        <Group justify="space-between" wrap="nowrap" align="flex-start">
          <div style={{ minWidth: 0 }}>
            <Anchor component={Link} to={`/people/${a.person.id}`} size="sm" fw={500} c="var(--color-navy)">
              {a.person.name}
            </Anchor>
            <Text size="xs" c="dimmed" truncate>
              {[a.person.title, a.person.company].filter(Boolean).join(', ')}
            </Text>
          </div>
          <StageSelect app={a} readOnly={readOnly} onChange={onStage} />
        </Group>
      )}
      toolbar={
        <Group gap="sm" wrap="wrap">
          <TextInput
            size="xs"
            aria-label={t('pipeline.cand.filterName')}
            placeholder={t('pipeline.cand.filterName')}
            leftSection={<IconSearch size={14} stroke={1.75} aria-hidden />}
            value={q}
            onChange={(e) => setQ(e.currentTarget.value)}
          />
          <Chip.Group multiple value={stages} onChange={setStages}>
            <Group gap={6} role="group" aria-label={t('pipeline.cand.filterStage')}>
              {STAGE_ORDER.filter((s) => counts[s]).map((s) => (
                <Chip key={s} value={s} size="xs" radius="sm">
                  {label('pipelineStage', s)} {counts[s]}
                </Chip>
              ))}
            </Group>
          </Chip.Group>
        </Group>
      }
      empty={<EmptyState title={t('pipeline.cand.noMatchTitle')} hint={t('pipeline.cand.noMatchHint')} />}
    />
  );
}
