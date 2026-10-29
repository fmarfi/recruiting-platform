// Shared plumbing for the three edit drawers (client, candidate, search): draft state, field errors, undo and 409 compare.
import { NativeSelect } from '@mantine/core';
import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api';
import { useUndoToast, type ConflictField, type VersionConflict } from '../../shell/edit';

type Result<R> = { data?: R; error?: unknown; response?: { status: number } };

/** Field errors of a ProblemDetail (`errors[]`, code VALIDATION_FAILED) as `{ field: message }`. */
export function fieldErrors(error: unknown): Record<string, string> {
  const list = (error as { errors?: { field: string; message: string }[] } | undefined)?.errors;
  return Object.fromEntries((list ?? []).map((e) => [e.field, e.message]));
}

/** Empty text means "clear" for nullable API fields. */
export const orNull = (v: string): string | null => (v.trim() === '' ? null : v.trim());

/** Draft form values for a drawer; resets to `initial` every time the drawer opens. */
export function useDraft<F extends Record<string, string>>(initial: F, opened: boolean) {
  const [values, setValues] = useState<F>(initial);
  const [errors, setErrors] = useState<Record<string, string>>({});
  useEffect(() => {
    if (opened) {
      setValues(initial);
      setErrors({});
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [opened]);
  const set = (key: keyof F & string, v: string) => {
    setValues((s) => ({ ...s, [key]: v }));
    setErrors((e) => (e[key] ? { ...e, [key]: '' } : e));
  };
  const dirty = (Object.keys(initial) as (keyof F)[]).some((k) => values[k] !== initial[k]);
  return { values, set, errors, setErrors, dirty };
}

interface SaveConfig<R extends { version: number }, F extends Record<string, string>> {
  record: R;
  initial: F;
  values: F;
  setErrors: (e: Record<string, string>) => void;
  /** API body for the given form keys (without version). */
  toBody: (values: F, keys: (keyof F & string)[]) => Record<string, unknown>;
  patch: (body: Record<string, unknown>, version: number) => Promise<Result<R>>;
  fetchLatest: () => Promise<Result<R>>;
  toForm: (r: R) => F;
  labels: Record<keyof F & string, string>;
  /** Undo toast text. */
  message: string;
  onSaved: () => void;
  /** Changes outside the PATCH body (for example tags), applied after the fields and reverted by undo. */
  extra?: { changed: boolean; apply: () => Promise<void>; revert: () => Promise<void> };
}

/** onSave / onResolveConflict for `EditDrawer`: sends only changed fields, shows inline field errors, offers undo, handles VERSION_CONFLICT. */
export function useRecordSave<R extends { version: number }, F extends Record<string, string>>(c: SaveConfig<R, F>) {
  const { t } = useTranslation();
  const showUndo = useUndoToast();
  const [latest, setLatest] = useState<R | null>(null);
  const changedKeys = useCallback(() => (Object.keys(c.initial) as (keyof F & string)[]).filter((k) => c.values[k] !== c.initial[k]), [c.initial, c.values]);

  const done = (saved: R | undefined, keys: (keyof F & string)[], previous: F) => {
    c.onSaved();
    showUndo({
      message: c.message,
      onUndo: async () => {
        if (saved && keys.length) {
          const r = await c.patch(c.toBody(previous, keys), saved.version);
          if (r.error) throw r.error;
        }
        if (c.extra?.changed) await c.extra.revert();
        c.onSaved();
      },
    });
  };

  const onSave = async (): Promise<void | VersionConflict> => {
    const keys = changedKeys();
    const extra = !!c.extra?.changed;
    if (keys.length === 0 && !extra) return;
    let saved: R | undefined;
    if (keys.length) {
      const r = await c.patch(c.toBody(c.values, keys), c.record.version);
      if (r.error) {
        const code = (r.error as { code?: string }).code;
        if (code === 'VERSION_CONFLICT' || (r.response?.status === 409 && code !== 'STATE_CONFLICT')) {
          const cur = (await c.fetchLatest()).data;
          if (cur) {
            setLatest(cur);
            const theirs = c.toForm(cur);
            const conflict: ConflictField[] = keys.filter((k) => theirs[k] !== c.initial[k]).map((k) => ({ key: k, label: c.labels[k], mine: String(c.values[k] ?? ""), theirs: String(theirs[k] ?? "") }));
            if (conflict.length) return { conflict };
            // nothing of mine clashes: retry once on top of the new version
            const retry = await c.patch(c.toBody(c.values, keys), cur.version);
            if (retry.error) throw retry.error;
            saved = retry.data;
          }
        }
        if (!saved) {
          const fe = fieldErrors(r.error);
          if (Object.keys(fe).length) {
            c.setErrors(fe);
            throw { detail: t('accounts.edit.fixFields') };
          }
          throw r.error;
        }
      } else saved = r.data;
    }
    if (extra) await c.extra!.apply();
    done(saved, keys, c.initial);
  };

  const onResolveConflict = async (choice: 'mine' | 'theirs') => {
    if (choice === 'theirs' || !latest) return void c.onSaved();
    const keys = changedKeys();
    const r = await c.patch(c.toBody(c.values, keys), latest.version);
    if (r.error) throw r.error;
    c.onSaved();
  };

  return { onSave, onResolveConflict };
}

interface Option {
  value: string;
  label: string;
}

/** Owner picker over `GET /users` (readable by every role). */
export function OwnerSelect({ label, value, current, onChange, error }: { label: string; value: string; current?: { id: string; name: string }; onChange: (v: string) => void; error?: string }) {
  const [users, setUsers] = useState<Option[]>([]);
  useEffect(() => {
    let live = true;
    void Promise.resolve(api.GET('/users', { params: { query: { active: true, size: 100 } } })).then((r) => {
      if (live && r?.data) setUsers(r.data.content.map((u) => ({ value: u.id, label: u.name })));
    });
    return () => {
      live = false;
    };
  }, []);
  const data = !current || users.some((u) => u.value === current.id) ? users : [{ value: current.id, label: current.name }, ...users];
  return <NativeSelect label={label} value={value} data={data} onChange={(e) => onChange(e.currentTarget.value)} error={error || undefined} />;
}
