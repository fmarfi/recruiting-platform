import { Button, Chip, Container, Group, Select, Stack, Text, Title } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useSearchParams } from 'react-router-dom';
import { EmptyState } from '../components/EmptyState';
import { ErrorBanner } from '../components/ErrorBanner';
import { SkeletonRows } from '../components/SkeletonRows';
import { StatusBadge } from '../components/StatusBadge';
import { fmtRelative } from '../lib/format';
import { useReference } from '../refdata/useReference';
import { SearchGroupCard } from './SearchGroupCard';
import { useSearchResults, type SearchType } from './useSearchResults';

/** Search results page (A-14): grouped results, type chips, sector/status filters. */
export function SearchPage() {
  const { t, i18n } = useTranslation();
  const ref = useReference();
  const [params, setParams] = useSearchParams();
  const q = (params.get('q') ?? '').trim();
  const sector = params.get('sector') ?? '';
  const accountStatus = params.get('accountStatus') ?? '';
  const [type, setType] = useState<SearchType>('all');
  const res = useSearchResults({ q, type, sector, accountStatus });

  function setParam(k: string, v: string | null) {
    const p = new URLSearchParams(params);
    if (v) p.set(k, v);
    else p.delete(k);
    setParams(p);
  }
  const clear = () => setParams(new URLSearchParams({ q }));

  const d = res.data;
  const filtersOn = !!(sector || accountStatus);
  const total = d ? d.totals.accounts + d.totals.people + d.totals.jobOrders : 0;
  const statusOptions = (ref.data?.lifecycles.account.statuses ?? []).map((s) => ({ value: s.value, label: i18n.language.startsWith('tr') ? s.labelTr : s.labelEn }));

  return (
    <Container maw="var(--content-max-width)" p={0}>
      <Title order={1} data-testid="screen-title">
        {q ? t('search.results', { q }) : t('nav.search')}
      </Title>
      {q.length < 2 ? (
        <Stack mt="md">
          <EmptyState title={t('search.start')} hint={t('search.startHint')} />
        </Stack>
      ) : (
        <Stack mt="md">
          <Chip.Group multiple={false} value={type} onChange={(v) => setType(v as SearchType)}>
            <Group gap="xs">
              <Chip value="all">{t('search.all')} {d ? total : ''}</Chip>
              <Chip value="ACCOUNT">{t('search.group.accounts')} {d?.totals.accounts ?? ''}</Chip>
              <Chip value="PERSON">{t('search.group.people')} {d?.totals.people ?? ''}</Chip>
              <Chip value="JOB_ORDER">{t('search.group.jobs')} {d?.totals.jobOrders ?? ''}</Chip>
            </Group>
          </Chip.Group>
          <Group>
            <Select aria-label={t('search.sector')} placeholder={t('search.sector')} data={ref.data?.sectors ?? []} value={sector || null} onChange={(v) => setParam('sector', v)} clearable />
            <Select aria-label={t('search.status')} placeholder={t('search.status')} data={statusOptions} value={accountStatus || null} onChange={(v) => setParam('accountStatus', v)} clearable />
            {filtersOn && (
              <Button variant="subtle" onClick={clear}>
                {t('search.clear')}
              </Button>
            )}
          </Group>

          {res.error ? (
            <ErrorBanner onRetry={res.reload} />
          ) : res.loading && !d ? (
            <SkeletonRows n={5} />
          ) : d && total === 0 ? (
            <EmptyState
              title={t('search.none', { q })}
              hint={t('search.noneHint')}
              actions={
                <>
                  {filtersOn && (
                    <Button variant="default" onClick={clear}>
                      {t('search.clear')}
                    </Button>
                  )}
                  <Button component={Link} to="/accounts">
                    {t('search.addClient', { q })}
                  </Button>
                </>
              }
            />
          ) : (
            d && (
              <Stack>
                <SearchGroupCard
                  q={q}
                  title={t('search.group.accounts')}
                  total={d.totals.accounts}
                  rows={d.accounts.map((a) => ({ id: a.id, to: `/accounts/${a.id}`, title: a.name, sub: [a.sector, a.city, a.owner.name].filter(Boolean).join(' - '), right: <StatusBadge kind="account" value={a.status} size="sm" /> }))}
                />
                <SearchGroupCard
                  q={q}
                  title={t('search.group.people')}
                  total={d.totals.people}
                  rows={d.people.map((p) => ({ id: p.id, to: `/people/${p.id}`, title: p.name, sub: [p.title, p.company].filter(Boolean).join(', '), right: <Text size="xs" c="dimmed">{fmtRelative(p.lastContactAt, i18n.language)}</Text> }))}
                />
                <SearchGroupCard
                  q={q}
                  title={t('search.group.jobs')}
                  total={d.totals.jobOrders}
                  rows={d.jobOrders.map((j) => ({ id: j.id, to: `/job-orders/${j.id}`, title: j.title, sub: `${j.account.name} - ${t('myday.daysOpen', { n: j.daysOpen })}`, right: <StatusBadge kind="jobOrder" value={j.status} size="sm" /> }))}
                />
              </Stack>
            )
          )}
        </Stack>
      )}
    </Container>
  );
}
