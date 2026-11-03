import { Button, Card, Container, Grid, Group, Skeleton, Stack, Tabs, Text } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../../shell/auth/useAuth';
import { EmptyState } from '../../shell/components/EmptyState';
import { ErrorBanner } from '../../shell/components/ErrorBanner';
import { SkeletonRows } from '../../shell/components/SkeletonRows';
import { HistoryPanel } from '../../shell/edit';
import { AddContactDialog } from '../components/AddContactDialog';
import { NewRecordDialog } from '../components/NewRecordDialog';
import { NewJobForm } from '../../pipeline/joborders/list/NewJobForm';
import { AddActionDialog } from '../components/AddActionDialog';
import { LogActivityDialog } from '../components/LogActivityDialog';
import { OffLimitsBanner } from './OffLimitsBanner';
import { AccountEditDrawer } from './AccountEditDrawer';
import { AccountHeader } from './AccountHeader';
import { AddButton } from './AddButton';
import { ContactsTab } from './ContactsTab';
import { DashboardStrip } from './DashboardStrip';
import { JobsTab } from './JobsTab';
import { ProposalsTab } from './ProposalsTab';
import { RelationshipsTab } from './RelationshipsTab';
import { TimelinePanel } from './TimelinePanel';
import { useAccountPage } from './useAccountPage';

const tabStyle = { whiteSpace: 'nowrap' } as const;
/** Tab count: tabular number after the label, in ink3 (visual system section 6). */
const Count = ({ n }: { n: number }) => (
  <Text span size="sm" c="dimmed" style={{ fontVariantNumeric: 'tabular-nums' }}>
    {n}
  </Text>
);

/** Account page (UX-05): header, dashboard strip, tabs and the timeline in one view. Sections sit on the canvas, not in cards. */
export function AccountPage() {
  const { t } = useTranslation();
  const { id = '' } = useParams();
  const { isManager } = useAuth();
  const page = useAccountPage(id);
  const [dialog, setDialog] = useState<null | 'edit' | 'log' | 'action' | 'contact' | 'job' | 'proposal'>(null);
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
  if (page.account.error) return <ErrorBanner onRetry={page.reloadAll} message={t('accounts.loadErrorTitle')} />;
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
    <TimelinePanel entries={page.timeline.data?.content ?? []} kind={page.kind} onKind={(k) => page.setKind(k as typeof page.kind)} onLog={() => setDialog('log')} />
  );

  return (
    <Container maw="var(--content-max-width)" p={0}>
      <Stack gap="lg">
        <AccountHeader account={a} onLog={() => setDialog('log')} onAddAction={() => setDialog('action')} onChanged={page.reloadAll} onEdit={() => setDialog('edit')} />
        <OffLimitsBanner accountId={a.id} canManage={isManager} />
        <DashboardStrip dashboard={page.dashboard.data} onAddAction={() => setDialog('action')} />
        <Grid>
          <Grid.Col span={{ base: 12, md: 8 }}>
            <Tabs defaultValue="jobs" variant="underline">
              <Tabs.List style={{ flexWrap: 'nowrap', overflowX: 'auto', overflowY: 'hidden' }}>
                <Tabs.Tab value="jobs" style={tabStyle} rightSection={<Count n={jobs.length} />}>
                  {t('accounts.tab.jobs')}
                </Tabs.Tab>
                <Tabs.Tab value="contacts" style={tabStyle} rightSection={<Count n={contacts.length} />}>
                  {t('accounts.tab.contacts')}
                </Tabs.Tab>
                <Tabs.Tab value="relationships" style={tabStyle}>
                  {t('accounts.tab.relationships')}
                </Tabs.Tab>
                <Tabs.Tab value="proposals" style={tabStyle} rightSection={<Count n={proposals.length} />}>
                  {t('accounts.tab.proposals')}
                </Tabs.Tab>
                <Tabs.Tab value="history" style={tabStyle}>
                  {t('accounts.tab.history')}
                </Tabs.Tab>
                <Tabs.Tab value="timeline" hiddenFrom="md" style={tabStyle}>
                  {t('accounts.tab.timeline')}
                </Tabs.Tab>
              </Tabs.List>
              <Tabs.Panel value="jobs" pt="md">
                {jobs.length > 0 && (
                  <Group justify="flex-end" mb="xs">
                    <AddButton label={t('accounts.newJob')} onClick={() => setDialog('job')} />
                  </Group>
                )}
                <JobsTab jobs={jobs} onAdd={() => setDialog('job')} />
              </Tabs.Panel>
              <Tabs.Panel value="contacts" pt="md">
                {contacts.length > 0 && (
                  <Group justify="flex-end" mb="xs">
                    <AddButton label={t('accounts.addContact')} onClick={() => setDialog('contact')} />
                  </Group>
                )}
                <ContactsTab onAdd={() => setDialog('contact')} contacts={contacts} showEnded={page.showEnded} onShowEnded={page.setShowEnded} accountId={a.id} onChanged={page.reloadAll} />
              </Tabs.Panel>
              <Tabs.Panel value="relationships" pt="md">
                <RelationshipsTab contacts={contacts.filter((c) => c.active)} connections={page.connections.data} onAddContact={() => setDialog('contact')} />
              </Tabs.Panel>
              <Tabs.Panel value="proposals" pt="md">
                {proposals.length > 0 && (
                  <Group justify="flex-end" mb="xs">
                    <AddButton label={t('accounts.newProposal')} onClick={() => setDialog('proposal')} />
                  </Group>
                )}
                <ProposalsTab proposals={proposals} onChanged={page.reloadAll} onAdd={() => setDialog('proposal')} />
              </Tabs.Panel>
              <Tabs.Panel value="history" pt="md">
                <HistoryPanel entityType="ACCOUNT" entityId={a.id} />
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
      <AccountEditDrawer account={a} opened={dialog === 'edit'} onClose={() => setDialog(null)} onSaved={page.reloadAll} />
      <LogActivityDialog opened={dialog === 'log'} onClose={() => setDialog(null)} accountId={a.id} onDone={page.reloadAll} />
      <AddActionDialog opened={dialog === 'action'} onClose={() => setDialog(null)} accountId={a.id} onDone={page.reloadAll} />
      <AddContactDialog opened={dialog === 'contact'} onClose={() => setDialog(null)} accountId={a.id} onDone={page.reloadAll} />
      <NewJobForm opened={dialog === 'job'} onClose={() => setDialog(null)} accountId={a.id} onDone={page.reloadAll} />
      <NewRecordDialog kind="proposal" opened={dialog === 'proposal'} onClose={() => setDialog(null)} accountId={a.id} onDone={page.reloadAll} />
    </Container>
  );
}
