import { Button, Group, Modal, PasswordInput, Select, Stack, Text, TextInput } from '@mantine/core';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api';
import { problemText } from '../../shell/lib/problem';

type Role = 'CONSULTANT' | 'MANAGER' | 'ADMIN';
const EMPTY = { name: '', email: '', role: 'CONSULTANT' as Role, initialPassword: '' };

/** Add user (admin): name, e-mail, role, initial password. Server errors (weak password, duplicate e-mail) show inline. */
export function InviteUserDialog({ opened, onClose, onCreated }: { opened: boolean; onClose: () => void; onCreated: () => void }) {
  const { t, i18n } = useTranslation();
  const [f, setF] = useState(EMPTY);
  const [err, setErr] = useState<string | null>(null);
  const set = (k: keyof typeof f) => (v: string) => setF((s) => ({ ...s, [k]: v }));

  async function submit(e: FormEvent) {
    e.preventDefault();
    const { error } = await api.POST('/users', {
      body: { ...f, name: f.name.trim(), email: f.email.trim(), language: i18n.language.startsWith('tr') ? 'TR' : 'EN' },
    });
    if (error) return setErr(problemText(error, t('shell.saveError')));
    setF(EMPTY);
    setErr(null);
    onCreated();
    onClose();
  }

  return (
    <Modal opened={opened} onClose={onClose} title={t('settings.users.invite')}>
      <form onSubmit={(e) => void submit(e)}>
        <Stack>
          <TextInput required label={t('settings.users.name')} value={f.name} onChange={(e) => set('name')(e.currentTarget.value)} />
          <TextInput required type="email" label={t('settings.users.email')} value={f.email} onChange={(e) => set('email')(e.currentTarget.value)} />
          <Select
            label={t('settings.users.role')}
            allowDeselect={false}
            data={['CONSULTANT', 'MANAGER', 'ADMIN'].map((r) => ({ value: r, label: t(`settings.role.${r}`) }))}
            value={f.role}
            onChange={(v) => v && set('role')(v)}
          />
          <PasswordInput required label={t('settings.users.password')} value={f.initialPassword} onChange={(e) => set('initialPassword')(e.currentTarget.value)} />
          {err && <Text c="red" role="alert">{err}</Text>}
          <Group justify="flex-end">
            <Button variant="default" onClick={onClose}>{t('shell.cancel')}</Button>
            <Button type="submit">{t('shell.save')}</Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}
