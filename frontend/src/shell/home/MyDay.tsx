import { Button, Container, Grid, Skeleton, Stack } from '@mantine/core';
import { IconPlus } from '@tabler/icons-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { AddActionDialog } from '../../accounts/components/AddActionDialog';
import { NewAccountDialog } from '../../accounts/components/NewAccountDialog';
import { useAuth } from '../auth/useAuth';
import { EmptyState } from '../components/EmptyState';
import { ErrorBanner } from '../components/ErrorBanner';
import { PageHeader } from '../components/PageHeader';
import { SkeletonRows } from '../components/SkeletonRows';
import { ICON_SIZE, ICON_STROKE } from '../components/objectIcons';
import { fmtDate } from '../lib/format';
import { HomePanel } from './HomeSection';
import { NextActionsSection } from './NextActionsSection';
import { OpenSearchesSection, RecentClientsSection } from './SideSections';
import { StatsStrip } from './StatsStrip';
import { useMyDay } from './useMyDay';

/** Home screen (UX-01): what should I do next? Content sits on the canvas; one white panel per column. */
export function MyDay() {
  const { t, i18n } = useTranslation();
  const { isManager } = useAuth();
  const nav = useNavigate();
  const home = useMyDay();
  const [adding, setAdding] = useState(false);
  const [newAccount, setNewAccount] = useState(false);
  const d = home.data;

  if (home.loading && !d)
    return (
      <Container maw="var(--content-max-width)" p={0}>
        <Skeleton height={32} w={260} mb="md" />
        <SkeletonRows n={4} />
      </Container>
    );
  if (!d) return <ErrorBanner onRetry={home.reload} message={t('myday.loadError')} hint={t('myday.loadErrorHint')} />;

  const noAccounts = d.stats.activeAccounts === 0 && d.recentAccounts.length === 0 && d.openJobOrders.length === 0;
  const actions = d.nextActions.filter((a) => !home.hidden.has(a.id));

  return (
    <Container maw="var(--content-max-width)" p={0}>
      <PageHeader
        title={t('myday.greeting', { name: d.user.name })}
        subtitle={fmtDate(d.today, i18n.language)}
        actions={
          !noAccounts && (
            <Button onClick={() => setAdding(true)} leftSection={<IconPlus size={ICON_SIZE} stroke={ICON_STROKE} aria-hidden />}>
              {t('accounts.addAction')}
            </Button>
          )
        }
      />

      {noAccounts ? (
        <EmptyState
          title={t('myday.welcome')}
          hint={t('myday.welcomeHint')}
          actions={
            <>
              <Button onClick={() => setNewAccount(true)}>{t('myday.addFirst')}</Button>
              {isManager && (
                <Button variant="default" onClick={() => nav('/settings/import')}>
                  {t('myday.import')}
                </Button>
              )}
            </>
          }
        />
      ) : (
        <Stack gap="var(--space-6)">
          <StatsStrip stats={d.stats} openJobs={d.openJobOrders} />
          <Grid>
            <Grid.Col span={{ base: 12, md: 7 }}>
              <NextActionsSection actions={actions} today={d.today} undo={home.undo} onDone={(a, v) => void home.setDone(a, v)} onSnooze={(a, due) => void home.snooze(a, due)} onAdd={() => setAdding(true)} />
            </Grid.Col>
            <Grid.Col span={{ base: 12, md: 5 }}>
              <HomePanel>
                <OpenSearchesSection jobs={d.openJobOrders} openTotal={d.stats.openJobOrders} />
                <RecentClientsSection accounts={d.recentAccounts} today={d.today} />
              </HomePanel>
            </Grid.Col>
          </Grid>
        </Stack>
      )}
      <AddActionDialog opened={adding} onClose={() => setAdding(false)} onDone={home.reload} />
      <NewAccountDialog opened={newAccount} onClose={() => setNewAccount(false)} onCreated={(id) => nav(`/clients/${id}`)} />
    </Container>
  );
}
