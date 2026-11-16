// Fee schedule (instalments) of an agreement: one row per instalment, each with exactly one of percent and amount.
import { ActionIcon, Button, Group, NativeSelect, SimpleGrid, Stack, Text, TextInput } from '@mantine/core';
import { IconPlus, IconTrash } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { ICON_SIZE, ICON_STROKE } from '../../shell/components/objectIcons';
import { emptyRow, FEE_TRIGGERS, parseRows, type ScheduleRow } from './feeModel';

export function ScheduleEditor({ value, onChange, error, max = 20 }: { value: string; onChange: (json: string) => void; error?: string; max?: number }) {
  const { t } = useTranslation();
  const rows = parseRows(value);
  const set = (next: ScheduleRow[]) => onChange(JSON.stringify(next));
  const patch = (i: number, p: Partial<ScheduleRow>) => set(rows.map((r, j) => (j === i ? { ...r, ...p } : r)));
  return (
    <Stack gap="xs" aria-label={t('agr.schedule.title')} role="group">
      {rows.length === 0 && (
        <Text size="sm" c="var(--color-ink3)">
          {t('agr.schedule.empty')}
        </Text>
      )}
      {rows.map((r, i) => (
        <Group key={r.id ?? i} align="flex-end" gap="xs" wrap="nowrap" style={{ borderTop: i === 0 ? 0 : '1px solid var(--color-border)', paddingTop: i === 0 ? 0 : 'var(--space-2)' }}>
          <SimpleGrid cols={{ base: 2, sm: 7 }} spacing="xs" style={{ flex: 1 }}>
            <TextInput label={t('agr.schedule.label')} value={r.label} maxLength={200} onChange={(e) => patch(i, { label: e.currentTarget.value })} style={{ gridColumn: 'span 2' }} />
            <NativeSelect
              label={t('agr.schedule.trigger')}
              value={r.trigger}
              data={FEE_TRIGGERS.map((x) => ({ value: x, label: t(`agr.trigger.${x}`) }))}
              onChange={(e) => patch(i, { trigger: e.currentTarget.value as ScheduleRow['trigger'] })}
            />
            {r.trigger === 'DATE_OFFSET' && (
              <TextInput label={t('agr.schedule.offsetDays')} inputMode="numeric" value={r.offsetDays} onChange={(e) => patch(i, { offsetDays: e.currentTarget.value })} />
            )}
            <TextInput label={t('agr.schedule.dueDays')} inputMode="numeric" value={r.dueDays} onChange={(e) => patch(i, { dueDays: e.currentTarget.value })} />
            <TextInput label={t('agr.schedule.percent')} inputMode="decimal" value={r.percent} onChange={(e) => patch(i, { percent: e.currentTarget.value, amount: e.currentTarget.value ? '' : r.amount })} />
            <TextInput label={t('agr.schedule.amount')} inputMode="decimal" value={r.amount} onChange={(e) => patch(i, { amount: e.currentTarget.value, percent: e.currentTarget.value ? '' : r.percent })} />
          </SimpleGrid>
          <ActionIcon variant="subtle" color="gray" size="lg" aria-label={t('agr.schedule.remove', { label: r.label || i + 1 })} onClick={() => set(rows.filter((_, j) => j !== i))}>
            <IconTrash size={ICON_SIZE} stroke={ICON_STROKE} />
          </ActionIcon>
        </Group>
      ))}
      {error && (
        <Text size="sm" c="red.8" role="alert">
          {error}
        </Text>
      )}
      <Group>
        <Button size="compact-md" variant="default" leftSection={<IconPlus size={ICON_SIZE} stroke={ICON_STROKE} aria-hidden />} disabled={rows.length >= max} onClick={() => set([...rows, emptyRow()])}>
          {t('agr.schedule.add')}
        </Button>
      </Group>
    </Stack>
  );
}
