import { Alert, Anchor, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { fmtDate } from '../../shell/lib/format';
import { useAccountOffLimits } from '../../settings/offLimits';

/** Off-limits rules in force for this account (GET /off-limits?accountId). Shows nothing when there is none or the list cannot be loaded. */
export function OffLimitsBanner({ accountId, canManage }: { accountId: string; canManage: boolean }) {
  const { t, i18n } = useTranslation();
  const rules = useAccountOffLimits(accountId);
  const list = rules.data?.content ?? [];
  if (list.length === 0) return null;
  return (
    <Alert color="red" variant="light" title={t('accounts.offLimits.title')} role="status">
      {list.map((r) => (
        <Text size="sm" key={r.id}>
          {t(`settings.offLimits.reasons.${r.reason}`)}
          {' - '}
          {r.endsOn ? t('accounts.offLimits.until', { date: fmtDate(r.endsOn, i18n.language) }) : t('settings.offLimits.openEnded')}
          {r.note ? ` (${r.note})` : ''}
        </Text>
      ))}
      <Text size="sm" mt={4}>{t('accounts.offLimits.hint')}</Text>
      {canManage && (
        <Anchor component={Link} to="/settings/offlimits" size="sm">
          {t('accounts.offLimits.manage')}
        </Anchor>
      )}
    </Alert>
  );
}
