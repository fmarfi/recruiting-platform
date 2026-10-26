// "+ New person" dialog (F7). Before creating, /duplicates/check runs; a hit shows the "possible duplicate" prompt
// (use existing / create anyway). A duplicate e-mail (409 DUPLICATE) still links to the existing person.
import { Anchor, Button, Group, Modal, Select, Stack, Text, TextInput } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { api } from '../../api';
import { problemText, type Schemas } from '../../pipeline/shared';
import { checkRequest, duplicateDecision, type Match } from '../duplicates/duplicateLogic';
import { DuplicatePrompt } from '../duplicates/DuplicatePrompt';
import { SOURCE_CHANNELS } from './networkFilters';

const TYPES: Schemas['PersonType'][] = ['EXECUTIVE', 'CLIENT_CONTACT', 'REFERRER', 'CANDIDATE'];
const EMPTY = { name: '', type: 'EXECUTIVE' as Schemas['PersonType'], title: '', company: '', city: '', email: '', phone: '', linkedinUrl: '', howWeKnow: '', sourceChannel: '' };

export function NewPersonDialog({ opened, onClose, onCreated }: { opened: boolean; onClose: () => void; onCreated: () => void }) {
  const { t } = useTranslation();
  const [f, setF] = useState(EMPTY);
  const [err, setErr] = useState<{ text: string; existingId?: string }>();
  const [matches, setMatches] = useState<Match[]>();
  const [busy, setBusy] = useState(false);
  const set = (k: keyof typeof f) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setF({ ...f, [k]: e.currentTarget.value });
    setMatches(undefined);
  };
  const orNull = (v: string) => (v.trim() ? v.trim() : null);

  const create = async () => {
    setBusy(true);
    try {
      const r = await api.POST('/people', {
        body: {
          name: f.name.trim(),
          type: f.type,
          title: orNull(f.title),
          company: orNull(f.company),
          city: orNull(f.city),
          email: orNull(f.email),
          phone: orNull(f.phone),
          linkedinUrl: orNull(f.linkedinUrl),
          howWeKnow: orNull(f.howWeKnow),
          ...(f.sourceChannel ? { sourceChannel: f.sourceChannel as Schemas['SourceChannel'] } : {}),
        },
      });
      if (r.error) {
        setMatches(undefined);
        setErr({ text: problemText(r.error, t('network.createFailed')), existingId: (r.error as { existingId?: string }).existingId });
        return;
      }
      setErr(undefined);
      setMatches(undefined);
      setF(EMPTY);
      onCreated();
      onClose();
    } catch {
      setErr({ text: t('network.createFailed') });
    } finally {
      setBusy(false);
    }
  };

  const save = async () => {
    setErr(undefined);
    const req = checkRequest(f);
    if (req) {
      setBusy(true);
      let found: Match[] | undefined;
      try {
        const r = await api.POST('/duplicates/check', { body: req });
        found = r.data;
      } catch {
        found = undefined; // a failed check must not block work; the server still guards the e-mail
      }
      setBusy(false);
      if (duplicateDecision(found) === 'prompt') {
        setMatches(found);
        return;
      }
    }
    await create();
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
        <Group grow>
          <TextInput label={t('network.dup.phone')} value={f.phone} onChange={set('phone')} />
          <TextInput label="LinkedIn" type="url" value={f.linkedinUrl} onChange={set('linkedinUrl')} />
        </Group>
        <Select
          label={t('network.pool.channels')}
          clearable
          value={f.sourceChannel || null}
          onChange={(v) => setF({ ...f, sourceChannel: v ?? '' })}
          data={SOURCE_CHANNELS.map((c) => ({ value: c, label: t(`network.pool.channel.${c}`) }))}
        />
        <TextInput label={t('network.fields.howWeKnow')} value={f.howWeKnow} onChange={set('howWeKnow')} />
        {matches && <DuplicatePrompt matches={matches} busy={busy} onBack={() => setMatches(undefined)} onCreateAnyway={() => void create()} />}
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
          {!matches && (
            <Button disabled={!f.name.trim()} loading={busy} onClick={() => void save()}>
              {t('common.save')}
            </Button>
          )}
        </Group>
      </Stack>
    </Modal>
  );
}
