// Form state and save logic for Quick add.
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api';
import { problemText, toIsoDate, type Schemas } from '../shared';
import type { Option } from './EntityPicker';

export type ActivityType = Schemas['ActivityType'];
export type Mode = 'activity' | 'action';
export const TYPES: ActivityType[] = ['CALL', 'VISIT', 'MEETING', 'EMAIL', 'NOTE'];
const LAST_KEY = 'quickAdd.lastType';

export type QuickAddContext = {
  account?: { id: string; name: string };
  person?: { id: string; name: string };
  jobOrder?: { id: string; title: string };
};

function readLast(): ActivityType {
  try {
    const v = localStorage.getItem(LAST_KEY) as ActivityType | null;
    if (v && TYPES.includes(v)) return v;
  } catch {
    /* storage can be blocked */
  }
  return 'CALL';
}

export const nowLocal = () => {
  const d = new Date();
  return `${toIsoDate(d)}T${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
};

export function useQuickAddForm(opened: boolean, initialMode: Mode, context: QuickAddContext | undefined, onSaved?: () => void, onClose?: () => void) {
  const { t } = useTranslation();
  const [mode, setMode] = useState<Mode>(initialMode);
  const [type, setType] = useState<ActivityType>(readLast);
  const [note, setNote] = useState('');
  const [when, setWhen] = useState(nowLocal);
  const [account, setAccount] = useState<Option | null>(null);
  const [person, setPerson] = useState<Option | null>(null);
  const [followUp, setFollowUp] = useState(false);
  const [title, setTitle] = useState('');
  const [due, setDue] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();
  const [done, setDone] = useState(false);

  useEffect(() => {
    if (!opened) return;
    setMode(initialMode);
    setType(readLast());
    setNote('');
    setWhen(nowLocal());
    setAccount(context?.account ? { value: context.account.id, label: context.account.name } : null);
    setPerson(context?.person ? { value: context.person.id, label: context.person.name } : null);
    setFollowUp(false);
    setTitle('');
    setDue('');
    setError(undefined);
    setDone(false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [opened]);

  const hasContext = !!(account || person);
  const canSave =
    !busy && (mode === 'activity' ? note.trim().length > 0 && hasContext && (!followUp || (title.trim() && due)) : title.trim().length > 0 && !!due);

  const save = async () => {
    if (!canSave) return;
    setBusy(true);
    setError(undefined);
    const ctx = {
      ...(account ? { accountId: account.value } : {}),
      ...(person ? { personId: person.value } : {}),
      ...(context?.jobOrder ? { jobOrderId: context.jobOrder.id } : {}),
    };
    try {
      if (mode === 'activity') {
        const r = await api.POST('/activities', { body: { type, note: note.trim(), occurredAt: new Date(when).toISOString(), ...ctx } });
        if (r.error) throw r.error;
        try {
          localStorage.setItem(LAST_KEY, type);
        } catch {
          /* ignore */
        }
      }
      if (mode === 'action' || followUp) {
        const r = await api.POST('/next-actions', { body: { title: title.trim(), dueOn: due, ...ctx } });
        if (r.error) throw r.error;
      }
      setDone(true);
      onSaved?.();
      onClose?.();
    } catch (e) {
      setError(problemText(e, t('quickAdd.failed')));
    } finally {
      setBusy(false);
    }
  };

  return {
    mode, setMode, type, setType, note, setNote, when, setWhen, account, setAccount, person, setPerson,
    followUp, setFollowUp, title, setTitle, due, setDue, busy, error, done, hasContext, canSave, save,
  };
}
