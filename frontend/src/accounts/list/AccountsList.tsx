import { Button, Container, Group, Pagination, Stack, Title } from '@mantine/core';
import { useMediaQuery } from '@mantine/hooks';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../shell/auth/useAuth';
import { EmptyState } from '../../shell/components/EmptyState';
import { ErrorBanner } from '../../shell/components/ErrorBanner';
import { SkeletonRows } from '../../shell/components/SkeletonRows';
import { tokens } from '../../theme/tokens';
import { NewAccountDialog } from '../components/NewAccountDialog';
import { AccountFilterBar } from './AccountFilterBar';
import { AccountsTable } from './AccountsTable';
import { useAccounts, type AccountFilters } from './useAccounts';

/** Accounts list (A-01): owner defaults to Me, status chips filter, sortable columns, pages of 20. */
export function AccountsList() {
  const { t } = useTranslation();
  const { user, isManager } = useAuth();
  const nav = useNavigate();
  const phone = useMediaQuery(`(max-width: ${tokens.breakpoints.phone}px)`);
  const meId = user?.id ?? '';
  const [filters, setFilters] = useState<AccountFilters>({ q: '', status: '', ownerId: meId, page: 0, sort: 'lastActivityAt,desc' });
  const [creating, setCreating] = useState(false);
  const res = useAccounts(filters);

  const change = (patch: Partial<AccountFilters>) => setFilters((f) => ({ ...f, page: 0, ...patch }));
  const clear = () => setFilters((f) => ({ ...f, q: '', status: '', ownerId: '', page: 0 }));
  const filtered = !!(filters.q || filters.status || filters.ownerId);
  const sortBy = (field: string) =>
    change({ sort: filters.sort.startsWith(`${field},`) ? `${field},${filters.sort.endsWith('asc') ? 'desc' : 'asc'}` : `${field},${field === 'name' ? 'asc' : 'desc'}` });
  const d = res.data;
  const newBtn = <Button onClick={() => setCreating(true)}>{t('accounts.newAccount')}</Button>;

  return (
    <Container maw="var(--content-max-width)" p={0}>
      <Group justify="space-between" mb="md">
        <Title order={1} data-testid="screen-title">
          {t('nav.accounts')} {d ? `(${d.page.totalElements})` : ''}
        </Title>
        <Group>
          {isManager && (
            <Button variant="default" onClick={() => nav('/settings/import')}>
              {t('accounts.import')}
            </Button>
          )}
          {newBtn}
        </Group>
      </Group>
      <Stack>
        <AccountFilterBar filters={filters} meId={meId} onChange={change} onClear={clear} active={filtered} />
        {res.error ? (
          <ErrorBanner onRetry={res.reload} />
        ) : res.loading && !d ? (
          <SkeletonRows n={8} />
        ) : d && d.content.length === 0 ? (
          filtered ? (
            <EmptyState
              title={t('accounts.emptyFilter')}
              actions={
                <Button variant="default" onClick={clear}>
                  {t('accounts.clear')}
                </Button>
              }
            />
          ) : (
            <EmptyState title={t('accounts.emptyTitle')} hint={t('accounts.emptyHint')} actions={newBtn} />
          )
        ) : (
          d && (
            <>
              <AccountsTable rows={d.content} sort={filters.sort} onSort={sortBy} phone={!!phone} />
              {d.page.totalPages > 1 && <Pagination total={d.page.totalPages} value={d.page.number + 1} onChange={(p) => setFilters((f) => ({ ...f, page: p - 1 }))} />}
            </>
          )
        )}
      </Stack>
      <NewAccountDialog opened={creating} onClose={() => setCreating(false)} onCreated={(id) => nav(`/accounts/${id}`)} />
    </Container>
  );
}
