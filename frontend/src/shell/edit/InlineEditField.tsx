import { ActionIcon, Group, Text, TextInput } from '@mantine/core';
import { useContext, useEffect, useRef, useState, type ReactNode } from 'react';
import { useText } from './text';
import { UndoContext } from './undo';

export interface InlineEditFieldProps {
  /** Visible field name; also used in the pencil and the input label. */
  label: string;
  /** Current saved value. */
  value: string;
  /** Persist the new value. Reject to roll back and show the error. */
  onSave: (next: string) => Promise<void>;
  /** When set (and an `UndoToastProvider` exists) a successful save shows an Undo toast that calls this with the old value. */
  onUndo?: (previous: string) => Promise<void>;
  /** Custom read-only rendering of the value. */
  display?: (value: string) => ReactNode;
  /** Return an error text to block the save. */
  validate?: (next: string) => string | null;
  /** HTML input type, default "text". */
  type?: 'text' | 'email' | 'tel' | 'url' | 'date';
  /** Hide the pencil. */
  readOnly?: boolean;
}

/**
 * One field edited in place: pencil, Enter saves, Esc cancels, optimistic with rollback, optional undo toast.
 * Focus returns to the pencil after saving or cancelling.
 */
export function InlineEditField({ label, value, onSave, onUndo, display, validate, type = 'text', readOnly }: InlineEditFieldProps) {
  const { tx } = useText();
  const showUndo = useContext(UndoContext);
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(value);
  const [shown, setShown] = useState(value);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const pencil = useRef<HTMLButtonElement>(null);
  const refocus = useRef(false);

  useEffect(() => setShown(value), [value]);
  useEffect(() => {
    if (!editing && refocus.current) {
      refocus.current = false;
      pencil.current?.focus();
    }
  }, [editing]);

  const stop = () => {
    refocus.current = true;
    setEditing(false);
    setError(null);
  };

  const commit = async () => {
    if (busy) return;
    if (draft === shown) return stop();
    const bad = validate?.(draft);
    if (bad) return setError(bad);
    const previous = shown;
    setBusy(true);
    setShown(draft);
    try {
      await onSave(draft);
      stop();
      if (onUndo && showUndo) {
        showUndo({
          message: tx('edit.inline.saved', { label }),
          onUndo: async () => {
            await onUndo(previous);
            setShown(previous);
          },
        });
      }
    } catch (e) {
      setShown(previous);
      setError((e as { detail?: string; message?: string })?.detail ?? tx('edit.saveError'));
    } finally {
      setBusy(false);
    }
  };

  if (editing) {
    return (
      <TextInput
        label={label}
        type={type}
        value={draft}
        onChange={(e) => setDraft(e.currentTarget.value)}
        error={error}
        description={tx('edit.inline.hint')}
        disabled={busy}
        data-autofocus
        autoFocus
        onKeyDown={(e) => {
          if (e.key === 'Enter') {
            e.preventDefault();
            void commit();
          } else if (e.key === 'Escape') {
            e.stopPropagation();
            stop();
          }
        }}
        rightSectionWidth={40}
        rightSection={
          <ActionIcon variant="subtle" size="lg" aria-label={tx('edit.inline.save', { label })} onClick={() => void commit()} loading={busy}>
            <span aria-hidden>{'✓'}</span>
          </ActionIcon>
        }
      />
    );
  }

  return (
    <Group gap="xs" wrap="nowrap" align="center">
      <Text component="div" size="sm" aria-label={undefined}>
        {shown ? (display ? display(shown) : shown) : <Text span c="dimmed">{tx('edit.inline.empty')}</Text>}
      </Text>
      {error && (
        <Text size="xs" c="red.8" role="alert">
          {error}
        </Text>
      )}
      {!readOnly && (
        <ActionIcon
          ref={pencil}
          variant="subtle"
          size="lg"
          aria-label={tx('edit.inline.edit', { label })}
          onClick={() => {
            setDraft(shown);
            setError(null);
            setEditing(true);
          }}
        >
          <span aria-hidden>{'✎'}</span>
        </ActionIcon>
      )}
    </Group>
  );
}
