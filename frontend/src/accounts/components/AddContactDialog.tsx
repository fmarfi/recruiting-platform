import { Alert, Button, Group, Modal, Stack, Switch, TextInput } from '@mantine/core';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api';
import { EntityPicker, type Option } from '../../pipeline/quickadd/EntityPicker';
import { problemText } from '../../shell/lib/problem';

interface Props {
  opened: boolean;
  onClose: () => void;
  accountId: string;
  onDone: () => void;
}

/** Link an existing person to the account as a contact (A-03). */
export function AddContactDialog({ opened, onClose, accountId, onDone }: Props) {
  const { t } = useTranslation();
  const [person, setPerson] = useState<Option | null>(null);
  const [role, setRole] = useState('');
  const [primary, setPrimary] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!person) return setErr(t('accounts.fillRequired'));
    setBusy(true);
    const { error } = await api.POST('/accounts/{id}/contacts', {
      params: { path: { id: accountId } },
      body: { personId: person.value, role: role.trim() || null, primary },
    });
    setBusy(false);
    if (error) return setErr(problemText(error, t('shell.saveError')));
    setPerson(null);
    setRole('');
    setPrimary(false);
    setErr(null);
    onClose();
    onDone();
  }

  return (
    <Modal opened={opened} onClose={onClose} title={t('accounts.addContact')}>
      <form onSubmit={(e) => void submit(e)}>
        <Stack>
          <EntityPicker kind="person" label={t('accounts.pickPerson')} value={person} onChange={setPerson} />
          <TextInput label={t('accounts.contactRole')} value={role} onChange={(e) => setRole(e.currentTarget.value)} maxLength={200} />
          <Switch label={t('accounts.contactPrimary')} checked={primary} onChange={(e) => setPrimary(e.currentTarget.checked)} />
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
