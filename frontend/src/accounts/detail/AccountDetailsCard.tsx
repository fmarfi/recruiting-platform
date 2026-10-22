import { Anchor, Box, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import type { components } from '../../api';
import { fmtDate } from '../../shell/lib/format';
import { StatusBadge } from '../../shell/components/StatusBadge';
import { SectionCard } from '../../shell/ui/SectionCard';

type Account = components['schemas']['Account'];

/** Details tab: the account's own fields in a two-column definition grid. */
export function AccountDetailsCard({ account: a }: { account: Account }) {
  const { t, i18n } = useTranslation();
  const rows: [string, React.ReactNode][] = [
    [t('accounts.name'), a.name],
    [t('accounts.status'), <StatusBadge key="s" kind="account" value={a.status} size="sm" />],
    [t('accounts.sector'), a.sector],
    [t('accounts.city'), a.city],
    [t('accounts.owner'), a.owner.name],
    [t('accounts.website'), a.website ? <Anchor href={a.website} target="_blank" rel="noreferrer">{a.website}</Anchor> : null],
    [t('accounts.since'), fmtDate(a.openedOn, i18n.language)],
    [t('accounts.notes'), a.notes],
  ];
  return (
    <SectionCard title={t('accounts.tab.details')}>
      <Box style={{ display: 'grid', gap: 'var(--space-3)', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))' }}>
        {rows.map(([label, value]) => (
          <Box key={label} style={{ minWidth: 0 }}>
            <Text fz="xs" c="dimmed">{label}</Text>
            <Text component="div">{value || '-'}</Text>
          </Box>
        ))}
      </Box>
    </SectionCard>
  );
}
