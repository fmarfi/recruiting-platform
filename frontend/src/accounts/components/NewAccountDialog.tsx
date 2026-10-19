import { Alert, Button, Group, Modal, Stack, TextInput } from '@mantine/core';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api';
import { problemText } from '../../shell/lib/problem';
import { useReference } from '../../shell/refdata/useReference';

export function NewAccountDialog({ opened, onClose, onCreated }: { opened: boolean; onClose: () => void; onCreated: (id: string) => void }) {
  const { t } = useTranslation();
  const ref = useReference();
  const [name, setName] = useState('');
  const [sector, setSector] = useState('');
  const [city, setCity] = useState('');
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!name.trim() || !sector.trim()) return setErr(t('accounts.fillRequired'));
    setBusy(true);
    const { data, error, response } = await api.POST('/accounts', { body: { name: name.trim(), sector: sector.trim(), city: city.trim() || null } });
    setBusy(false);
    if (error || !data) return setErr(response?.status === 409 ? t('accounts.duplicate') : problemText(error, t('shell.saveError')));
    setErr(null);
    onClose();
    onCreated(data.id);
  }

  return (
    <Modal opened={opened} onClose={onClose} title={t('accounts.newAccount')}>
      <form onSubmit={(e) => void submit(e)}>
        <Stack>
          <TextInput label={t('accounts.name')} value={name} onChange={(e) => setName(e.currentTarget.value)} required data-autofocus />
          <TextInput label={t('accounts.sector')} list="sector-options" value={sector} onChange={(e) => setSector(e.currentTarget.value)} required />
          <datalist id="sector-options">
            {(ref.data?.sectors ?? []).map((s) => (
              <option key={s} value={s} />
            ))}
          </datalist>
          <TextInput label={t('accounts.city')} value={city} onChange={(e) => setCity(e.currentTarget.value)} />
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
