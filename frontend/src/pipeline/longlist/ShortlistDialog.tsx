// Longlist to shortlist needs a rating and a rationale; the contract's refusal is shown inside the dialog.
import { Alert, Button, Group, Modal, Select, Stack, Textarea } from '@mantine/core';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { App } from './useLonglist';

export function ShortlistDialog({ app, onClose, onConfirm }: { app?: App; onClose: () => void; onConfirm: (rating: number | null, rationale: string) => Promise<string[] | undefined> }) {
  const { t } = useTranslation();
  const [rating, setRating] = useState<string | null>(null);
  const [rationale, setRationale] = useState('');
  const [errors, setErrors] = useState<string[]>([]);
  useEffect(() => {
    setRating(app?.rating ? String(app.rating) : null);
    setRationale(app?.rationale ?? '');
    setErrors([]);
  }, [app]);
  const submit = async () => {
    const e = await onConfirm(rating ? Number(rating) : null, rationale);
    if (e) setErrors(e);
    else onClose();
  };
  return (
    <Modal opened={!!app} onClose={onClose} title={t('pipeline.ll.shortlistTitle', { name: app?.person.name })} centered>
      <Stack>
        {errors.length > 0 && (
          <Alert color="red" role="alert" data-testid="shortlist-error">
            {errors.map((m) => (
              <div key={m}>{m}</div>
            ))}
          </Alert>
        )}
        <Select label={t('pipeline.ll.rating')} data={['1', '2', '3', '4', '5']} value={rating} onChange={setRating} clearable />
        <Textarea label={t('pipeline.ll.rationale')} description={t('pipeline.ll.rationaleHint')} value={rationale} onChange={(e) => setRationale(e.currentTarget.value)} autosize minRows={3} />
        <Group justify="flex-end">
          <Button variant="default" onClick={onClose}>
            {t('pipeline.ll.cancel')}
          </Button>
          <Button onClick={() => void submit()}>{t('pipeline.ll.moveToShortlist')}</Button>
        </Group>
      </Stack>
    </Modal>
  );
}
