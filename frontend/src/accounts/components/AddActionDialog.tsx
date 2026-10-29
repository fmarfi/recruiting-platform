import { Alert, Button, Group, Modal, Stack, TextInput } from '@mantine/core';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api';
import { FormSection, RequiredNote } from '../../shell/components/FormSection';
import { addDays, todayIso } from '../../shell/lib/format';
import { problemText } from '../../shell/lib/problem';
import { AccountPicker } from './AccountPicker';
import type { DialogProps } from './dialogProps';

export function AddActionDialog({ opened, onClose, accountId, onDone }: DialogProps) {
  const { t } = useTranslation();
  const [title, setTitle] = useState('');
  const [due, setDue] = useState(addDays(todayIso(), 1));
  const [acc, setAcc] = useState<string | null>(accountId ?? null);
  const [errs, setErrs] = useState<{ title?: string; due?: string }>({});
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    const next = { title: title.trim() ? undefined : t('accounts.err.title'), due: due ? undefined : t('accounts.err.due') };
    setErrs(next);
    if (next.title || next.due) return setErr(null);
    setBusy(true);
    const { error } = await api.POST('/next-actions', { body: { title: title.trim(), dueOn: due, accountId: accountId ?? acc ?? undefined } });
    setBusy(false);
    if (error) return setErr(problemText(error, t('shell.saveError')));
    setTitle('');
    setErrs({});
    setErr(null);
    onClose();
    onDone?.();
  }

  return (
    <Modal opened={opened} onClose={onClose} title={t('accounts.addAction')}>
      <form onSubmit={(e) => void submit(e)} noValidate>
        <Stack>
          <RequiredNote text={t('shell.requiredNote')} />
          <FormSection title={t('accounts.section.what')}>
            <TextInput
              label={t('accounts.actionTitle')}
              description={t('accounts.help.actionTitle')}
              value={title}
              onChange={(e) => {
                setTitle(e.currentTarget.value);
                setErrs((x) => ({ ...x, title: undefined }));
              }}
              error={errs.title}
              withAsterisk
              data-autofocus
            />
            {!accountId && <AccountPicker optional label={`${t('accounts.client')} (${t('accounts.optional')})`} value={acc} onChange={setAcc} />}
          </FormSection>
          <FormSection title={t('accounts.section.when')}>
            <TextInput
              label={t('accounts.due')}
              description={t('accounts.help.due')}
              type="date"
              value={due}
              onChange={(e) => {
                setDue(e.currentTarget.value);
                setErrs((x) => ({ ...x, due: undefined }));
              }}
              error={errs.due}
              withAsterisk
            />
          </FormSection>
          {err && (
            <Alert color="red" role="alert">
              {err}
            </Alert>
          )}
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
