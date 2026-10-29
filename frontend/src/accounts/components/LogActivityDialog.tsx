import { Alert, Button, Group, Modal, Select, Stack, Textarea } from '@mantine/core';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { api, type components } from '../../api';
import { FormSection, RequiredNote } from '../../shell/components/FormSection';
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
  const [errs, setErrs] = useState<{ client?: string; note?: string }>({});
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const types = ref.data?.vocabularies.activityType ?? ['CALL', 'VISIT', 'MEETING', 'EMAIL', 'NOTE'];

  async function submit(e?: FormEvent) {
    e?.preventDefault();
    const target = accountId ?? acc ?? undefined;
    const next = { client: target ? undefined : t('accounts.err.client'), note: note.trim() ? undefined : t('accounts.err.note') };
    setErrs(next);
    if (next.client || next.note) return setErr(null);
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
    setErrs({});
    setErr(null);
    onClose();
    onDone?.();
  }

  return (
    <Modal opened={opened} onClose={onClose} title={t('accounts.logActivity')}>
      <form onSubmit={(e) => void submit(e)} onKeyDown={(e) => (e.ctrlKey || e.metaKey) && e.key === 'Enter' && (e.preventDefault(), void submit())} noValidate>
        <Stack>
          <RequiredNote text={t('shell.requiredNote')} />
          <FormSection title={t('accounts.section.what')}>
          {!accountId && <AccountPicker label={t('accounts.client')} value={acc} onChange={(v) => { setAcc(v); setErrs((x) => ({ ...x, client: undefined })); }} error={errs.client} />}
          <Select
            label={t('accounts.activityType')}
            data={types.map((v) => ({ value: v, label: t(`accounts.activity.${v}`) }))}
            value={type}
            onChange={(v) => v && setType(v as ActivityType)}
            allowDeselect={false}
          />
          <Textarea label={t('accounts.note')} description={t('accounts.help.note')} value={note} onChange={(e) => { setNote(e.currentTarget.value); setErrs((x) => ({ ...x, note: undefined })); }} error={errs.note} autosize minRows={3} withAsterisk data-autofocus />
          </FormSection>
          {err && <Alert color="red" role="alert">{err}</Alert>}
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
