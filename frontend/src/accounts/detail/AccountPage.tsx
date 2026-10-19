import { Anchor, Button, Card, Container, Grid, Skeleton, Stack, Tabs, Text } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router-dom';
import { EmptyState } from '../../shell/components/EmptyState';
import { ErrorBanner } from '../../shell/components/ErrorBanner';
import { SkeletonRows } from '../../shell/components/SkeletonRows';
import { AddActionDialog } from '../components/AddActionDialog';
import { LogActivityDialog } from '../components/LogActivityDialog';
import { AccountHeader } from './AccountHeader';
import { ContactsTab } from './ContactsTab';
import { DashboardStrip } from './DashboardStrip';
import { JobsTab } from './JobsTab';
import { ProposalsTab } from './ProposalsTab';
import { TimelinePanel } from './TimelinePanel';
import { useAccountPage } from './useAccountPage';

/** Account page (UX-05): header, dashboard strip, tabs and the timeline in one view. */
export function AccountPage() {
  const { t } = useTranslation();
  const { id = '' } = useParams();
  const page = useAccountPage(id);
  const [dialog, setDialog] = useState<null | 'log' | 'action'>(null);
  const a = page.account.data;

  if (page.account.status === 404)
    return (
      <EmptyState
        title={t('accounts.notFound')}
        actions={
          <Button component={Link} to="/accounts">
            {t('accounts.backToList')}
          </Button>
        }
      />
    );
  if (page.account.error) return <ErrorBanner onRetry={page.reloadAll} />;
  if (!a)
    return (
      <Container maw="var(--content-max-width)" p={0}>
        <Skeleton height={80} mb="md" />
        <SkeletonRows n={4} />
      </Container>
    );

  const jobs = page.jobs.data?.content ?? [];
  const contacts = page.contacts.data ?? [];
  const proposals = page.proposals.data?.content ?? [];
  const timeline = (
    <TimelinePanel entries={page.timeline.data?.content ?? []} kind={page.kind} onKind={(k) => page.setKind(k as typeof page.kind)} />
  );

  return (
    <Container maw="var(--content-max-width)" p={0}>
      <Stack>
        <Text size="sm">
          <Anchor component={Link} to="/accounts">
            {t('nav.accounts')}
          </Anchor>{' '}
          / {a.name}
        </Text>
        <AccountHeader account={a} onLog={() => setDialog('log')} onAddAction={() => setDialog('action')} onChanged={page.reloadAll} />
        <DashboardStrip dashboard={page.dashboard.data} onAddAction={() => setDialog('action')} />
        <Grid>
          <Grid.Col span={{ base: 12, md: 8 }}>
            <Tabs defaultValue="jobs">
              <Tabs.List>
                <Tabs.Tab value="jobs">{t('accounts.tab.jobs')} {jobs.length}</Tabs.Tab>
                <Tabs.Tab value="contacts">{t('accounts.tab.contacts')} {contacts.length}</Tabs.Tab>
                <Tabs.Tab value="proposals">{t('accounts.tab.proposals')} {proposals.length}</Tabs.Tab>
                <Tabs.Tab value="timeline" hiddenFrom="md">
                  {t('accounts.tab.timeline')}
                </Tabs.Tab>
              </Tabs.List>
              <Tabs.Panel value="jobs" pt="md">
                <JobsTab jobs={jobs} />
              </Tabs.Panel>
              <Tabs.Panel value="contacts" pt="md">
                <ContactsTab contacts={contacts} showEnded={page.showEnded} onShowEnded={page.setShowEnded} />
              </Tabs.Panel>
              <Tabs.Panel value="proposals" pt="md">
                <ProposalsTab proposals={proposals} onChanged={page.reloadAll} />
              </Tabs.Panel>
              <Tabs.Panel value="timeline" pt="md" hiddenFrom="md">
                {timeline}
              </Tabs.Panel>
            </Tabs>
          </Grid.Col>
          <Grid.Col span={4} visibleFrom="md">
            <Card withBorder>{timeline}</Card>
          </Grid.Col>
        </Grid>
      </Stack>
      <LogActivityDialog opened={dialog === 'log'} onClose={() => setDialog(null)} accountId={a.id} onDone={page.reloadAll} />
      <AddActionDialog opened={dialog === 'action'} onClose={() => setDialog(null)} accountId={a.id} onDone={page.reloadAll} />
    </Container>
  );
}
