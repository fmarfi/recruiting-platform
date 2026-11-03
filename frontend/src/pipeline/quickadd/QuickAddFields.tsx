// Form fields for Quick add (UX-06). Order: type, about (client and candidate), content, when.
import { Button, Checkbox, Group, SegmentedControl, SimpleGrid, Stack, Text, TextInput, Textarea } from '@mantine/core';
import { IconBuilding, IconMail, IconMapPin, IconNote, IconPhone, IconUsers, IconUserStar, type IconProps } from '@tabler/icons-react';
import type { ComponentType } from 'react';
import { useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toIsoDate } from '../shared';
import { EntityPicker } from './EntityPicker';
import { TYPES, nowLocal, type ActivityType, type QuickAddContext, type useQuickAddForm } from './quickAddState';

type Form = ReturnType<typeof useQuickAddForm>;

/** Type icons (Tabler, outline, stroke 1.75, 16 px in controls). */
const TYPE_ICON: Record<ActivityType, ComponentType<IconProps>> = {
  CALL: IconPhone,
  VISIT: IconMapPin,
  MEETING: IconUsers,
  EMAIL: IconMail,
  NOTE: IconNote,
};

const iconProps: IconProps = { size: 16, stroke: 1.75, 'aria-hidden': true };

/** Local date (YYYY-MM-DD) `days` from today. */
const dayOffset = (days: number) => {
  const d = new Date();
  d.setDate(d.getDate() + days);
  return toIsoDate(d);
};

export function QuickAddFields({ f, context }: { f: Form; context?: QuickAddContext }) {
  const { t } = useTranslation();
  const [touched, setTouched] = useState<Record<string, boolean>>({});
  const dueRef = useRef<HTMLInputElement>(null);
  const touch = (k: string) => () => setTouched((x) => ({ ...x, [k]: true }));

  // Quick picks only set the same values the inputs hold; they never change validation.
  const pickWhen = (day: 'now' | 'today' | 'yesterday') => {
    if (day === 'now') return f.setWhen(nowLocal());
    const now = nowLocal();
    const time = f.when.length >= 16 ? f.when.slice(11, 16) : now.slice(11, 16);
    const value = `${dayOffset(day === 'today' ? 0 : -1)}T${time}`;
    f.setWhen(value > now ? now : value);
  };
  const pickDue = (days: number) => f.setDue(dayOffset(days));
  const pickDueByHand = () => {
    dueRef.current?.focus();
    try {
      dueRef.current?.showPicker?.();
    } catch {
      /* the browser may refuse; focus is enough */
    }
  };

  const titleInput = (autoFocus: boolean) => (
    <TextInput
      label={t('quickAdd.actionTitle')}
      placeholder={t('quickAdd.actionPlaceholder')}
      withAsterisk
      value={f.title}
      onChange={(e) => f.setTitle(e.currentTarget.value)}
      onBlur={touch('title')}
      error={touched.title && !f.title.trim() ? t('accounts.err.title') : undefined}
      maxLength={300}
      data-autofocus={autoFocus || undefined}
    />
  );

  const quick = (label: string, onClick: () => void) => (
    <Button type="button" variant="default" size="compact-sm" radius="sm" onClick={onClick}>
      {label}
    </Button>
  );

  const dueRow = (
    <Group align="flex-end" gap="xs">
      <Group gap="xs">
        {quick(t('quickAdd.tomorrow'), () => pickDue(1))}
        {quick(t('quickAdd.nextWeek'), () => pickDue(7))}
        {quick(t('quickAdd.pickDate'), pickDueByHand)}
      </Group>
      <TextInput
        ref={dueRef}
        type="date"
        label={t('quickAdd.due')}
        withAsterisk
        value={f.due}
        onChange={(e) => f.setDue(e.currentTarget.value)}
        onBlur={touch('due')}
        error={touched.due && !f.due ? t('accounts.err.due') : undefined}
        style={{ flex: 1 }}
      />
    </Group>
  );

  return (
    <Stack gap="lg">
      <Stack gap="sm">
        <SegmentedControl
          size="sm"
          radius="sm"
          aria-label={t('quickAdd.title')}
          value={f.mode}
          onChange={(v) => f.setMode(v as 'activity' | 'action')}
          data={[
            { value: 'activity', label: t('quickAdd.modeActivity') },
            { value: 'action', label: t('quickAdd.modeAction') },
          ]}
        />
        {f.mode === 'activity' && (
          <SegmentedControl
            size="sm"
            radius="sm"
            aria-label={t('quickAdd.type')}
            value={f.type}
            onChange={(v) => f.setType(v as ActivityType)}
            data={TYPES.map((x) => {
              const Icon = TYPE_ICON[x];
              return {
                value: x,
                label: (
                  <Group gap="xs" wrap="nowrap" justify="center">
                    <Icon {...iconProps} />
                    <span>{t(`quickAdd.types.${x}`)}</span>
                  </Group>
                ),
              };
            })}
            style={{ overflowX: 'auto' }}
          />
        )}
      </Stack>

      <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="sm">
        <EntityPicker
          kind="account"
          label={t('quickAdd.account')}
          placeholder={t('quickAdd.searchClients')}
          icon={<IconBuilding {...iconProps} />}
          value={f.account}
          onChange={f.setAccount}
        />
        <EntityPicker
          kind="person"
          label={t('quickAdd.person')}
          placeholder={t('quickAdd.searchCandidates')}
          icon={<IconUserStar {...iconProps} />}
          value={f.person}
          onChange={f.setPerson}
        />
      </SimpleGrid>
      {context?.jobOrder && (
        <Text size="xs" style={{ color: 'var(--color-ink3)' }}>
          {t('quickAdd.job')}: {context.jobOrder.title}
        </Text>
      )}

      {f.mode === 'activity' ? (
        <Textarea
          label={t('quickAdd.note')}
          placeholder={t('quickAdd.notePlaceholder')}
          withAsterisk
          value={f.note}
          onChange={(e) => f.setNote(e.currentTarget.value)}
          onBlur={touch('note')}
          error={touched.note && !f.note.trim() ? t('accounts.err.note') : undefined}
          autosize
          minRows={3}
          maxRows={8}
          maxLength={4000}
          data-autofocus
        />
      ) : (
        titleInput(true)
      )}

      {f.mode === 'activity' && (
        <Stack gap="sm">
          <Group align="flex-end" gap="xs">
            <Group gap="xs">
              {quick(t('quickAdd.now'), () => pickWhen('now'))}
              {quick(t('quickAdd.today'), () => pickWhen('today'))}
              {quick(t('quickAdd.yesterday'), () => pickWhen('yesterday'))}
            </Group>
            <TextInput
              type="datetime-local"
              label={t('quickAdd.when')}
              value={f.when}
              max={nowLocal()}
              onChange={(e) => f.setWhen(e.currentTarget.value)}
              style={{ flex: 1 }}
            />
          </Group>
          <Checkbox label={t('quickAdd.followUp')} checked={f.followUp} onChange={(e) => f.setFollowUp(e.currentTarget.checked)} />
          {f.followUp && (
            <Stack gap="sm">
              {titleInput(false)}
              {dueRow}
            </Stack>
          )}
        </Stack>
      )}

      {f.mode === 'action' && dueRow}
    </Stack>
  );
}
