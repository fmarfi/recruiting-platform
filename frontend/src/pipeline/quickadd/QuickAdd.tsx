import { Button, Group, Modal, SegmentedControl, Stack, Switch, Text, TextInput, Textarea } from '@mantine/core';
import { useMediaQuery } from '@mantine/hooks';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api';
import { EntityPicker, type Option } from './EntityPicker';
import { problemText, toIsoDate, type Schemas } from '../shared';

type ActivityType = Schemas['ActivityType'];
const TYPES: ActivityType[] = ['CALL', 'VISIT', 'MEETING', 'EMAIL', 'NOTE'];
const LAST_KEY = 'quickAdd.lastType';

export type QuickAddContext = {
  account?: { id: string; name: string };
  person?: { id: string; name: string };
  jobOrder?: { id: string; title: string };
};

export type QuickAddProps = {
  opened: boolean;
  onClose: () => void;
  /** Pre-filled context; the pickers can change account and person. */
  context?: QuickAddContext;
  /** `activity` logs a call, visit or note; `action` creates a next action. */
  initialMode?: 'activity' | 'action';
  /** Called after a successful save so the host screen can reload its timeline. */
  onSaved?: () => void;
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

const nowLocal = () => {
  const d = new Date();
  return `${toIsoDate(d)}T${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
};

/**
 * Quick add (UX-06): log a call, visit, meeting, e-mail or note, or create a next action, in under 30 seconds.
 * Controlled by the host (account, person and job pages, or the top bar of frontend-shell).
 */
export function QuickAdd({ opened, onClose, context, initialMode = 'activity', onSaved }: QuickAddProps) {
  const { t } = useTranslation();
  const phone = useMediaQuery('(max-width: 48em)');
  const [mode, setMode] = useState<'activity' | 'action'>(initialMode);
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
        const r = await api.POST('/activities', {
          body: { type, note: note.trim(), occurredAt: new Date(when).toISOString(), ...ctx },
        });
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
      onClose();
    } catch (e) {
      setError(problemText(e, t('quickAdd.failed')));
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal
      opened={opened}
      onClose={onClose}
      title={t('quickAdd.title')}
      fullScreen={!!phone}
      centered={!phone}
      trapFocus
      onKeyDown={(e) => {
        if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') void save();
      }}
    >
      <Stack>
        <SegmentedControl
          fullWidth
          value={mode}
          onChange={(v) => setMode(v as 'activity' | 'action')}
          data={[
            { value: 'activity', label: t('quickAdd.modeActivity') },
            { value: 'action', label: t('quickAdd.modeAction') },
          ]}
        />
        {mode === 'activity' && (
          <>
            <SegmentedControl
              aria-label={t('quickAdd.type')}
              value={type}
              onChange={(v) => setType(v as ActivityType)}
              data={TYPES.map((x) => ({ value: x, label: t(`quickAdd.types.${x}`) }))}
              style={{ overflowX: 'auto' }}
            />
            <Textarea
              label={t('quickAdd.note')}
              value={note}
              onChange={(e) => setNote(e.currentTarget.value)}
              autosize
              minRows={3}
              maxLength={4000}
              data-autofocus
            />
          </>
        )}
        {mode === 'action' && (
          <TextInput label={t('quickAdd.actionTitle')} value={title} onChange={(e) => setTitle(e.currentTarget.value)} maxLength={300} data-autofocus />
        )}
        <Group grow align="flex-start">
          <EntityPicker kind="account" label={t('quickAdd.account')} value={account} onChange={setAccount} />
          <EntityPicker kind="person" label={t('quickAdd.person')} value={person} onChange={setPerson} />
        </Group>
        {context?.jobOrder && (
          <Text size="sm" c="dimmed">
            {t('quickAdd.job')}: {context.jobOrder.title}
          </Text>
        )}
        {mode === 'activity' && (
          <>
            <TextInput type="datetime-local" label={t('quickAdd.when')} value={when} max={nowLocal()} onChange={(e) => setWhen(e.currentTarget.value)} />
            <Switch label={t('quickAdd.followUp')} checked={followUp} onChange={(e) => setFollowUp(e.currentTarget.checked)} />
          </>
        )}
        {(mode === 'action' || followUp) && (
          <Group grow align="flex-start">
            {mode === 'activity' && (
              <TextInput label={t('quickAdd.actionTitle')} value={title} onChange={(e) => setTitle(e.currentTarget.value)} maxLength={300} />
            )}
            <TextInput type="date" label={t('quickAdd.due')} value={due} onChange={(e) => setDue(e.currentTarget.value)} />
          </Group>
        )}
        {mode === 'activity' && !hasContext && (
          <Text size="xs" c="dimmed">
            {t('quickAdd.needContext')}
          </Text>
        )}
        {error && (
          <Text c="red" size="sm" role="alert">
            {error}
          </Text>
        )}
        <Group justify="flex-end">
          <Button variant="default" onClick={onClose}>
            {t('common.cancel')}
          </Button>
          <Button onClick={() => void save()} disabled={!canSave || done} loading={busy}>
            {t('quickAdd.save')}
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
