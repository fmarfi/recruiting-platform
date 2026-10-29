import { Alert, Button, Group, Modal, Stack, TextInput } from '@mantine/core';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api';
import { FormSection, RequiredNote } from '../../shell/components/FormSection';
import { problemText } from '../../shell/lib/problem';
import { useReference } from '../../shell/refdata/useReference';

export function NewAccountDialog({ opened, onClose, onCreated }: { opened: boolean; onClose: () => void; onCreated: (id: string) => void }) {
  const { t } = useTranslation();
  const ref = useReference();
  const [name, setName] = useState('');
  const [sector, setSector] = useState('');
  const [city, setCity] = useState('');
  const [errs, setErrs] = useState<{ name?: string; sector?: string }>({});
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    const next = { name: name.trim() ? undefined : t('accounts.err.name'), sector: sector.trim() ? undefined : t('accounts.err.sector') };
    setErrs(next);
    if (next.name || next.sector) return setErr(null);
    setBusy(true);
    const { data, error, response } = await api.POST('/accounts', { body: { name: name.trim(), sector: sector.trim(), city: city.trim() || null } });
    setBusy(false);
    if (response?.status === 409) return setErrs({ name: t('accounts.duplicate') });
    if (error || !data) return setErr(problemText(error, t('shell.saveError')));
    setErr(null);
    setErrs({});
    onClose();
    onCreated(data.id);
  }

  return (
    <Modal opened={opened} onClose={onClose} title={t('accounts.newAccount')}>
      <form onSubmit={(e) => void submit(e)} noValidate>
        <Stack>
          <RequiredNote text={t('shell.requiredNote')} />
          <FormSection title={t('accounts.section.company')}>
            <TextInput
              label={t('accounts.name')}
              description={t('accounts.help.name')}
              value={name}
              onChange={(e) => {
                setName(e.currentTarget.value);
                setErrs((x) => ({ ...x, name: undefined }));
              }}
              error={errs.name}
              withAsterisk
              data-autofocus
            />
            <TextInput
              label={t('accounts.sector')}
              description={t('accounts.help.sector')}
              list="sector-options"
              value={sector}
              onChange={(e) => {
                setSector(e.currentTarget.value);
                setErrs((x) => ({ ...x, sector: undefined }));
              }}
              error={errs.sector}
              withAsterisk
            />
            <datalist id="sector-options">
              {(ref.data?.sectors ?? []).map((s) => (
                <option key={s} value={s} />
              ))}
            </datalist>
          </FormSection>
          <FormSection title={t('accounts.section.location')}>
            <TextInput label={t('accounts.city')} description={t('accounts.help.city')} value={city} onChange={(e) => setCity(e.currentTarget.value)} />
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
