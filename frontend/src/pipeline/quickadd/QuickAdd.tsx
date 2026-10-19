import { Button, Group, Modal, Stack, Text } from '@mantine/core';
import { useMediaQuery } from '@mantine/hooks';
import { useTranslation } from 'react-i18next';
import { QuickAddFields } from './QuickAddFields';
import { useQuickAddForm, type QuickAddContext } from './quickAddState';

export type { QuickAddContext };

export type QuickAddProps = {
  opened: boolean;
  onClose: () => void;
  /** Pre-filled context; the pickers can change account and person. */
  context?: QuickAddContext;
  /** `activity` logs a call, visit or note; `action` creates a next action. */
  initialMode?: 'activity' | 'action';
  /** Called after a successful save so the host screen can reload its timeline. */
  onSaved?: () => void;
};

/**
 * Quick add (UX-06): log a call, visit, meeting, e-mail or note, or create a next action, in under 30 seconds.
 * Controlled by the host (account, person and job pages, or the top bar of frontend-shell).
 */
export function QuickAdd({ opened, onClose, context, initialMode = 'activity', onSaved }: QuickAddProps) {
  const { t } = useTranslation();
  const phone = useMediaQuery('(max-width: 48em)');
  const f = useQuickAddForm(opened, initialMode, context, onSaved, onClose);
  return (
    <Modal
      opened={opened}
      onClose={onClose}
      title={t('quickAdd.title')}
      fullScreen={!!phone}
      centered={!phone}
      trapFocus
      onKeyDown={(e) => {
        if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') void f.save();
      }}
    >
      <Stack>
        <QuickAddFields f={f} context={context} />
        {f.error && (
          <Text c="red" size="sm" role="alert">
            {f.error}
          </Text>
        )}
        <Group justify="flex-end">
          <Button variant="default" onClick={onClose}>
            {t('common.cancel')}
          </Button>
          <Button onClick={() => void f.save()} disabled={!f.canSave || f.done} loading={f.busy}>
            {t('quickAdd.save')}
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
