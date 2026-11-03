// Smart lists: one-click presets. Saved views live in the grid's ViewsMenu.
import { Button, Group, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { PRESETS, sameFilters, type ViewFilters } from './networkFilters';

export function PresetsBar({ current, onApply }: { current: ViewFilters; onApply: (f: ViewFilters) => void }) {
  const { t } = useTranslation();
  return (
    <Group gap="xs" mb="sm" data-testid="saved-views" style={{ overflowX: 'auto' }} wrap="nowrap">
      <Text size="sm" c="dimmed" fw={600}>
        {t('network.views')}
      </Text>
      {PRESETS.map((p) => (
        <Button key={p.id} size="compact-sm" radius="xl" variant={sameFilters(current, p.filters) ? 'filled' : 'light'} onClick={() => onApply(p.filters)}>
          {t(`network.presets.${p.id}`)}
        </Button>
      ))}
    </Group>
  );
}
