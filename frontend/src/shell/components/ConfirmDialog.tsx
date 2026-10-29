import { Button, Group, Modal, Stack, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';

/** In-app confirmation (never window.confirm): names the consequence, Esc or Cancel keeps things as they are, focus lands on Cancel. */
export function ConfirmDialog({
  opened,
  title,
  body,
  confirmLabel,
  danger,
  busy,
  onConfirm,
  onCancel,
}: {
  opened: boolean;
  title: string;
  body?: string;
  confirmLabel: string;
  danger?: boolean;
  busy?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  const { t } = useTranslation();
  return (
    <Modal opened={opened} onClose={onCancel} title={title} centered>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          onConfirm();
        }}
      >
        <Stack>
          {body && <Text size="sm">{body}</Text>}
          <Group justify="flex-end">
            <Button variant="default" onClick={onCancel} data-autofocus>
              {t('shell.cancel')}
            </Button>
            <Button type="submit" color={danger ? 'red' : undefined} loading={busy}>
              {confirmLabel}
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}
