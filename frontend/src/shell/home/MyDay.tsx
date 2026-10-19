import { Button, Container, Grid, Group, Skeleton, Stack, Text, Title } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { AddActionDialog } from '../../accounts/components/AddActionDialog';
import { NewAccountDialog } from '../../accounts/components/NewAccountDialog';
import { useAuth } from '../auth/useAuth';
import { EmptyState } from '../components/EmptyState';
import { ErrorBanner } from '../components/ErrorBanner';
import { SkeletonRows } from '../components/SkeletonRows';
import { fmtDate } from '../lib/format';
import { NextActionsCard } from './NextActionsCard';
import { OpenJobsCard, RecentAccountsCard } from './SideCards';
import { StatsStrip } from './StatsStrip';
import { useMyDay } from './useMyDay';

/** Home screen (UX-01): what should I do next? */
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
  if (!d) return <ErrorBanner onRetry={home.reload} message={t('myday.loadError')} />;

  const noAccounts = d.stats.activeAccounts === 0 && d.recentAccounts.length === 0 && d.openJobOrders.length === 0;
  const actions = d.nextActions.filter((a) => !home.hidden.has(a.id));
  const todayCount = actions.filter((a) => !a.overdue && a.dueOn === d.today).length;

  return (
    <Container maw="var(--content-max-width)" p={0}>
      <Group justify="space-between" align="baseline" mb="md">
        <div>
          <Title order={1} data-testid="screen-title">
            {t('myday.greeting', { name: d.user.name })}
          </Title>
          <Text c="dimmed">{t('myday.summary', { overdue: d.overdueCount, today: todayCount })}</Text>
        </div>
        <Text c="dimmed">{fmtDate(d.today, i18n.language)}</Text>
      </Group>

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
        <Stack>
          <StatsStrip stats={d.stats} />
          <Grid>
            <Grid.Col span={{ base: 12, md: 7 }}>
              <NextActionsCard actions={actions} today={d.today} undo={home.undo} onDone={(a, v) => void home.setDone(a, v)} onSnooze={(a, due) => void home.snooze(a, due)} onAdd={() => setAdding(true)} />
            </Grid.Col>
            <Grid.Col span={{ base: 12, md: 5 }}>
              <Stack>
                <OpenJobsCard jobs={d.openJobOrders} />
                <RecentAccountsCard accounts={d.recentAccounts} today={d.today} />
              </Stack>
            </Grid.Col>
          </Grid>
        </Stack>
      )}
      <AddActionDialog opened={adding} onClose={() => setAdding(false)} onDone={home.reload} />
      <NewAccountDialog opened={newAccount} onClose={() => setNewAccount(false)} onCreated={(id) => nav(`/accounts/${id}`)} />
    </Container>
  );
}
