// Smart lists: one-click presets, plus views the consultant saves (kept in this browser).
import { Button, CloseButton, Group, Text, TextInput } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { PRESETS, sameFilters, type SavedView, type ViewFilters } from './savedViews';

export function SavedViewsBar({
  current,
  views,
  onApply,
  onSave,
  onDelete,
}: {
  current: ViewFilters;
  views: SavedView[];
  onApply: (f: ViewFilters) => void;
  onSave: (name: string) => void;
  onDelete: (id: string) => void;
}) {
  const { t } = useTranslation();
  const [naming, setNaming] = useState(false);
  const [name, setName] = useState('');
  const submit = () => {
    if (!name.trim()) return;
    onSave(name.trim());
    setName('');
    setNaming(false);
  };
  const btn = (key: string, label: string, f: ViewFilters, extra?: React.ReactNode) => (
    <Group key={key} gap={2} wrap="nowrap">
      <Button size="compact-sm" radius="xl" variant={sameFilters(current, f) ? 'filled' : 'light'} onClick={() => onApply(f)}>
        {label}
      </Button>
      {extra}
    </Group>
  );
  return (
    <Group gap="xs" mb="sm" data-testid="saved-views" style={{ overflowX: 'auto' }} wrap="nowrap">
      <Text size="xs" c="dimmed" fw={600} tt="uppercase">
        {t('network.views')}
      </Text>
      {PRESETS.map((p) => btn(p.id, t(`network.presets.${p.id}`), p.filters))}
      {views.map((v) => btn(v.id, v.name, v.filters, <CloseButton size="xs" aria-label={t('network.deleteView', { name: v.name })} onClick={() => onDelete(v.id)} />))}
      {naming ? (
        <Group gap={4} wrap="nowrap">
          <TextInput size="xs" autoFocus aria-label={t('network.viewName')} placeholder={t('network.viewName')} value={name} onChange={(e) => setName(e.currentTarget.value)} onKeyDown={(e) => e.key === 'Enter' && submit()} />
          <Button size="compact-sm" onClick={submit}>
            {t('network.saveView')}
          </Button>
        </Group>
      ) : (
        <Button size="compact-sm" variant="subtle" onClick={() => setNaming(true)}>
          + {t('network.saveCurrent')}
        </Button>
      )}
    </Group>
  );
}
