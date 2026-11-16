import { Button, Modal, Text } from '@mantine/core';
import { IconHistory } from '@tabler/icons-react';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ICON_SIZE, ICON_STROKE } from '../../shell/components/objectIcons';
import { HistoryPanel, type HistoryEntityType } from '../../shell/edit';

/** Label and value shown like a disabled field, for values that cannot change after creation (kind, position). */
export function ReadOnlyField({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <Text size="sm" fw={500}>
        {label}
      </Text>
      <Text size="sm" c="var(--color-ink2)" mt={4}>
        {value}
      </Text>
    </div>
  );
}

/** State that resets to `initial` each time the drawer opens. */
export function useCreateState<T>(opened: boolean, initial: T) {
  const [v, setV] = useState<T>(initial);
  useEffect(() => {
    if (opened) setV(initial);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [opened]);
  return [v, setV] as const;
}

export function HistoryLink({ onClick }: { onClick: () => void }) {
  const { t } = useTranslation();
  return (
    <Button variant="default" size="compact-md" w="fit-content" leftSection={<IconHistory size={ICON_SIZE} stroke={ICON_STROKE} aria-hidden />} onClick={onClick}>
      {t('agr.history')}
    </Button>
  );
}

/** Revisions of one proposal or agreement, newest first (GET /audit/revisions). */
export function HistoryDialog({ target, onClose }: { target?: { type: HistoryEntityType; id: string; name: string }; onClose: () => void }) {
  const { t } = useTranslation();
  return (
    <Modal opened={!!target} onClose={onClose} size="lg" title={t('agr.historyOf', { name: target?.name ?? '' })}>
      {target && <HistoryPanel entityType={target.type} entityId={target.id} headingOrder={null} />}
    </Modal>
  );
}
