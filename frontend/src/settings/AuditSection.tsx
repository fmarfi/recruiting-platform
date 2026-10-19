import { Select, Stack, Table, Text } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api, type components } from '../api';
import { useAuth } from '../shell/auth/useAuth';
import { ErrorBanner } from '../shell/components/ErrorBanner';
import { SkeletonRows } from '../shell/components/SkeletonRows';
import { useAsync } from '../shell/hooks/useAsync';
import { fmtDate } from '../shell/lib/format';

type Action = components['schemas']['AccessAction'];
const ACTIONS: Action[] = ['VIEW', 'EXPORT', 'DOWNLOAD'];

/** Audit log (NFR-05): who viewed or exported personal data. Admin only (the API enforces it too). */
export function AuditSection() {
  const { t, i18n } = useTranslation();
  const { isAdmin } = useAuth();
  const [action, setAction] = useState<Action | null>(null);
  const log = useAsync(() => api.GET('/audit/access-log', { params: { query: { action: action ?? undefined, size: 50 } } }), [action]);

  if (!isAdmin) return <Text c="dimmed">{t('settings.audit.adminOnly')}</Text>;
  const rows = log.data?.content ?? [];
  return (
    <Stack>
      <Select
        w={220}
        clearable
        aria-label={t('settings.common.action')}
        placeholder={t('settings.audit.all')}
        value={action}
        onChange={(v) => setAction(v as Action | null)}
        data={ACTIONS.map((a) => ({ value: a, label: t(`settings.audit.${a}`) }))}
      />
      {log.loading && !log.data && <SkeletonRows n={4} />}
      {log.error !== undefined && <ErrorBanner onRetry={log.reload} />}
      {log.data && rows.length === 0 && <Text c="dimmed">{t('settings.common.none')}</Text>}
      {rows.length > 0 && (
        <Table withTableBorder>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>{t('settings.audit.at')}</Table.Th><Table.Th>{t('settings.common.user')}</Table.Th><Table.Th>{t('settings.common.action')}</Table.Th>
              <Table.Th>{t('settings.audit.entity')}</Table.Th><Table.Th>{t('settings.audit.ip')}</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {rows.map((r) => (
              <Table.Tr key={r.id}>
                <Table.Td>{fmtDate(r.at, i18n.language)}</Table.Td><Table.Td>{r.user.name}</Table.Td>
                <Table.Td>{t(`settings.audit.${r.action}`)}</Table.Td><Table.Td>{r.entityType}</Table.Td><Table.Td>{r.ipAddress ?? '-'}</Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      )}
    </Stack>
  );
}
