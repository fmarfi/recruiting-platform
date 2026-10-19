import { Button, Chip, Group, ScrollArea, SegmentedControl, TextInput } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { useReference } from '../../shell/refdata/useReference';
import type { AccountFilters } from './useAccounts';

interface Props {
  filters: AccountFilters;
  meId: string;
  onChange: (patch: Partial<AccountFilters>) => void;
  onClear: () => void;
  active: boolean;
}

/** Name filter, Mine/All toggle and status chips that double as the status filter. */
export function AccountFilterBar({ filters, meId, onChange, onClear, active }: Props) {
  const { t, i18n } = useTranslation();
  const ref = useReference();
  const statuses = ref.data?.lifecycles.account.statuses ?? [];
  return (
    <>
      <Group>
        <TextInput placeholder={t('accounts.filterName')} aria-label={t('accounts.filterName')} value={filters.q} onChange={(e) => onChange({ q: e.currentTarget.value })} style={{ flex: 1, minWidth: 180 }} />
        <SegmentedControl
          value={filters.ownerId ? 'mine' : 'all'}
          onChange={(v) => onChange({ ownerId: v === 'mine' ? meId : '' })}
          data={[
            { value: 'mine', label: t('accounts.mine') },
            { value: 'all', label: t('accounts.all') },
          ]}
        />
        {active && (
          <Button variant="subtle" onClick={onClear}>
            {t('accounts.clear')}
          </Button>
        )}
      </Group>
      <ScrollArea type="never">
        <Chip.Group multiple={false} value={filters.status || 'ALL'} onChange={(v) => onChange({ status: v === 'ALL' ? '' : v })}>
          <Group gap="xs" wrap="nowrap">
            <Chip value="ALL">{t('accounts.all')}</Chip>
            {statuses.map((s) => (
              <Chip key={s.value} value={s.value}>
                {i18n.language.startsWith('tr') ? s.labelTr : s.labelEn}
              </Chip>
            ))}
          </Group>
        </Chip.Group>
      </ScrollArea>
    </>
  );
}
