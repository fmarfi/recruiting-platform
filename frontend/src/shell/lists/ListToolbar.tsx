// List-view toolbar (S3): view selector, search, filter panel, column chooser, density toggle, count line.
import { Badge, Box, Button, Group, Popover, SegmentedControl, Text, TextInput } from '@mantine/core';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { ColumnChooser, type ColumnOption } from './ColumnChooser';
import type { Density } from './useListPrefs';
import { ViewSelector, type ViewOption } from './ViewSelector';

interface Props {
  views: ViewOption[];
  view: string;
  onView: (value: string) => void;
  onSaveView: (name: string) => void;
  onDeleteView: (value: string) => void;
  search: string;
  onSearch: (q: string) => void;
  searchLabel: string;
  /** Controls inside the filter popover, for example status chips. */
  filterPanel: ReactNode;
  activeFilters: number;
  onClear: () => void;
  columns?: { options: ColumnOption[]; visible: string[]; onChange: (v: string[]) => void };
  density: Density;
  onDensity: (d: Density) => void;
  /** Count line under the toolbar, for example "42 items". */
  summary?: string;
}

/** One toolbar for every list so Accounts and Network look and behave the same. */
export function ListToolbar(p: Props) {
  const { t } = useTranslation();
  return (
    <Box>
      <Group gap="sm" wrap="wrap">
        <ViewSelector options={p.views} value={p.view} onSelect={p.onView} onSave={p.onSaveView} onDelete={p.onDeleteView} />
        <TextInput
          placeholder={p.searchLabel}
          aria-label={p.searchLabel}
          value={p.search}
          onChange={(e) => p.onSearch(e.currentTarget.value)}
          style={{ flex: 1, minWidth: 180 }}
        />
        <Popover position="bottom-end" withinPortal width={320}>
          <Popover.Target>
            <Button
              variant="default"
              size="sm"
              rightSection={p.activeFilters > 0 ? <Badge size="sm" circle>{p.activeFilters}</Badge> : undefined}
            >
              {t('shell.filters')}
            </Button>
          </Popover.Target>
          <Popover.Dropdown>
            {p.filterPanel}
            {p.activeFilters > 0 && (
              <Button mt="sm" variant="subtle" size="compact-sm" onClick={p.onClear}>
                {t('accounts.clear')}
              </Button>
            )}
          </Popover.Dropdown>
        </Popover>
        {p.columns && <ColumnChooser {...p.columns} />}
        <SegmentedControl
          size="xs"
          aria-label={t('shell.density')}
          value={p.density}
          onChange={(v) => p.onDensity(v as Density)}
          data={[
            { value: 'comfortable', label: t('shell.comfortable') },
            { value: 'compact', label: t('shell.compact') },
          ]}
        />
      </Group>
      {p.summary && <Text fz="xs" c="dimmed" mt="xs">{p.summary}</Text>}
    </Box>
  );
}
