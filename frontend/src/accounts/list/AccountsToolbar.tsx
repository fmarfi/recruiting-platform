// Accounts list toolbar: built-in and saved views, name search, status filter panel, columns, density (A-01, S3).
import { Chip, Group, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { ListToolbar } from '../../shell/lists/ListToolbar';
import { useDensity, useSavedViews } from '../../shell/lists/useListPrefs';
import type { ViewOption } from '../../shell/lists/ViewSelector';
import { useReference } from '../../shell/refdata/useReference';
import type { AccountFilters } from './useAccounts';

/** Table columns the user can hide; the name column is always shown. */
export const ACCOUNT_COLUMNS = ['sector', 'city', 'owner', 'status', 'jobs', 'lastActivity'];

type SavedFilters = Pick<AccountFilters, 'q' | 'status' | 'ownerId'>;

interface Props {
  filters: AccountFilters;
  meId: string;
  total?: number;
  onChange: (patch: Partial<AccountFilters>) => void;
  onClear: () => void;
  visibleColumns: string[];
  onColumns: (cols: string[]) => void;
  density: ReturnType<typeof useDensity>;
}

/** Wires the shared list toolbar to the account filters. */
export function AccountsToolbar({ filters, meId, total, onChange, onClear, visibleColumns, onColumns, density }: Props) {
  const { t, i18n } = useTranslation();
  const ref = useReference();
  const saved = useSavedViews<SavedFilters>('accounts');
  const statuses = ref.data?.lifecycles.account.statuses ?? [];

  const views: ViewOption[] = [
    { value: 'mine', label: t('accounts.myClients') },
    { value: 'all', label: t('accounts.allClients') },
    ...saved.views.map((v) => ({ value: `saved:${v.name}`, label: v.name, custom: true })),
  ];

  // The selected view is whichever one the current filters equal, otherwise none.
  const same = (f: SavedFilters) => f.q === filters.q && f.status === filters.status && f.ownerId === filters.ownerId;
  const current =
    saved.views.find((v) => same(v.filters)) ? `saved:${saved.views.find((v) => same(v.filters))!.name}`
    : same({ q: '', status: '', ownerId: meId }) ? 'mine'
    : same({ q: '', status: '', ownerId: '' }) ? 'all'
    : '';

  const selectView = (value: string) => {
    if (value === 'mine') onChange({ q: '', status: '', ownerId: meId });
    else if (value === 'all') onChange({ q: '', status: '', ownerId: '' });
    else {
      const v = saved.views.find((s) => `saved:${s.name}` === value);
      if (v) onChange(v.filters);
    }
  };

  const columnLabel: Record<string, string> = {
    sector: t('accounts.sector'), city: t('accounts.city'), owner: t('accounts.owner'),
    status: t('accounts.status'), jobs: t('accounts.jobs'), lastActivity: t('accounts.lastActivity'),
  };

  return (
    <ListToolbar
      views={views}
      view={current}
      onView={selectView}
      onSaveView={(name) => saved.save(name, { q: filters.q, status: filters.status, ownerId: filters.ownerId })}
      onDeleteView={(value) => saved.remove(value.replace('saved:', ''))}
      search={filters.q}
      onSearch={(q) => onChange({ q })}
      searchLabel={t('accounts.filterName')}
      activeFilters={(filters.status ? 1 : 0) + (filters.ownerId && filters.ownerId !== meId ? 1 : 0)}
      onClear={onClear}
      filterPanel={
        <>
          <Text fz="xs" c="dimmed" mb="xs">{t('accounts.status')}</Text>
          <Chip.Group multiple={false} value={filters.status || 'ALL'} onChange={(v) => onChange({ status: v === 'ALL' ? '' : v })}>
            <Group gap="xs">
              <Chip value="ALL">{t('accounts.all')}</Chip>
              {statuses.map((s) => (
                <Chip key={s.value} value={s.value}>{i18n.language.startsWith('tr') ? s.labelTr : s.labelEn}</Chip>
              ))}
            </Group>
          </Chip.Group>
        </>
      }
      columns={{
        options: ACCOUNT_COLUMNS.map((key) => ({ key, label: columnLabel[key] ?? key })),
        visible: visibleColumns,
        onChange: onColumns,
      }}
      density={density[0]}
      onDensity={density[1]}
      summary={total === undefined ? undefined : t('shell.items', { n: total })}
    />
  );
}
