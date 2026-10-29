import { Button, Group, Paper, Text } from '@mantine/core';
import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from 'react';
import { useText } from './text';

/** How long an undo toast stays visible before it closes (WCAG 2.2.1, rule A11). */
export const UNDO_MS = 6000;

export interface UndoRequest {
  /** Visible message, for example "Owner changed to Can Yildiz". */
  message: string;
  /** Reverts the change. Rejecting shows an error toast. */
  onUndo: () => Promise<void> | void;
}

export type ShowUndo = (r: UndoRequest) => void;
export const UndoContext = createContext<ShowUndo | null>(null);

/** Returns `showUndo({ message, onUndo })`. Needs an `UndoToastProvider` above it. */
export function useUndoToast(): ShowUndo {
  const ctx = useContext(UndoContext);
  if (!ctx) throw new Error('useUndoToast needs <UndoToastProvider>');
  return ctx;
}

interface Active extends UndoRequest {
  key: number;
  kind: 'undo' | 'info' | 'error';
}

function editable(el: EventTarget | null): boolean {
  const e = el as HTMLElement | null;
  return !!e && (e.isContentEditable || /^(INPUT|TEXTAREA|SELECT)$/.test(e.tagName));
}

/**
 * Hosts the single undo toast (a newer one replaces the older). It stays 6 s, pauses while hovered or focused,
 * and Ctrl/Cmd+Z undoes it while visible (ignored inside text fields, where it undoes typing).
 */
export function UndoToastProvider({ children, duration = UNDO_MS }: { children: ReactNode; duration?: number }) {
  const { tx } = useText();
  const [toast, setToast] = useState<Active | null>(null);
  const [paused, setPaused] = useState(false);
  const counter = useRef(0);
  const remaining = useRef(duration);
  const startedAt = useRef(0);

  const close = useCallback(() => setToast(null), []);
  const show = useCallback<ShowUndo>(
    (r) => {
      remaining.current = duration;
      setPaused(false);
      setToast({ ...r, key: ++counter.current, kind: 'undo' });
    },
    [duration],
  );

  const undo = useCallback(async () => {
    if (!toast || toast.kind !== 'undo') return;
    const current = toast;
    setToast(null);
    remaining.current = duration;
    try {
      await current.onUndo();
      setToast({ key: ++counter.current, kind: 'info', message: tx('edit.undo.done'), onUndo: () => {} });
    } catch {
      setToast({ key: ++counter.current, kind: 'error', message: tx('edit.undo.failed'), onUndo: () => {} });
    }
  }, [toast, duration, tx]);

  useEffect(() => {
    if (!toast || paused) return;
    startedAt.current = Date.now();
    const id = setTimeout(close, remaining.current);
    return () => {
      clearTimeout(id);
      remaining.current = Math.max(1000, remaining.current - (Date.now() - startedAt.current));
    };
  }, [toast, paused, close]);

  useEffect(() => {
    if (toast?.kind !== 'undo') return;
    const onKey = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && !e.shiftKey && e.key.toLowerCase() === 'z' && !editable(e.target)) {
        e.preventDefault();
        void undo();
      }
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [toast, undo]);

  return (
    <UndoContext.Provider value={show}>
      {children}
      <div
        style={{ position: 'fixed', insetInline: 16, bottom: 16, zIndex: 1000, display: 'flex', justifyContent: 'center', pointerEvents: 'none' }}
        role={toast?.kind === 'error' ? 'alert' : 'status'}
      >
        {toast && (
          <Paper
            key={toast.key}
            withBorder
            shadow="md"
            p="sm"
            style={{ pointerEvents: 'auto' }}
            onMouseEnter={() => setPaused(true)}
            onMouseLeave={() => setPaused(false)}
            onFocusCapture={() => setPaused(true)}
            onBlurCapture={(e) => {
              if (!e.currentTarget.contains(e.relatedTarget as Node | null)) setPaused(false);
            }}
          >
            <Group gap="md" wrap="nowrap">
              <Text size="sm">{toast.message}</Text>
              {toast.kind === 'undo' && (
                <Button size="xs" variant="light" onClick={() => void undo()}>
                  {tx('edit.undo.action')}{' '}
                  <Text span size="xs" ml={6} aria-hidden>
                    Ctrl+Z
                  </Text>
                </Button>
              )}
            </Group>
          </Paper>
        )}
      </div>
    </UndoContext.Provider>
  );
}
