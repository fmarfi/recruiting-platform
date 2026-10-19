import { Alert, Button, Group, Modal, Select, Stack, Textarea } from '@mantine/core';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { api, type components } from '../../api';
import { problemText } from '../../shell/lib/problem';
import { useReference } from '../../shell/refdata/useReference';
import { AccountPicker } from './AccountPicker';
import type { DialogProps } from './dialogProps';

type ActivityType = components['schemas']['ActivityType'];
const LAST_TYPE_KEY = 'quickadd.lastType';

function remembered(): ActivityType {
  try {
    const v = localStorage.getItem(LAST_TYPE_KEY);
    if (v === 'CALL' || v === 'VISIT' || v === 'MEETING' || v === 'EMAIL' || v === 'NOTE') return v;
  } catch {
    /* storage unavailable */
  }
  return 'CALL';
}


export function LogActivityDialog({ opened, onClose, accountId, onDone }: DialogProps) {
  const { t } = useTranslation();
  const ref = useReference();
  const [type, setType] = useState<ActivityType>(remembered());
  const [note, setNote] = useState('');
  const [acc, setAcc] = useState<string | null>(accountId ?? null);
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const types = ref.data?.vocabularies.activityType ?? ['CALL', 'VISIT', 'MEETING', 'EMAIL', 'NOTE'];

  async function submit(e?: FormEvent) {
    e?.preventDefault();
    const target = accountId ?? acc;
    if (!target || !note.trim()) return setErr(t('accounts.fillRequired'));
    setBusy(true);
    const { error } = await api.POST('/activities', { body: { type, note: note.trim(), accountId: target } });
    setBusy(false);
    if (error) return setErr(problemText(error, t('shell.saveError')));
    try {
      localStorage.setItem(LAST_TYPE_KEY, type);
    } catch {
      /* ignore */
    }
    setNote('');
    setErr(null);
    onClose();
    onDone?.();
  }

  return (
    <Modal opened={opened} onClose={onClose} title={t('accounts.logActivity')}>
      <form onSubmit={(e) => void submit(e)} onKeyDown={(e) => (e.ctrlKey || e.metaKey) && e.key === 'Enter' && void submit()}>
        <Stack>
          {!accountId && <AccountPicker label={t('accounts.client')} value={acc} onChange={setAcc} />}
          <Select
            label={t('accounts.activityType')}
            data={types.map((v) => ({ value: v, label: t(`accounts.activity.${v}`) }))}
            value={type}
            onChange={(v) => v && setType(v as ActivityType)}
            allowDeselect={false}
          />
          <Textarea label={t('accounts.note')} value={note} onChange={(e) => setNote(e.currentTarget.value)} autosize minRows={3} required data-autofocus />
          {err && <Alert color="red">{err}</Alert>}
          <Group justify="flex-end">
            <Button variant="default" onClick={onClose}>
              {t('shell.cancel')}
            </Button>
            <Button type="submit" loading={busy}>
              {t('shell.save')}
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}
