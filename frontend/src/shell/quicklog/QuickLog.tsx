import { Alert, Button, Chip, Group, Modal, NumberInput, Stack, Text, Textarea } from '@mantine/core';
import { useEffect, useRef, useState, type KeyboardEvent } from 'react';
import { api, type components } from '../../api';
import { EntityPicker, type Option } from '../../pipeline/quickadd/EntityPicker';
import { useTranslation } from 'react-i18next';
import { useUndoToast } from '../edit';
import { problemText } from '../lib/problem';
import { useQlText } from './text';

type Schemas = components['schemas'];
type ActivityType = Schemas['ActivityType'];
type Outcome = Schemas['ActivityOutcome'];
type Direction = Schemas['ActivityDirection'];

const TYPES: ActivityType[] = ['CALL', 'VISIT', 'MEETING', 'EMAIL', 'NOTE'];
const OUTCOMES: Outcome[] = ['CONNECTED', 'NO_ANSWER', 'LEFT_MESSAGE', 'CALLBACK_REQUESTED', 'INTERESTED', 'NOT_INTERESTED', 'MEETING_BOOKED', 'WRONG_CONTACT', 'COMPLETED'];
const LAST_TYPE_KEY = 'quicklog.lastType';

function remembered(): ActivityType {
  try {
    const v = localStorage.getItem(LAST_TYPE_KEY);
    if (TYPES.includes(v as ActivityType)) return v as ActivityType;
  } catch {
    /* storage unavailable */
  }
  return 'CALL';
}

export interface QuickLogProps {
  opened: boolean;
  onClose: () => void;
  /** Prefill: the person the page is about. */
  personId?: string;
  /** Prefill: the client (fixed, no picker). */
  accountId?: string;
  /** Prefill: the search (job order); its client is taken from it. */
  jobOrderId?: string;
  /** Called after a save and after an undo, so the page can reload. */
  onDone?: () => void;
}

/** Quick log composer (S4, R70): person and search prefilled, CALL by default, Enter saves, Esc closes, undo toast after saving. */
export function QuickLog({ opened, onClose, personId, accountId, jobOrderId, onDone }: QuickLogProps) {
  const { t } = useTranslation();
  const tx = useQlText();
  const showUndo = useUndoToast();
  const [type, setType] = useState<ActivityType>(remembered);
  const [outcome, setOutcome] = useState<Outcome | null>(null);
  const [direction, setDirection] = useState<Direction>('OUTBOUND');
  const [duration, setDuration] = useState<number | string>('');
  const [note, setNote] = useState('');
  const [person, setPerson] = useState<Option | null>(null);
  const [account, setAccount] = useState<Option | null>(null);
  const [jobTitle, setJobTitle] = useState<string | null>(null);
  const [jobAccountId, setJobAccountId] = useState<string | null>(null);
  const [errs, setErrs] = useState<{ target?: string; duration?: string }>({});
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const noteRef = useRef<HTMLTextAreaElement>(null);

  // Prefill from the page each time the composer opens.
  useEffect(() => {
    if (!opened) return;
    setType(remembered());
    setOutcome(null);
    setDirection('OUTBOUND');
    setDuration('');
    setNote('');
    setErrs({});
    setErr(null);
    setPerson(null);
    setAccount(null);
    setJobTitle(null);
    setJobAccountId(null);
    let live = true;
    if (personId) {
      void api.GET('/people/{id}', { params: { path: { id: personId } } }).then((r) => {
        if (live) setPerson({ value: personId, label: r.data?.name ?? personId });
      });
    }
    if (jobOrderId) {
      void api.GET('/job-orders/{id}', { params: { path: { id: jobOrderId } } }).then((r) => {
        if (!live || !r.data) return;
        setJobTitle(r.data.title);
        setJobAccountId(r.data.account.id);
      });
    }
    return () => {
      live = false;
    };
  }, [opened, personId, jobOrderId]);

  const effectiveAccount = accountId ?? jobAccountId ?? account?.value ?? undefined;
  const effectivePerson = person?.value ?? personId;

  async function submit() {
    if (busy) return;
    const next: { target?: string; duration?: string } = {};
    if (!effectivePerson && !effectiveAccount) next.target = tx('quicklog.needsTarget');
    const mins = duration === '' ? undefined : Number(duration);
    if (mins !== undefined && (!Number.isInteger(mins) || mins < 0 || mins > 1440)) next.duration = tx('quicklog.tooLong');
    setErrs(next);
    if (next.target || next.duration) return setErr(null);
    const typeLabel = tx(`quicklog.types.${type}`);
    const text = note.trim() || (outcome ? tx('quicklog.autoNoteOutcome', { type: typeLabel, outcome: tx(`quicklog.outcomes.${outcome}`) }) : tx('quicklog.autoNote', { type: typeLabel }));
    setBusy(true);
    const { data, error } = await api.POST('/activities', {
      body: {
        type,
        note: text,
        ...(effectivePerson ? { personId: effectivePerson } : {}),
        ...(effectiveAccount ? { accountId: effectiveAccount } : {}),
        ...(jobOrderId ? { jobOrderId } : {}),
        ...(outcome ? { outcome } : {}),
        ...(type !== 'NOTE' ? { direction } : {}),
        ...(mins !== undefined ? { durationMin: mins } : {}),
      },
    });
    setBusy(false);
    if (error || !data) return setErr(problemText(error, t('shell.saveError')));
    try {
      localStorage.setItem(LAST_TYPE_KEY, type);
    } catch {
      /* ignore */
    }
    const id = data.id;
    onClose();
    onDone?.();
    showUndo({
      message: tx('quicklog.saved'),
      onUndo: async () => {
        const r = await api.DELETE('/activities/{id}', { params: { path: { id } } });
        if (r.error) throw new Error('undo failed');
        onDone?.();
      },
    });
  }

  function onKeyDown(e: KeyboardEvent) {
    if (e.key !== 'Enter' || e.shiftKey || e.nativeEvent.isComposing) return;
    const el = e.target as HTMLElement;
    if (el.tagName === 'BUTTON' || el.getAttribute('aria-expanded') === 'true') return;
    e.preventDefault();
    void submit();
  }

  const showNoDirection = type === 'NOTE';

  return (
    <Modal opened={opened} onClose={onClose} title={tx('quicklog.title')} trapFocus returnFocus>
      <form onSubmit={(e) => (e.preventDefault(), void submit())} onKeyDown={onKeyDown} noValidate>
        <Stack gap="sm">
          <EntityPicker kind="person" label={tx('quicklog.person')} value={person} onChange={(v) => { setPerson(v); setErrs((x) => ({ ...x, target: undefined })); }} error={errs.target} />
          {!accountId && !jobOrderId && <EntityPicker kind="account" label={tx('quicklog.account')} value={account} onChange={(v) => { setAccount(v); setErrs((x) => ({ ...x, target: undefined })); }} />}
          {jobTitle && (
            <Text size="sm">
              {tx('quicklog.search')}: <b>{jobTitle}</b>
            </Text>
          )}
          <Chip.Group multiple={false} value={type} onChange={(v) => setType(v as ActivityType)}>
            <Group gap={6} role="radiogroup" aria-label={tx('quicklog.type')}>
              {TYPES.map((v) => (
                <Chip key={v} value={v} size="xs">
                  {tx(`quicklog.types.${v}`)}
                </Chip>
              ))}
            </Group>
          </Chip.Group>
          <Chip.Group multiple={false} value={outcome ?? ''} onChange={(v) => setOutcome((v || null) as Outcome | null)}>
            <Group gap={6} role="radiogroup" aria-label={tx('quicklog.outcome')}>
              {OUTCOMES.map((v) => (
                <Chip key={v} value={v} size="xs">
                  {tx(`quicklog.outcomes.${v}`)}
                </Chip>
              ))}
            </Group>
          </Chip.Group>
          <Group align="flex-start" gap="md">
            {!showNoDirection && (
              <Chip.Group multiple={false} value={direction} onChange={(v) => v && setDirection(v as Direction)}>
                <Group gap={6} role="radiogroup" aria-label={tx('quicklog.direction')}>
                  {(['OUTBOUND', 'INBOUND'] as Direction[]).map((v) => (
                    <Chip key={v} value={v} size="xs">
                      {tx(`quicklog.${v}`)}
                    </Chip>
                  ))}
                </Group>
              </Chip.Group>
            )}
            <NumberInput label={tx('quicklog.duration')} value={duration} onChange={setDuration} min={0} max={1440} allowDecimal={false} allowNegative={false} w={110} error={errs.duration} />
          </Group>
          <Textarea ref={noteRef} label={tx('quicklog.note')} description={tx('quicklog.noteHint')} value={note} onChange={(e) => setNote(e.currentTarget.value)} rows={2} data-autofocus />
          {err && (
            <Alert color="red" role="alert">
              {err}
            </Alert>
          )}
          <Group justify="space-between">
            <Text size="xs" c="dimmed">
              {tx('quicklog.hint')}
            </Text>
            <Group>
              <Button variant="default" onClick={onClose}>
                {t('shell.cancel')}
              </Button>
              <Button type="submit" loading={busy}>
                {t('shell.save')}
              </Button>
            </Group>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}
