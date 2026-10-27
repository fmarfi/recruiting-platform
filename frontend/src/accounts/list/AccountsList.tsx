import { Button, Container, Group, Pagination, Stack, Title } from '@mantine/core';
import { useMediaQuery } from '@mantine/hooks';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../shell/auth/useAuth';
import { EmptyState } from '../../shell/components/EmptyState';
import { DataGrid, ViewsMenu, formatSort, parseSort, useGridColumns, useViewStore } from '../../shell/grid';
import { tokens } from '../../theme/tokens';
import { NewAccountDialog } from '../components/NewAccountDialog';
import { AccountCard, useAccountColumns, type Account } from './accountColumns';
import { applyView, DEFAULT_SORT, filtersFromParams, paramsFromFilters, toView, VIEW_OPTIONS, type AccountView } from './accountFilters';
import { AccountFilterBar } from './AccountFilterBar';
import { useAccounts, type AccountFilters } from './useAccounts';

const GRID_ID = 'accounts';

/** Accounts list (A-01): owner defaults to Me, status chips filter, sortable columns, pages of 20. Filters can come from the URL query. */
export function AccountsList() {
  const { t } = useTranslation();
  const { user, isManager, isAdmin } = useAuth();
  const nav = useNavigate();
  const phone = useMediaQuery(`(max-width: ${tokens.breakpoints.phone}px)`);
  const meId = user?.id ?? '';
  // The filters are the URL: Ctrl+K links, Back and reload all keep the state. Replace avoids a history entry per keystroke.
  const [params, setParams] = useSearchParams();
  const filters = useMemo(() => filtersFromParams(params, meId, isAdmin), [params, meId, isAdmin]);
  const setFilters = (fn: (f: AccountFilters) => AccountFilters) => setParams(paramsFromFilters(fn(filters), isAdmin), { replace: true });
  const [creating, setCreating] = useState(false);
  const res = useAccounts(filters);
  const defs = useAccountColumns();
  const columns = useGridColumns<Account>(GRID_ID, defs);
  const { store: views, notice } = useViewStore<AccountView>(VIEW_OPTIONS);

  const change = (patch: Partial<AccountFilters>) => setFilters((f) => ({ ...f, page: 0, ...patch }));
  const clear = () => setFilters((f) => ({ ...f, q: '', status: '', ownerId: '', page: 0 }));
  const filtered = !!(filters.q || filters.status || filters.ownerId);
  const d = res.data;
  const newBtn = <Button onClick={() => setCreating(true)}>{t('accounts.newAccount')}</Button>;
  const sort = parseSort(filters.sort) ?? parseSort(DEFAULT_SORT);

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
        <DataGrid<Account>
          gridId={GRID_ID}
          columns={defs}
          columnsController={columns}
          rows={d?.content}
          rowId={(a) => a.id}
          loading={res.loading}
          error={!!res.error}
          onRetry={res.reload}
          sort={sort}
          onSort={(s) => change({ sort: formatSort(s) })}
          exportName="accounts"
          phone={!!phone}
          renderCard={(a) => <AccountCard a={a} />}
          toolbar={
            <>
              <ViewsMenu<AccountView>
                store={views}
                notice={notice}
                current={{ filters: toView(filters), columns: columns.state, sort }}
                onApply={(v) => {
                  setFilters((f) => applyView(f, v, meId));
                  if (v.columns) columns.set(v.columns);
                }}
              />
            </>
          }
          empty={
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
          }
        />
        {d && d.page.totalPages > 1 && <Pagination total={d.page.totalPages} value={d.page.number + 1} onChange={(p) => setFilters((f) => ({ ...f, page: p - 1 }))} />}
      </Stack>
      <NewAccountDialog opened={creating} onClose={() => setCreating(false)} onCreated={(id) => nav(`/accounts/${id}`)} />
    </Container>
  );
}
