import { Alert, Button, Drawer, Group, Modal, Stack, Table, Text, Title } from '@mantine/core';
import { useEffect, useRef, useState, type KeyboardEvent, type ReactNode } from 'react';
import { useText } from './text';

export interface EditSection {
  id: string;
  /** Section heading (h3). */
  title: string;
  content: ReactNode;
}

/** One field that differs between the user's draft and the record as it is now on the server. */
export interface ConflictField {
  key: string;
  label: string;
  mine: string;
  theirs: string;
}

/** Return this from `onSave` when the server answered 409 (version conflict) to open "Compare and save". */
export interface VersionConflict {
  conflict: ConflictField[];
}

export interface EditDrawerProps {
  opened: boolean;
  /** Called when the drawer should close (after a save, or after the user confirmed discarding). */
  onClose: () => void;
  /** Drawer heading (h2), for example the record name. */
  title: string;
  sections: EditSection[];
  /** True when the form differs from the saved record; enables the unsaved-changes guard. */
  dirty: boolean;
  /** Persist the form. Resolve with nothing on success, with `{ conflict }` on a 409, reject on any other error. */
  onSave: () => Promise<void | VersionConflict>;
  /** Called from the compare dialog: "mine" re-saves the draft on top of the new version, "theirs" drops the draft. */
  onResolveConflict?: (choice: 'mine' | 'theirs') => Promise<void>;
  /** Width in px, default 640 (visual system v3, section 6b). */
  size?: number;
  /** Label of the primary footer button, a verb with the object (default "Save changes"). */
  saveLabel?: string;
}

/**
 * Right-hand edit drawer: titled sections, sticky Save/Cancel (Ctrl/Cmd+Enter saves), a guard before closing with
 * unsaved edits, and a compare dialog for version conflicts. Focus is trapped while open and returns to the trigger.
 */
export function EditDrawer({ opened, onClose, title, sections, dirty, onSave, onResolveConflict, size = 640, saveLabel }: EditDrawerProps) {
  const { tx } = useText();
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [guard, setGuard] = useState(false);
  const [conflict, setConflict] = useState<ConflictField[] | null>(null);
  const [resolving, setResolving] = useState(false);
  const guarding = useRef(false);
  guarding.current = guard;

  useEffect(() => {
    if (!opened) {
      setError(null);
      setGuard(false);
      setConflict(null);
    }
  }, [opened]);

  useEffect(() => {
    if (!opened || !dirty) return;
    const warn = (e: BeforeUnloadEvent) => e.preventDefault();
    window.addEventListener('beforeunload', warn);
    return () => window.removeEventListener('beforeunload', warn);
  }, [opened, dirty]);

  const requestClose = () => {
    if (guarding.current || conflict) return;
    if (dirty) setGuard(true);
    else onClose();
  };

  const save = async () => {
    if (saving) return;
    setSaving(true);
    setError(null);
    try {
      const res = await onSave();
      if (res && 'conflict' in res) setConflict(res.conflict);
      else onClose();
    } catch (e) {
      setError((e as { detail?: string })?.detail ?? tx('edit.saveError'));
    } finally {
      setSaving(false);
    }
  };

  const resolve = async (choice: 'mine' | 'theirs') => {
    setResolving(true);
    try {
      await onResolveConflict?.(choice);
      setConflict(null);
      onClose();
    } catch (e) {
      setError((e as { detail?: string })?.detail ?? tx('edit.saveError'));
      setConflict(null);
    } finally {
      setResolving(false);
    }
  };

  const onKeyDown = (e: KeyboardEvent) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      e.preventDefault();
      void save();
    }
  };

  return (
    <>
      <Drawer
        opened={opened}
        onClose={requestClose}
        position="right"
        size={size}
        title={title}
        closeButtonProps={{ 'aria-label': tx('edit.close') }}
        trapFocus
        styles={{ title: { fontFamily: 'var(--font-serif, inherit)', fontWeight: 600 } }}
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            void save();
          }}
          onKeyDown={onKeyDown}
          noValidate
        >
          <Stack gap="xl" pb="md">
            {sections.map((s) => (
              <section key={s.id} aria-labelledby={`edit-sec-${s.id}`}>
                <Title order={3} id={`edit-sec-${s.id}`} fz="0.8125rem" fw={600} c="var(--color-ink2)" mb="xs">
                  {s.title}
                </Title>
                <Stack gap="sm">{s.content}</Stack>
              </section>
            ))}
          </Stack>
          <div style={{ position: 'sticky', bottom: 0, background: 'var(--mantine-color-body)', borderTop: '1px solid var(--color-border)', padding: 'var(--space-3) 0' }}>
            {error && (
              <Alert color="red" role="alert" mb="xs" p="xs">
                {error}
              </Alert>
            )}
            <Group justify="space-between" wrap="wrap">
              <Text fz="0.75rem" c="dimmed">
                {tx('edit.saveHint')}
              </Text>
              <Group gap="xs">
                <Button variant="default" onClick={requestClose}>
                  {tx('edit.cancel')}
                </Button>
                <Button type="submit" loading={saving} loaderProps={{ 'aria-hidden': true }}>
                  {saving ? tx('edit.saving') : (saveLabel ?? tx('edit.saveChanges'))}
                </Button>
              </Group>
            </Group>
          </div>
        </form>
      </Drawer>

      <Modal opened={opened && guard} onClose={() => setGuard(false)} title={tx('edit.guard.title')} centered zIndex={400} closeButtonProps={{ 'aria-label': tx('edit.close') }}>
        <Text size="sm" mb="md">
          {tx('edit.guard.body')}
        </Text>
        <Group justify="flex-end" gap="xs">
          <Button variant="default" data-autofocus onClick={() => setGuard(false)}>
            {tx('edit.guard.keep')}
          </Button>
          <Button color="red" onClick={onClose}>
            {tx('edit.guard.discard')}
          </Button>
        </Group>
      </Modal>

      <Modal opened={opened && !!conflict} onClose={() => setConflict(null)} title={tx('edit.conflict.title')} centered size="lg" zIndex={400} closeButtonProps={{ 'aria-label': tx('edit.close') }}>
        <Text size="sm" mb="sm">
          {tx('edit.conflict.body')}
        </Text>
        <Table withTableBorder mb="md">
          <Table.Thead>
            <Table.Tr>
              <Table.Th>{tx('edit.conflict.field')}</Table.Th>
              <Table.Th>{tx('edit.conflict.mine')}</Table.Th>
              <Table.Th>{tx('edit.conflict.theirs')}</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {(conflict ?? []).map((f) => (
              <Table.Tr key={f.key}>
                <Table.Th scope="row">{f.label}</Table.Th>
                <Table.Td>{f.mine || tx('edit.conflict.empty')}</Table.Td>
                <Table.Td>{f.theirs || tx('edit.conflict.empty')}</Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
        <Group justify="flex-end" gap="xs">
          <Button variant="default" loading={resolving} onClick={() => void resolve('theirs')}>
            {tx('edit.conflict.useTheirs')}
          </Button>
          <Button loading={resolving} data-autofocus onClick={() => void resolve('mine')}>
            {tx('edit.conflict.keepMine')}
          </Button>
        </Group>
      </Modal>
    </>
  );
}
