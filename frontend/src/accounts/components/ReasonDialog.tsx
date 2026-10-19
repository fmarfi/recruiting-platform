import { Button, Group, Modal, Stack, Textarea } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

/** Asks for the reason of a status move flagged reasonRequired. */
export function ReasonDialog({ opened, target, onConfirm, onCancel }: { opened: boolean; target: string; onConfirm: (reason: string) => void; onCancel: () => void }) {
  const { t } = useTranslation();
  const [reason, setReason] = useState('');
  return (
    <Modal opened={opened} onClose={onCancel} title={t('accounts.reasonTitle', { status: target })}>
      <Stack>
        <Textarea label={t('accounts.reason')} value={reason} onChange={(e) => setReason(e.currentTarget.value)} required data-autofocus />
        <Group justify="flex-end">
          <Button variant="default" onClick={onCancel}>
            {t('shell.cancel')}
          </Button>
          <Button
            disabled={!reason.trim()}
            onClick={() => {
              onConfirm(reason.trim());
              setReason('');
            }}
          >
            {t('shell.save')}
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
