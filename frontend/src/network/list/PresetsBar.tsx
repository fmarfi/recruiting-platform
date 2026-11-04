// Smart lists: one-click presets, square-cornered chips (visual system v3, section 6). Saved views live in the grid's ViewsMenu.
import { Button, Group, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { PRESETS, sameFilters, type ViewFilters } from './networkFilters';

export function PresetsBar({ current, onApply }: { current: ViewFilters; onApply: (f: ViewFilters) => void }) {
  const { t } = useTranslation();
  return (
    <Group gap="xs" mb="sm" data-testid="saved-views" style={{ overflowX: 'auto' }} wrap="nowrap">
      <Text size="xs" c="var(--color-ink3)" fw={500}>
        {t('network.views')}
      </Text>
      {PRESETS.map((p) => (
        <Button key={p.id} size="compact-sm" radius="sm" variant={sameFilters(current, p.filters) ? 'light' : 'default'} onClick={() => onApply(p.filters)}>
          {t(`network.presets.${p.id}`)}
        </Button>
      ))}
    </Group>
  );
}
