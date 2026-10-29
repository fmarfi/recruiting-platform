import { Alert, Button, Group, Modal, Stack, Switch, TextInput } from '@mantine/core';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api';
import { EntityPicker, type Option } from '../../pipeline/quickadd/EntityPicker';
import { FormSection, RequiredNote } from '../../shell/components/FormSection';
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
  const [personErr, setPersonErr] = useState<string>();
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!person) {
      setErr(null);
      return setPersonErr(t('accounts.err.person'));
    }
    setPersonErr(undefined);
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
      <form onSubmit={(e) => void submit(e)} noValidate>
        <Stack>
          <RequiredNote text={t('shell.requiredNote')} />
          <FormSection title={t('accounts.section.who')}>
            <EntityPicker
              kind="person"
              label={t('accounts.pickPerson')}
              description={t('accounts.help.person')}
              error={personErr}
              required
              value={person}
              onChange={(v) => {
                setPerson(v);
                setPersonErr(undefined);
              }}
            />
          </FormSection>
          <FormSection title={t('accounts.section.about')}>
            <TextInput label={t('accounts.contactRole')} description={t('accounts.help.role')} value={role} onChange={(e) => setRole(e.currentTarget.value)} maxLength={200} />
            <Switch label={t('accounts.contactPrimary')} description={t('accounts.help.primary')} checked={primary} onChange={(e) => setPrimary(e.currentTarget.checked)} />
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
