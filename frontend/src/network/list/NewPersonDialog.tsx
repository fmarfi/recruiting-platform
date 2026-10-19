// "+ New person" dialog (F7). A duplicate e-mail (409 DUPLICATE) links to the existing person.
import { Anchor, Button, Group, Modal, Select, Stack, Text, TextInput } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { api } from '../../api';
import { problemText, type Schemas } from '../../pipeline/shared';

const TYPES: Schemas['PersonType'][] = ['EXECUTIVE', 'CLIENT_CONTACT', 'REFERRER', 'CANDIDATE'];

export function NewPersonDialog({ opened, onClose, onCreated }: { opened: boolean; onClose: () => void; onCreated: () => void }) {
  const { t } = useTranslation();
  const [f, setF] = useState({ name: '', type: 'EXECUTIVE' as Schemas['PersonType'], title: '', company: '', city: '', email: '', howWeKnow: '' });
  const [err, setErr] = useState<{ text: string; existingId?: string }>();
  const set = (k: keyof typeof f) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => setF({ ...f, [k]: e.currentTarget.value });
  const orNull = (v: string) => (v.trim() ? v.trim() : null);

  const save = async () => {
    const r = await api.POST('/people', {
      body: {
        name: f.name.trim(),
        type: f.type,
        title: orNull(f.title),
        company: orNull(f.company),
        city: orNull(f.city),
        email: orNull(f.email),
        howWeKnow: orNull(f.howWeKnow),
      },
    });
    if (r.error) {
      setErr({ text: problemText(r.error, t('network.createFailed')), existingId: (r.error as { existingId?: string }).existingId });
      return;
    }
    setErr(undefined);
    onCreated();
    onClose();
  };

  return (
    <Modal opened={opened} onClose={onClose} title={t('network.newPerson')} centered>
      <Stack>
        <TextInput label={t('network.fields.name')} required value={f.name} onChange={set('name')} data-autofocus />
        <Select
          label={t('network.fields.type')}
          allowDeselect={false}
          data={TYPES.map((x) => ({ value: x, label: t(`network.types.${x}`) }))}
          value={f.type}
          onChange={(v) => v && setF({ ...f, type: v as Schemas['PersonType'] })}
        />
        <Group grow>
          <TextInput label={t('network.fields.title')} value={f.title} onChange={set('title')} />
          <TextInput label={t('network.fields.company')} value={f.company} onChange={set('company')} />
        </Group>
        <Group grow>
          <TextInput label={t('network.fields.city')} value={f.city} onChange={set('city')} />
          <TextInput label={t('network.fields.email')} type="email" value={f.email} onChange={set('email')} />
        </Group>
        <TextInput label={t('network.fields.howWeKnow')} value={f.howWeKnow} onChange={set('howWeKnow')} />
        {err && (
          <Text c="red" size="sm" role="alert">
            {err.text}{' '}
            {err.existingId && (
              <Anchor component={Link} to={`/people/${err.existingId}`}>
                {t('network.openExisting')}
              </Anchor>
            )}
          </Text>
        )}
        <Group justify="flex-end">
          <Button variant="default" onClick={onClose}>
            {t('common.cancel')}
          </Button>
          <Button disabled={!f.name.trim()} onClick={() => void save()}>
            {t('common.save')}
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
