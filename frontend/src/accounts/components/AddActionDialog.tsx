import { Alert, Button, Group, Modal, Stack, TextInput } from '@mantine/core';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api';
import { addDays, todayIso } from '../../shell/lib/format';
import { problemText } from '../../shell/lib/problem';
import { AccountPicker } from './AccountPicker';
import type { DialogProps } from './dialogProps';

export function AddActionDialog({ opened, onClose, accountId, onDone }: DialogProps) {
  const { t } = useTranslation();
  const [title, setTitle] = useState('');
  const [due, setDue] = useState(addDays(todayIso(), 1));
  const [acc, setAcc] = useState<string | null>(accountId ?? null);
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!title.trim() || !due) return setErr(t('accounts.fillRequired'));
    setBusy(true);
    const { error } = await api.POST('/next-actions', { body: { title: title.trim(), dueOn: due, accountId: accountId ?? acc ?? undefined } });
    setBusy(false);
    if (error) return setErr(problemText(error, t('shell.saveError')));
    setTitle('');
    setErr(null);
    onClose();
    onDone?.();
  }

  return (
    <Modal opened={opened} onClose={onClose} title={t('accounts.addAction')}>
      <form onSubmit={(e) => void submit(e)}>
        <Stack>
          <TextInput label={t('accounts.actionTitle')} value={title} onChange={(e) => setTitle(e.currentTarget.value)} required data-autofocus />
          <TextInput label={t('accounts.due')} type="date" value={due} onChange={(e) => setDue(e.currentTarget.value)} required />
          {!accountId && <AccountPicker optional label={`${t('accounts.client')} (${t('accounts.optional')})`} value={acc} onChange={setAcc} />}
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
