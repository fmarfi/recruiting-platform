// Form fields for Quick add (UX-06).
import { Group, SegmentedControl, Stack, Switch, Text, TextInput, Textarea } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { RequiredNote } from '../../shell/components/FormSection';
import { EntityPicker } from './EntityPicker';
import { TYPES, nowLocal, type ActivityType, type QuickAddContext, type useQuickAddForm } from './quickAddState';

type Form = ReturnType<typeof useQuickAddForm>;

export function QuickAddFields({ f, context }: { f: Form; context?: QuickAddContext }) {
  const { t } = useTranslation();
  const [touched, setTouched] = useState<Record<string, boolean>>({});
  const touch = (k: string) => () => setTouched((x) => ({ ...x, [k]: true }));
  const titleInput = (autoFocus: boolean) => (
    <TextInput
      label={t('quickAdd.actionTitle')}
      description={t('accounts.help.actionTitle')}
      withAsterisk
      value={f.title}
      onChange={(e) => f.setTitle(e.currentTarget.value)}
      onBlur={touch('title')}
      error={touched.title && !f.title.trim() ? t('accounts.err.title') : undefined}
      maxLength={300}
      data-autofocus={autoFocus || undefined}
    />
  );
  return (
    <Stack>
      <RequiredNote text={t('shell.requiredNote')} />
      <SegmentedControl
        fullWidth
        aria-label={t('quickAdd.title')}
        value={f.mode}
        onChange={(v) => f.setMode(v as 'activity' | 'action')}
        data={[
          { value: 'activity', label: t('quickAdd.modeActivity') },
          { value: 'action', label: t('quickAdd.modeAction') },
        ]}
      />
      {f.mode === 'activity' && (
        <>
          <SegmentedControl
            aria-label={t('quickAdd.type')}
            value={f.type}
            onChange={(v) => f.setType(v as ActivityType)}
            data={TYPES.map((x) => ({ value: x, label: t(`quickAdd.types.${x}`) }))}
            style={{ overflowX: 'auto' }}
          />
          <Textarea label={t('quickAdd.note')} description={t('accounts.help.note')} withAsterisk value={f.note} onChange={(e) => f.setNote(e.currentTarget.value)} onBlur={touch('note')} error={touched.note && !f.note.trim() ? t('accounts.err.note') : undefined} autosize minRows={3} maxLength={4000} data-autofocus />
        </>
      )}
      {f.mode === 'action' && titleInput(true)}
      <Group grow align="flex-start">
        <EntityPicker kind="account" label={t('quickAdd.account')} value={f.account} onChange={f.setAccount} />
        <EntityPicker kind="person" label={t('quickAdd.person')} value={f.person} onChange={f.setPerson} />
      </Group>
      {context?.jobOrder && (
        <Text size="sm" c="dimmed">
          {t('quickAdd.job')}: {context.jobOrder.title}
        </Text>
      )}
      {f.mode === 'activity' && (
        <>
          <TextInput type="datetime-local" label={t('quickAdd.when')} value={f.when} max={nowLocal()} onChange={(e) => f.setWhen(e.currentTarget.value)} />
          <Switch label={t('quickAdd.followUp')} checked={f.followUp} onChange={(e) => f.setFollowUp(e.currentTarget.checked)} />
        </>
      )}
      {(f.mode === 'action' || f.followUp) && (
        <Group grow align="flex-start">
          {f.mode === 'activity' && titleInput(false)}
          <TextInput type="date" label={t('quickAdd.due')} withAsterisk value={f.due} onChange={(e) => f.setDue(e.currentTarget.value)} onBlur={touch('due')} error={touched.due && !f.due ? t('accounts.err.due') : undefined} />
        </Group>
      )}
      {f.mode === 'activity' && !f.hasContext && (
        <Text size="xs" c="dimmed">
          {t('quickAdd.needContext')}
        </Text>
      )}
    </Stack>
  );
}
