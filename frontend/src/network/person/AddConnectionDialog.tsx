// Add connection (A-13): pick another person, choose the link type; shown on both people.
import { Button, Group, Modal, Select, Stack, Text, TextInput } from '@mantine/core';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api';
import { problemText, type Schemas } from '../../pipeline/shared';

const LINKS: Schemas['LinkType'][] = ['KNOWS', 'WORKED_WITH', 'INTRODUCED', 'REFERRED', 'REPORTS_TO'];

export function AddConnectionDialog({ personId, opened, onClose, onSaved }: { personId: string; opened: boolean; onClose: () => void; onSaved: () => void }) {
  const { t } = useTranslation();
  const [q, setQ] = useState('');
  const [options, setOptions] = useState<{ value: string; label: string }[]>([]);
  const [other, setOther] = useState<string | null>(null);
  const [type, setType] = useState<Schemas['LinkType']>('KNOWS');
  const [note, setNote] = useState('');
  const [err, setErr] = useState<string>();
  useEffect(() => {
    if (!opened || q.trim().length < 2) return;
    let live = true;
    api
      .GET('/people', { params: { query: { q, size: 8 } } })
      .then((r) => live && setOptions((r.data?.content ?? []).filter((p) => p.id !== personId).map((p) => ({ value: p.id, label: p.name }))))
      .catch(() => undefined);
    return () => {
      live = false;
    };
  }, [q, opened, personId]);
  const save = async () => {
    if (!other) return;
    const r = await api.POST('/network-links', {
      body: { fromPersonId: personId, toPersonId: other, type, note: note.trim() || null },
    });
    if (r.error) setErr(problemText(r.error, t('network.linkFailed')));
    else {
      onSaved();
      onClose();
    }
  };
  return (
    <Modal opened={opened} onClose={onClose} title={t('network.addConnection')} centered>
      <Stack>
        <Select label={t('network.person')} searchable data={options} value={other} onChange={setOther} onSearchChange={setQ} filter={({ options: o }) => o} />
        <Select
          label={t('network.linkType')}
          allowDeselect={false}
          data={LINKS.map((l) => ({ value: l, label: t(`network.linkTypes.${l}`) }))}
          value={type}
          onChange={(v) => v && setType(v as Schemas['LinkType'])}
        />
        <TextInput label={t('network.note')} value={note} onChange={(e) => setNote(e.currentTarget.value)} />
        {err && (
          <Text c="red" size="sm" role="alert">
            {err}
          </Text>
        )}
        <Group justify="flex-end">
          <Button variant="default" onClick={onClose}>
            {t('common.cancel')}
          </Button>
          <Button disabled={!other} onClick={() => void save()}>
            {t('common.save')}
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
