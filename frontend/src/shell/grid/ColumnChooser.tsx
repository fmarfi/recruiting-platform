import { ActionIcon, Button, Checkbox, Group, Popover, Stack, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import type { GridColumns } from './useGridColumns';
import type { ColumnDef } from './types';

/** Popover with a checkbox and up/down buttons per column. */
export function ColumnChooser<T>({ defs, columns }: { defs: ColumnDef<T>[]; columns: GridColumns<T> }) {
  const { t } = useTranslation();
  const byId = new Map(defs.map((d) => [d.id, d]));
  const order = columns.state.order;
  return (
    <Popover position="bottom-end" withinPortal trapFocus>
      <Popover.Target>
        <Button variant="default" size="xs">
          {t('grid.columns')}
        </Button>
      </Popover.Target>
      <Popover.Dropdown>
        <Stack gap="xs" role="group" aria-label={t('grid.columns')} style={{ minWidth: 240 }}>
          {order.map((id, i) => {
            const d = byId.get(id);
            if (!d) return null;
            return (
              <Group key={id} justify="space-between" wrap="nowrap" gap="xs">
                <Checkbox
                  label={d.header}
                  checked={columns.state.visible.includes(id)}
                  disabled={d.locked}
                  onChange={() => columns.toggle(id)}
                />
                <Group gap={2} wrap="nowrap">
                  <ActionIcon variant="subtle" size="sm" disabled={i === 0} onClick={() => columns.move(id, -1)} aria-label={t('grid.moveUp', { col: d.header })}>
                    <Text size="xs">{'\u25B2'}</Text>
                  </ActionIcon>
                  <ActionIcon variant="subtle" size="sm" disabled={i === order.length - 1} onClick={() => columns.move(id, 1)} aria-label={t('grid.moveDown', { col: d.header })}>
                    <Text size="xs">{'\u25BC'}</Text>
                  </ActionIcon>
                </Group>
              </Group>
            );
          })}
          <Button variant="subtle" size="xs" onClick={columns.reset}>
            {t('grid.resetColumns')}
          </Button>
        </Stack>
      </Popover.Dropdown>
    </Popover>
  );
}
