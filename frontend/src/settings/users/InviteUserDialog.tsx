import { Button, Group, Modal, PasswordInput, Select, Stack, Text, TextInput } from '@mantine/core';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api';
import { FormSection, RequiredNote } from '../../shell/components/FormSection';
import { problemText } from '../../shell/lib/problem';

type Role = 'CONSULTANT' | 'MANAGER' | 'ADMIN';
const EMPTY = { name: '', email: '', role: 'CONSULTANT' as Role, initialPassword: '' };

/** Add user (admin): name, e-mail, role, initial password. Field errors show next to the field; server errors (weak password, duplicate e-mail) show above the buttons. */
export function InviteUserDialog({ opened, onClose, onCreated }: { opened: boolean; onClose: () => void; onCreated: () => void }) {
  const { t, i18n } = useTranslation();
  const [f, setF] = useState(EMPTY);
  const [errs, setErrs] = useState<{ name?: string; email?: string; initialPassword?: string }>({});
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const set = (k: keyof typeof f) => (v: string) => {
    setF((s) => ({ ...s, [k]: v }));
    setErrs((e) => ({ ...e, [k]: undefined }));
  };

  async function submit(e: FormEvent) {
    e.preventDefault();
    const next = {
      name: f.name.trim() ? undefined : t('settings.users.err.name'),
      email: /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(f.email.trim()) ? undefined : t('settings.users.err.email'),
      initialPassword: f.initialPassword.length >= 8 ? undefined : t('settings.users.err.password'),
    };
    setErrs(next);
    if (next.name || next.email || next.initialPassword) return setErr(null);
    setBusy(true);
    const { error } = await api.POST('/users', {
      body: { ...f, name: f.name.trim(), email: f.email.trim(), language: i18n.language.startsWith('tr') ? 'TR' : 'EN' },
    });
    setBusy(false);
    if (error) return setErr(problemText(error, t('shell.saveError')));
    setF(EMPTY);
    setErrs({});
    setErr(null);
    onCreated();
    onClose();
  }

  return (
    <Modal opened={opened} onClose={onClose} title={t('settings.users.invite')}>
      <form onSubmit={(e) => void submit(e)} noValidate>
        <Stack>
          <RequiredNote text={t('shell.requiredNote')} />
          <FormSection title={t('settings.users.section.person')}>
            <TextInput withAsterisk label={t('settings.users.name')} value={f.name} error={errs.name} onChange={(e) => set('name')(e.currentTarget.value)} data-autofocus />
            <TextInput withAsterisk type="email" label={t('settings.users.email')} description={t('settings.users.help.email')} value={f.email} error={errs.email} onChange={(e) => set('email')(e.currentTarget.value)} />
          </FormSection>
          <FormSection title={t('settings.users.section.access')}>
            <Select
              label={t('settings.users.role')}
              description={t('settings.users.help.role')}
              allowDeselect={false}
              data={['CONSULTANT', 'MANAGER', 'ADMIN'].map((r) => ({ value: r, label: t(`settings.role.${r}`) }))}
              value={f.role}
              onChange={(v) => v && set('role')(v)}
            />
            <PasswordInput withAsterisk label={t('settings.users.password')} description={t('settings.users.help.password')} value={f.initialPassword} error={errs.initialPassword} onChange={(e) => set('initialPassword')(e.currentTarget.value)} />
          </FormSection>
          {err && <Text c="red" role="alert">{err}</Text>}
          <Group justify="flex-end">
            <Button variant="default" onClick={onClose}>{t('shell.cancel')}</Button>
            <Button type="submit" loading={busy}>{t('shell.save')}</Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}
