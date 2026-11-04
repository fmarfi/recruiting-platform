// Client page (visual system v3, section 5): header band with tabs, the current tab in Panels, Details and Next actions in the aside.
import { Button, Container, Skeleton, Tabs, Text } from '@mantine/core';
import { useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../../shell/auth/useAuth';
import { EmptyState } from '../../shell/components/EmptyState';
import { ErrorBanner } from '../../shell/components/ErrorBanner';
import { Panel } from '../../shell/components/Panel';
import { RecordLayout } from '../../shell/components/RecordLayout';
import { SkeletonRows } from '../../shell/components/SkeletonRows';
import { HistoryPanel } from '../../shell/edit';
import { AddContactDialog } from '../components/AddContactDialog';
import { NewRecordDialog } from '../components/NewRecordDialog';
import { NewJobForm } from '../../pipeline/joborders/list/NewJobForm';
import { AddActionDialog } from '../components/AddActionDialog';
import { LogActivityDialog } from '../components/LogActivityDialog';
import { OffLimitsBanner } from './OffLimitsBanner';
import { AccountAside } from './AccountAside';
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
/** Tab count: plain ink3 text after the label (visual system v3, section 6). */
const Count = ({ n }: { n: number }) => (
  <Text span size="sm" c="var(--color-ink3)" style={{ fontVariantNumeric: 'tabular-nums' }}>
    {n}
  </Text>
);

export function AccountPage() {
  const { t } = useTranslation();
  const { id = '' } = useParams();
  const { isManager } = useAuth();
  const page = useAccountPage(id);
  const [dialog, setDialog] = useState<null | 'edit' | 'log' | 'action' | 'contact' | 'job' | 'proposal'>(null);
  const [tab, setTab] = useState('overview');
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
  const panel = (title: string, body: ReactNode, opts: { count?: number; action?: ReactNode; padding?: 'md' | 'none' } = {}) => (
    <Panel title={title} count={opts.count} actions={opts.action} padding={opts.padding}>
      {body}
    </Panel>
  );

  const tabs = (
    <Tabs.List style={{ flexWrap: 'nowrap', overflowX: 'auto', overflowY: 'hidden', borderBottom: 0 }}>
      <Tabs.Tab value="overview" style={tabStyle}>
        {t('accounts.record.overview')}
      </Tabs.Tab>
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
    </Tabs.List>
  );

  return (
    <Tabs value={tab} onChange={(v) => v && setTab(v)} keepMounted={false} variant="underline">
      <RecordLayout
        header={
          <AccountHeader
            account={a}
            tabs={tabs}
            onLog={() => setDialog('log')}
            onAddJob={() => setDialog('job')}
            onAddAction={() => setDialog('action')}
            onChanged={page.reloadAll}
            onEdit={() => setDialog('edit')}
          />
        }
        banner={<OffLimitsBanner accountId={a.id} canManage={isManager} />}
        main={
          <>
            <Tabs.Panel value="overview">
              <div style={{ display: 'grid', gap: 'var(--space-4)' }}>
                <DashboardStrip dashboard={page.dashboard.data} />
                {panel(t('accounts.record.activity'), timeline)}
                {panel(t('accounts.openSearches'), <JobsTab jobs={jobs} onAdd={() => setDialog('job')} />, { count: jobs.length, padding: 'none' })}
              </div>
            </Tabs.Panel>
            <Tabs.Panel value="jobs">
              {panel(t('accounts.tab.jobs'), <JobsTab jobs={jobs} onAdd={() => setDialog('job')} />, {
                count: jobs.length,
                padding: 'none',
                action: jobs.length > 0 ? <AddButton label={t('accounts.newJob')} onClick={() => setDialog('job')} /> : undefined,
              })}
            </Tabs.Panel>
            <Tabs.Panel value="contacts">
              {panel(
                t('accounts.tab.contacts'),
                <ContactsTab onAdd={() => setDialog('contact')} contacts={contacts} showEnded={page.showEnded} onShowEnded={page.setShowEnded} accountId={a.id} onChanged={page.reloadAll} />,
                { count: contacts.length, action: contacts.length > 0 ? <AddButton label={t('accounts.addContact')} onClick={() => setDialog('contact')} /> : undefined },
              )}
            </Tabs.Panel>
            <Tabs.Panel value="relationships">
              {panel(t('accounts.tab.relationships'), <RelationshipsTab contacts={contacts.filter((c) => c.active)} connections={page.connections.data} onAddContact={() => setDialog('contact')} />)}
            </Tabs.Panel>
            <Tabs.Panel value="proposals">
              {panel(t('accounts.tab.proposals'), <ProposalsTab proposals={proposals} onChanged={page.reloadAll} onAdd={() => setDialog('proposal')} />, {
                count: proposals.length,
                action: proposals.length > 0 ? <AddButton label={t('accounts.newProposal')} onClick={() => setDialog('proposal')} /> : undefined,
              })}
            </Tabs.Panel>
            <Tabs.Panel value="history">{panel(t('accounts.tab.history'), <HistoryPanel entityType="ACCOUNT" entityId={a.id} />)}</Tabs.Panel>
          </>
        }
        aside={<AccountAside account={a} dashboard={page.dashboard.data} onAddAction={() => setDialog('action')} />}
      />
      <AccountEditDrawer account={a} opened={dialog === 'edit'} onClose={() => setDialog(null)} onSaved={page.reloadAll} />
      <LogActivityDialog opened={dialog === 'log'} onClose={() => setDialog(null)} accountId={a.id} onDone={page.reloadAll} />
      <AddActionDialog opened={dialog === 'action'} onClose={() => setDialog(null)} accountId={a.id} onDone={page.reloadAll} />
      <AddContactDialog opened={dialog === 'contact'} onClose={() => setDialog(null)} accountId={a.id} onDone={page.reloadAll} />
      <NewJobForm opened={dialog === 'job'} onClose={() => setDialog(null)} accountId={a.id} onDone={page.reloadAll} />
      <NewRecordDialog kind="proposal" opened={dialog === 'proposal'} onClose={() => setDialog(null)} accountId={a.id} onDone={page.reloadAll} />
    </Tabs>
  );
}
