// Create form of a search with the full brief: description and fee terms are kept (the short account-page dialog drops them).
import { Alert, Button, Group, Modal, NumberInput, Stack, Textarea, TextInput } from '@mantine/core';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../../api';
import { fieldErrors, orNull } from '../../../accounts/detail/editSupport';
import { problemText } from '../../shared';

interface Props {
  accountId: string;
  opened: boolean;
  onClose: () => void;
  onDone: () => void;
}

const EMPTY = { title: '', location: '', seniority: '', description: '', feeTerms: '' };

export function NewJobForm({ accountId, opened, onClose, onDone }: Props) {
  const { t } = useTranslation();
  const [v, setV] = useState(EMPTY);
  const [openings, setOpenings] = useState<number | string>(1);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [err, setErr] = useState<string>();
  const [busy, setBusy] = useState(false);
  const set = (k: keyof typeof v, val: string) => {
    setV((s) => ({ ...s, [k]: val }));
    setErrors((e) => ({ ...e, [k]: '' }));
  };
  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!v.title.trim()) return setErrors({ title: t('pipeline.edit.titleRequired') });
    setBusy(true);
    setErr(undefined);
    const { error } = await api.POST('/job-orders', {
      body: { accountId, title: v.title.trim(), location: orNull(v.location), seniority: orNull(v.seniority), description: orNull(v.description), feeTerms: orNull(v.feeTerms), openings: Number(openings) || 1 },
    });
    setBusy(false);
    if (error) {
      const fe = fieldErrors(error);
      setErrors(fe);
      return setErr(Object.keys(fe).length ? t('accounts.edit.fixFields') : problemText(error, t('shell.saveError')));
    }
    setV(EMPTY);
    setOpenings(1);
    onClose();
    onDone();
  };
  const text = (k: keyof typeof v, label: string, extra: object = {}) => (
    <TextInput label={label} value={v[k]} onChange={(e) => set(k, e.currentTarget.value)} error={errors[k] || undefined} {...extra} />
  );
  return (
    <Modal opened={opened} onClose={onClose} title={t('pipeline.edit.newTitle')} size="lg">
      <form onSubmit={(e) => void submit(e)} noValidate>
        <Stack>
          {text('title', t('pipeline.edit.title'), { withAsterisk: true, maxLength: 200, 'data-autofocus': true })}
          <Group grow align="flex-start">
            {text('location', t('pipeline.hl.location'), { maxLength: 200 })}
            {text('seniority', t('pipeline.hl.seniority'), { maxLength: 100 })}
            <NumberInput label={t('pipeline.edit.openings')} min={1} allowDecimal={false} value={openings} onChange={setOpenings} error={errors.openings || undefined} />
          </Group>
          <Textarea label={t('pipeline.edit.description')} value={v.description} onChange={(e) => set('description', e.currentTarget.value)} error={errors.description || undefined} autosize minRows={3} maxRows={10} maxLength={10000} />
          <Textarea label={t('pipeline.fee')} description={t('pipeline.edit.feeHint')} value={v.feeTerms} onChange={(e) => set('feeTerms', e.currentTarget.value)} error={errors.feeTerms || undefined} autosize minRows={2} maxLength={2000} />
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
