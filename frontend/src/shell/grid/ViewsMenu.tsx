import { ActionIcon, Button, Group, Menu, Modal, Stack, TextInput } from '@mantine/core';
import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { ColumnState, SavedView, SortState, ViewStore } from './types';

interface Props<F> {
  store: ViewStore<F>;
  /** Current screen configuration, captured when "Save view" is pressed. */
  current: { filters: F; columns: ColumnState; sort: SortState | null };
  onApply: (view: SavedView<F>) => void;
}

/** Saved views menu: apply, save current, delete. Works against any ViewStore. */
export function ViewsMenu<F>({ store, current, onApply }: Props<F>) {
  const { t } = useTranslation();
  const [views, setViews] = useState<SavedView<F>[]>([]);
  const [naming, setNaming] = useState(false);
  const [name, setName] = useState('');
  const refresh = useCallback(() => void store.list().then(setViews, () => setViews([])), [store]);
  useEffect(refresh, [refresh]);

  const save = async () => {
    const n = name.trim();
    if (!n) return;
    const existing = views.find((v) => v.name.toLowerCase() === n.toLowerCase());
    await store.save({ id: existing?.id, name: n, ...current });
    setNaming(false);
    setName('');
    refresh();
  };

  return (
    <>
      <Menu position="bottom-end" withinPortal>
        <Menu.Target>
          <Button variant="default" size="xs">
            {t('grid.views')}
          </Button>
        </Menu.Target>
        <Menu.Dropdown>
          {views.length === 0 && <Menu.Label>{t('grid.noViews')}</Menu.Label>}
          {views.map((v) => (
            <Group key={v.id} gap={0} wrap="nowrap">
              <Menu.Item style={{ flex: 1 }} onClick={() => onApply(v)}>
                {v.name}
              </Menu.Item>
              <ActionIcon
                variant="subtle"
                color="red"
                aria-label={t('grid.deleteView', { name: v.name })}
                onClick={() => void store.delete(v.id).then(refresh)}
              >
                {'\u00D7'}
              </ActionIcon>
            </Group>
          ))}
          <Menu.Divider />
          <Menu.Item onClick={() => setNaming(true)}>{t('grid.saveView')}</Menu.Item>
        </Menu.Dropdown>
      </Menu>
      <Modal opened={naming} onClose={() => setNaming(false)} title={t('grid.saveView')} size="sm">
        <Stack>
          <TextInput data-autofocus label={t('grid.viewName')} value={name} onChange={(e) => setName(e.currentTarget.value)} onKeyDown={(e) => e.key === 'Enter' && void save()} />
          <Group justify="flex-end">
            <Button variant="default" onClick={() => setNaming(false)}>
              {t('shell.cancel')}
            </Button>
            <Button onClick={() => void save()} disabled={!name.trim()}>
              {t('shell.save')}
            </Button>
          </Group>
        </Stack>
      </Modal>
    </>
  );
}
