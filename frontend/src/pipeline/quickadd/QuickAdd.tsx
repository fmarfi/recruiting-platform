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
  const verb = f.mode === 'action' ? t('quickAdd.addAction') : t(`quickAdd.logVerb.${f.type}`);
  // The reason Save is disabled, shown next to the button (never a hint at the bottom).
  const reason = !f.canSave && !f.busy && !f.done ? saveReason(f, t) : undefined;
  return (
    <Modal
      opened={opened}
      onClose={onClose}
      title={t('quickAdd.title')}
      size="lg"
      fullScreen={!!phone}
      centered={!phone}
      trapFocus
      onKeyDown={(e) => {
        if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') void f.save();
      }}
    >
      <form
        onSubmit={(e) => {
          e.preventDefault();
          void f.save();
        }}
      >
        <Stack gap="lg">
          <QuickAddFields f={f} context={context} />
          {f.error && (
            <Text c="red" size="sm" role="alert">
              {f.error}
            </Text>
          )}
          <Group justify="space-between" align="center" wrap="nowrap">
            <Text size="xs" style={{ color: 'var(--color-ink3)' }}>
              {t('quickAdd.saveHint')}
            </Text>
            <Group gap="sm" wrap="nowrap" justify="flex-end">
              {reason && (
                <Text size="xs" style={{ color: 'var(--color-ink3)' }}>
                  {reason}
                </Text>
              )}
              <Button variant="default" onClick={onClose}>
                {t('common.cancel')}
              </Button>
              <Button type="submit" disabled={!f.canSave || f.done} loading={f.busy}>
                {verb}
              </Button>
            </Group>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}

type Tr = (key: string) => string;

/** First missing piece, in form order. Empty when Save is enabled. */
function saveReason(f: ReturnType<typeof useQuickAddForm>, t: Tr): string | undefined {
  if (f.mode === 'activity') {
    if (!f.hasContext) return t('quickAdd.needContext');
    if (!f.note.trim()) return t('quickAdd.needNote');
  }
  if (f.mode === 'action' || f.followUp) {
    if (!f.title.trim()) return t('quickAdd.needTitle');
    if (!f.due) return t('quickAdd.needDue');
  }
  return undefined;
}
