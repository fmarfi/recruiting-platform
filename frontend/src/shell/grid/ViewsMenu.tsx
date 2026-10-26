import { ActionIcon, Alert, Button, Checkbox, Group, Menu, Modal, Stack, Text, TextInput } from '@mantine/core';
import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { ColumnState, SavedView, SortState, ViewStore } from './types';
import { viewErrorCode } from './viewErrors';
import type { ViewNotice } from './viewStoreFactory';
import { effectiveShare, findOverwrite, saveVisibility, shouldWarnQ } from './viewsMenuLogic';

interface Props<F> {
  store: ViewStore<F>;
  /** Current screen configuration, captured when "Save view" is pressed. */
  current: { filters: F; columns: ColumnState; sort: SortState | null };
  onApply: (view: SavedView<F>) => void;
  /** From useViewStore: fallback, migration and storage notices. */
  notice?: ViewNotice | null;
}

/** Saved views menu: apply, save current (private or shared), rename, delete. Works against any ViewStore. */
export function ViewsMenu<F>({ store, current, onApply, notice }: Props<F>) {
  const { t } = useTranslation();
  const [views, setViews] = useState<SavedView<F>[]>([]);
  const [naming, setNaming] = useState(false);
  const [name, setName] = useState('');
  const [shareChoice, setShareChoice] = useState<boolean | null>(null);
  const [confirm, setConfirm] = useState(false);
  const [renaming, setRenaming] = useState<SavedView<F> | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const refresh = useCallback(() => void store.list().then(setViews, (e) => setErr(viewErrorCode(e) ?? 'UNAVAILABLE')), [store]);
  useEffect(refresh, [refresh]);

  const fail = (e: unknown) => {
    const code = viewErrorCode(e) ?? 'UNAVAILABLE';
    setErr(code);
    if (code === 'CONFLICT' || code === 'NOT_FOUND') refresh();
  };
  const close = () => {
    setNaming(false);
    setRenaming(null);
    setName('');
    setShareChoice(null);
    setConfirm(false);
    setErr(null);
  };

  const overwrite = findOverwrite(views, name);
  const share = effectiveShare(shareChoice, overwrite);
  const save = async () => {
    const n = name.trim();
    if (!n) return;
    const existing = overwrite;
    if (existing && !confirm) {
      setConfirm(true);
      return;
    }
    try {
      await store.save({ id: existing?.id, name: n, ...current, visibility: saveVisibility(!!store.canShare, share, existing) });
      close();
      refresh();
    } catch (e) {
      fail(e);
    }
  };
  const rename = async () => {
    const n = name.trim();
    if (!n || !renaming) return;
    try {
      if (store.rename) await store.rename(renaming.id, n);
      else await store.save({ ...renaming, name: n });
      close();
      refresh();
    } catch (e) {
      fail(e);
    }
  };
  const remove = (v: SavedView<F>) => void store.delete(v.id).then(refresh, fail);

  const noticeText =
    notice?.type === 'migrated'
      ? t('grid.notice.migrated', { moved: notice.moved }) + (notice.left ? t('grid.notice.migratedLeft', { left: notice.left }) : '') + (notice.dropped ? t('grid.notice.migratedDropped', { dropped: notice.dropped }) : '')
      : notice
        ? t(`grid.notice.${notice.type}`)
        : null;
  const warnQ = shouldWarnQ(share, !!store.warnsOnShare?.(current.filters));
  const errorAlert = err && (
    <Alert color="red" role="alert">
      {t(`grid.err.${err}`)}
    </Alert>
  );

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
              <Menu.Item style={{ flex: 1 }} onClick={() => onApply(v)} title={v.visibility === 'SHARED' && v.ownerName ? t('grid.sharedWith', { name: v.ownerName }) : undefined}>
                {v.name}
                {v.visibility === 'SHARED' && (
                  <Text span size="xs" c="dimmed" ml={6}>
                    {t('grid.shared')}
                  </Text>
                )}
              </Menu.Item>
              {v.editable !== false && (
                <>
                  <ActionIcon
                    variant="subtle"
                    aria-label={t('grid.rename', { name: v.name })}
                    onClick={() => {
                      setErr(null);
                      setName(v.name);
                      setRenaming(v);
                    }}
                  >
                    {'✎'}
                  </ActionIcon>
                  <ActionIcon variant="subtle" color="red" aria-label={t('grid.deleteView', { name: v.name })} onClick={() => remove(v)}>
                    {'×'}
                  </ActionIcon>
                </>
              )}
            </Group>
          ))}
          <Menu.Divider />
          <Menu.Item
            onClick={() => {
              setConfirm(false);
              setErr(null);
              setNaming(true);
            }}
          >
            {t('grid.saveView')}
          </Menu.Item>
        </Menu.Dropdown>
      </Menu>
      {noticeText && (
        <Text size="xs" c={notice?.type === 'persist' ? 'red' : 'dimmed'} role="status">
          {noticeText}
        </Text>
      )}
      {err && !naming && !renaming && (
        <Text size="xs" c="red" role="alert">
          {t(`grid.err.${err}`)}
        </Text>
      )}
      <Modal opened={naming} onClose={close} title={t('grid.saveView')} size="sm">
        <Stack>
          <TextInput
            data-autofocus
            label={t('grid.viewName')}
            value={name}
            maxLength={120}
            onChange={(e) => {
              setName(e.currentTarget.value);
              setConfirm(false);
              setErr(null);
            }}
            onKeyDown={(e) => e.key === 'Enter' && void save()}
          />
          {store.canShare && <Checkbox label={t('grid.shareToggle')} checked={share} onChange={(e) => setShareChoice(e.currentTarget.checked)} />}
          {warnQ && (
            <Text size="sm" role="status">
              {t('grid.qNotSaved')}
            </Text>
          )}
          {confirm && (
            <Text size="sm" role="alert">
              {t('grid.overwrite', { name: name.trim() })}
            </Text>
          )}
          {errorAlert}
          <Group justify="flex-end">
            <Button variant="default" onClick={close}>
              {t('shell.cancel')}
            </Button>
            <Button onClick={() => void save()} disabled={!name.trim()}>
              {confirm ? t('grid.replace') : t('shell.save')}
            </Button>
          </Group>
        </Stack>
      </Modal>
      <Modal opened={!!renaming} onClose={close} title={t('grid.renameTitle')} size="sm">
        <Stack>
          <TextInput data-autofocus label={t('grid.viewName')} value={name} maxLength={120} onChange={(e) => setName(e.currentTarget.value)} onKeyDown={(e) => e.key === 'Enter' && void rename()} />
          {errorAlert}
          <Group justify="flex-end">
            <Button variant="default" onClick={close}>
              {t('shell.cancel')}
            </Button>
            <Button onClick={() => void rename()} disabled={!name.trim()}>
              {t('shell.save')}
            </Button>
          </Group>
        </Stack>
      </Modal>
    </>
  );
}
