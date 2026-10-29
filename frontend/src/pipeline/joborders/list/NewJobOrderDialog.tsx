import { Button, Group, Modal, Stack, Text } from '@mantine/core';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { AccountPicker } from '../../../accounts/components/AccountPicker';
import { NewJobForm } from './NewJobForm';

/** "New job order" from the list: a job order always belongs to a client, so pick it first, then fill the usual job form. */
export function NewJobOrderDialog({ opened, onClose, onDone }: { opened: boolean; onClose: () => void; onDone: () => void }) {
  const { t } = useTranslation();
  const [client, setClient] = useState<string | null>(null);
  const [step, setStep] = useState<'client' | 'job'>('client');
  const [err, setErr] = useState<string>();
  const close = () => {
    setClient(null);
    setStep('client');
    setErr(undefined);
    onClose();
  };
  const next = (e: FormEvent) => {
    e.preventDefault();
    if (!client) return setErr(t('accounts.err.client'));
    setStep('job');
  };
  return (
    <>
      <Modal opened={opened && step === 'client'} onClose={close} title={t('pipeline.list.newPickClient')} centered>
        <form onSubmit={next}>
          <Stack>
            <Text size="sm" c="dimmed">
              {t('pipeline.list.newPickHint')}
            </Text>
            <AccountPicker label={t('pipeline.list.pickClient')} value={client} onChange={(v) => { setClient(v); setErr(undefined); }} />
            {err && (
              <Text size="sm" c="red" role="alert">
                {err}
              </Text>
            )}
            <Group justify="flex-end">
              <Button variant="default" onClick={close}>
                {t('shell.cancel')}
              </Button>
              <Button type="submit">{t('pipeline.list.continue')}</Button>
            </Group>
          </Stack>
        </form>
      </Modal>
      {client && (
        <NewJobForm
          opened={opened && step === 'job'}
          onClose={close}
          accountId={client}
          onDone={() => {
            onDone();
          }}
        />
      )}
    </>
  );
}
