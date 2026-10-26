// Create a person from an intake item: the detected e-mail, phone and LinkedIn URL are suggestions only; the name is typed.
import { Alert, Button, Group, Modal, Select, Stack, TextInput } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { Schemas } from '../../pipeline/shared';
import { SOURCE_CHANNELS } from '../list/networkFilters';
import { prefillFromDetected } from './intakeLogic';

export function CreatePersonDialog({
  item,
  error,
  busy,
  onClose,
  onCreate,
}: {
  item: Schemas['CvIntakeItem'] | undefined;
  error?: string;
  busy: boolean;
  onClose: () => void;
  onCreate: (person: Schemas['PersonCreateRequest']) => void;
}) {
  const { t } = useTranslation();
  return (
    <Modal opened={!!item} onClose={onClose} title={t('network.intake.createTitle')} centered>
      {item && <Form key={item.id} item={item} error={error} busy={busy} onClose={onClose} onCreate={onCreate} />}
    </Modal>
  );
}

function Form({ item, error, busy, onClose, onCreate }: { item: Schemas['CvIntakeItem']; error?: string; busy: boolean; onClose: () => void; onCreate: (p: Schemas['PersonCreateRequest']) => void }) {
  const { t } = useTranslation();
  const pre = prefillFromDetected(item.detected);
  const [f, setF] = useState({ name: '', title: '', company: '', city: '', ...pre, sourceChannel: item.sourceChannel ?? 'CV_UPLOAD' });
  const set = (k: keyof typeof f) => (e: React.ChangeEvent<HTMLInputElement>) => setF({ ...f, [k]: e.currentTarget.value });
  const orNull = (v: string) => (v.trim() ? v.trim() : null);
  return (
    <Stack>
      <Alert color="blue" variant="light">
        {t('network.intake.prefillHint')}
      </Alert>
      <TextInput label={t('network.fields.name')} required value={f.name} onChange={set('name')} data-autofocus />
      <Group grow>
        <TextInput label={t('network.fields.title')} value={f.title} onChange={set('title')} />
        <TextInput label={t('network.fields.company')} value={f.company} onChange={set('company')} />
      </Group>
      <Group grow>
        <TextInput label={t('network.fields.email')} type="email" value={f.email} onChange={set('email')} />
        <TextInput label={t('network.dup.phone')} value={f.phone} onChange={set('phone')} />
      </Group>
      <TextInput label="LinkedIn" type="url" value={f.linkedinUrl} onChange={set('linkedinUrl')} />
      <Select
        label={t('network.pool.channels')}
        allowDeselect={false}
        value={f.sourceChannel}
        onChange={(v) => v && setF({ ...f, sourceChannel: v as Schemas['SourceChannel'] })}
        data={SOURCE_CHANNELS.map((c) => ({ value: c, label: t(`network.pool.channel.${c}`) }))}
      />
      {error && (
        <Alert color="red" role="alert">
          {error}
        </Alert>
      )}
      <Group justify="flex-end">
        <Button variant="default" onClick={onClose}>
          {t('common.cancel')}
        </Button>
        <Button
          disabled={!f.name.trim()}
          loading={busy}
          onClick={() =>
            onCreate({
              name: f.name.trim(),
              type: 'CANDIDATE',
              title: orNull(f.title),
              company: orNull(f.company),
              city: orNull(f.city),
              email: orNull(f.email),
              phone: orNull(f.phone),
              linkedinUrl: orNull(f.linkedinUrl),
              sourceChannel: f.sourceChannel as Schemas['SourceChannel'],
            })
          }
        >
          {t('network.intake.createConfirm')}
        </Button>
      </Group>
    </Stack>
  );
}
