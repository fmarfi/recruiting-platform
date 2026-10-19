// Result of a board move: confirmation with Undo, or the server's rejection message (with Refresh on a concurrent edit).
import { Alert, Button, Group, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import type { Toast } from '../../board/useBoardMoves';

export function MoveToast({ toast, onClose, onRefresh }: { toast: Toast; onClose: () => void; onRefresh: () => void }) {
  const { t } = useTranslation();
  return (
    <Alert color={toast.kind === 'error' ? 'red' : 'blue'} mb="md" role={toast.kind === 'error' ? 'alert' : 'status'} withCloseButton onClose={onClose}>
      <Group justify="space-between">
        <Text size="sm">{toast.text}</Text>
        <Group gap="xs">
          {toast.undo && (
            <Button size="compact-sm" variant="light" onClick={toast.undo}>
              {t('common.undo')}
            </Button>
          )}
          {toast.refresh && (
            <Button
              size="compact-sm"
              variant="light"
              onClick={() => {
                onClose();
                onRefresh();
              }}
            >
              {t('common.refresh')}
            </Button>
          )}
        </Group>
      </Group>
    </Alert>
  );
}
