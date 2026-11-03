import { IconChevronDown } from '@tabler/icons-react';
import { Button, Group, Menu, Modal, Popover, Select, Stack, Text, TextInput } from '@mantine/core';
import { useContext, useState } from 'react';
import { api, type components } from '../../api';
import { useText } from '../edit/text';
import { UndoContext } from '../edit/undo';

type Schemas = components['schemas'];
export type BulkEntity = Schemas['ListEntity'];
export type BulkActionType = Schemas['BulkActionType'];
export type BulkActionResult = Schemas['BulkActionResult'];
type BulkRequest = Schemas['BulkActionRequest'];

export const ALL_BULK_ACTIONS: BulkActionType[] = ['REASSIGN_OWNER', 'CHANGE_STATUS', 'ADD_TAG', 'REMOVE_TAG', 'SET_NEXT_ACTION', 'REQUEST_CONSENT'];

export interface BulkBarProps {
  entity: BulkEntity;
  /** Ids from the grid selection (the `bulkActions` slot gives them to you). */
  selectedIds: string[];
  /** Clears the selection; called when every record succeeded. */
  clear: () => void;
  /** Which actions this list offers, default all six. */
  actions?: BulkActionType[];
  /** Choices for "Change owner" (without them a plain owner id field is shown). */
  ownerOptions?: { value: string; label: string }[];
  /** Choices for "Change status" (without them a plain text field is shown). */
  statusOptions?: { value: string; label: string }[];
  /** Called with the server result after every run. */
  onDone?: (result: BulkActionResult) => void;
  /** Injected API client for tests, defaults to the shared one. */
  client?: Pick<typeof api, 'POST'>;
}

type Draft = { ownerId: string; status: string; reason: string; tag: string; title: string; dueOn: string };
const EMPTY: Draft = { ownerId: '', status: '', reason: '', tag: '', title: '', dueOn: '' };

function build(action: BulkActionType, entity: BulkEntity, ids: string[], d: Draft): BulkRequest {
  switch (action) {
    case 'REASSIGN_OWNER':
      return { action, entity, ids, params: { ownerId: d.ownerId } };
    case 'CHANGE_STATUS':
      return { action, entity, ids, params: { status: d.status, ...(d.reason ? { reason: d.reason } : {}) } };
    case 'ADD_TAG':
    case 'REMOVE_TAG':
      return { action, entity, ids, params: { tag: d.tag.trim() } };
    case 'SET_NEXT_ACTION':
      return { action, entity, ids, params: { title: d.title.trim(), dueOn: d.dueOn } };
    case 'REQUEST_CONSENT':
      return { action, entity, ids, params: {} };
  }
}

function ready(action: BulkActionType, d: Draft): boolean {
  switch (action) {
    case 'REASSIGN_OWNER':
      return !!d.ownerId.trim();
    case 'CHANGE_STATUS':
      return !!d.status.trim();
    case 'ADD_TAG':
    case 'REMOVE_TAG':
      return !!d.tag.trim();
    case 'SET_NEXT_ACTION':
      return !!d.title.trim() && !!d.dueOn;
    default:
      return true;
  }
}

/**
 * Bulk action bar content for the DataGrid `bulkActions` slot: an Actions menu, a parameter dialog where needed,
 * `POST /bulk-actions`, a result summary ("3 done, 2 skipped") with the reason codes, and an undo toast for tag actions.
 * Partial success is normal: records that were skipped or failed stay selected so the user can see them.
 */
export function BulkBar({ entity, selectedIds, clear, actions = ALL_BULK_ACTIONS, ownerOptions, statusOptions, onDone, client = api }: BulkBarProps) {
  const { tx } = useText();
  const showUndo = useContext(UndoContext);
  const [pending, setPending] = useState<BulkActionType | null>(null);
  const [draft, setDraft] = useState<Draft>(EMPTY);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(false);
  const [result, setResult] = useState<BulkActionResult | null>(null);
  const set = (k: keyof Draft) => (v: string | null) => setDraft((d) => ({ ...d, [k]: v ?? '' }));

  const run = async (action: BulkActionType, d: Draft) => {
    setBusy(true);
    setError(false);
    try {
      const { data } = await client.POST('/bulk-actions', { body: build(action, entity, selectedIds, d) });
      if (!data) throw new Error('bulk failed');
      setResult(data);
      setPending(null);
      onDone?.(data);
      if (data.failed === 0 && data.skipped === 0) clear();
      const okIds = data.results.filter((r) => r.outcome === 'OK').map((r) => r.id);
      const inverse = action === 'ADD_TAG' ? 'REMOVE_TAG' : action === 'REMOVE_TAG' ? 'ADD_TAG' : null;
      if (inverse && okIds.length > 0 && showUndo) {
        showUndo({
          message: tx('bulk.undoLabel', { action: tx(`bulk.action.${action}`), ok: okIds.length }),
          onUndo: async () => {
            const r = await client.POST('/bulk-actions', { body: build(inverse, entity, okIds, d) });
            if (!r.data) throw new Error('undo failed');
          },
        });
      }
    } catch {
      setError(true);
    } finally {
      setBusy(false);
    }
  };

  const choose = (a: BulkActionType) => {
    setDraft(EMPTY);
    if (a === 'REQUEST_CONSENT') void run(a, EMPTY);
    else setPending(a);
  };

  const codes = new Map<string, number>();
  for (const r of result?.results ?? []) if (r.code) codes.set(r.code, (codes.get(r.code) ?? 0) + 1);

  return (
    <Group gap="xs" role="group" aria-label={tx('bulk.bar')}>
      <Menu position="bottom-end" withinPortal>
        <Menu.Target>
          <Button size="xs" variant="default" loading={busy && !pending} aria-haspopup="menu">
            {tx('bulk.actions')} <IconChevronDown size={14} stroke={1.75} aria-hidden style={{ marginLeft: 4, verticalAlign: 'middle' }} />
          </Button>
        </Menu.Target>
        <Menu.Dropdown>
          {actions.map((a) => (
            <Menu.Item key={a} onClick={() => choose(a)}>
              {tx(`bulk.action.${a}`)}
            </Menu.Item>
          ))}
        </Menu.Dropdown>
      </Menu>

      <div role={error ? 'alert' : 'status'} aria-live="polite">
        {error && (
          <Text size="xs" c="red.8">
            {tx('bulk.error')}
          </Text>
        )}
        {!error && result && (
          <Group gap={6} wrap="nowrap">
            <Text size="xs">{tx('bulk.summary', { ok: result.ok, skipped: result.skipped, failed: result.failed })}</Text>
            {codes.size > 0 && (
              <Popover position="bottom" withinPortal trapFocus>
                <Popover.Target>
                  <Button size="compact-xs" variant="subtle">
                    {tx('bulk.details')}
                  </Button>
                </Popover.Target>
                <Popover.Dropdown>
                  <Stack gap={2} component="ul" m={0} p={0} style={{ listStyle: 'none' }}>
                    {[...codes].map(([code, n]) => (
                      <li key={code}>
                        <Text size="sm">
                          {n} x {tx(`bulk.code.${code}`)}
                        </Text>
                      </li>
                    ))}
                  </Stack>
                </Popover.Dropdown>
              </Popover>
            )}
            <Button size="compact-xs" variant="subtle" onClick={() => setResult(null)}>
              {tx('bulk.dismiss')}
            </Button>
          </Group>
        )}
      </div>

      <Modal opened={!!pending} onClose={() => setPending(null)} title={pending ? tx(`bulk.action.${pending}`) : ''} centered closeButtonProps={{ 'aria-label': tx('edit.close') }}>
        {pending && (
          <form
            onSubmit={(e) => {
              e.preventDefault();
              if (ready(pending, draft)) void run(pending, draft);
            }}
          >
            <Stack gap="sm">
              {pending === 'REASSIGN_OWNER' &&
                (ownerOptions ? (
                  <Select label={tx('bulk.field.owner')} data={ownerOptions} value={draft.ownerId || null} onChange={set('ownerId')} required withAsterisk comboboxProps={{ withinPortal: false }} data-autofocus />
                ) : (
                  <TextInput label={tx('bulk.field.ownerId')} value={draft.ownerId} onChange={(e) => set('ownerId')(e.currentTarget.value)} required data-autofocus />
                ))}
              {pending === 'CHANGE_STATUS' && (
                <>
                  {statusOptions ? (
                    <Select label={tx('bulk.field.status')} data={statusOptions} value={draft.status || null} onChange={set('status')} required withAsterisk comboboxProps={{ withinPortal: false }} data-autofocus />
                  ) : (
                    <TextInput label={tx('bulk.field.status')} value={draft.status} onChange={(e) => set('status')(e.currentTarget.value)} required data-autofocus />
                  )}
                  <TextInput label={tx('bulk.field.reason')} value={draft.reason} onChange={(e) => set('reason')(e.currentTarget.value)} />
                </>
              )}
              {(pending === 'ADD_TAG' || pending === 'REMOVE_TAG') && (
                <TextInput label={tx('bulk.field.tag')} value={draft.tag} onChange={(e) => set('tag')(e.currentTarget.value)} required data-autofocus />
              )}
              {pending === 'SET_NEXT_ACTION' && (
                <>
                  <TextInput label={tx('bulk.field.title')} value={draft.title} onChange={(e) => set('title')(e.currentTarget.value)} required data-autofocus />
                  <TextInput label={tx('bulk.field.dueOn')} type="date" value={draft.dueOn} onChange={(e) => set('dueOn')(e.currentTarget.value)} required />
                </>
              )}
              <Group justify="flex-end" gap="xs">
                <Button variant="default" onClick={() => setPending(null)}>
                  {tx('bulk.cancel')}
                </Button>
                <Button type="submit" loading={busy} disabled={!ready(pending, draft)}>
                  {busy ? tx('bulk.running') : tx('bulk.run')}
                </Button>
              </Group>
            </Stack>
          </form>
        )}
      </Modal>
    </Group>
  );
}
