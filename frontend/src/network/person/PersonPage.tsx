// Person page (A-12, A-13, S-01): relationship, every pipeline the person is in, network links, consent status.
import { Alert, Button, Container, Group, Skeleton, Stack, Tabs, Text } from '@mantine/core';
import { ErrorBanner } from '../../shell/components/ErrorBanner';
import { useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router-dom';
import { api } from '../../api';
import { HistoryPanel } from '../../shell/edit';
import { QuickLog } from '../../shell/quicklog';
import { QuickAdd } from '../../pipeline/quickadd/QuickAdd';
import { problemText, type Schemas } from '../../pipeline/shared';
import { Panel } from '../../shell/components/Panel';
import { RecordLayout } from '../../shell/components/RecordLayout';
import { ICON_SIZE, ICON_STROKE, ConsentIcon } from '../../shell/components/objectIcons';
import { ExposureTab } from './ExposureTab';
import { FilesTab } from './FilesTab';
import { ActivityTab } from './ActivityTab';
import { AddConnectionDialog } from './AddConnectionDialog';
import { AddToJobDialog } from './AddToJobDialog';
import { ConnectionsTab } from './ConnectionsTab';
import { JobsTab } from './JobsTab';
import { OverviewTab } from './OverviewTab';
import { PersonConnections, PersonDetails } from './PersonAside';
import { PersonEditDrawer } from './PersonEditDrawer';
import { PersonHeader } from './PersonHeader';
import { PrivacyTab } from './PrivacyTab';
import { RelationshipMap } from './RelationshipMap';
import { RelationshipSnapshot } from './RelationshipSnapshot';
import { SendConsentDialog } from './SendConsentDialog';
import { usePerson } from './usePerson';

const tabStyle = { whiteSpace: 'nowrap' } as const;
/** Tab count: plain ink3 text after the label (visual system v3, section 6). */
const Count = ({ n }: { n: number }) => (
  <Text span size="sm" c="var(--color-ink3)" style={{ fontVariantNumeric: 'tabular-nums' }}>
    {n}
  </Text>
);

export function PersonPage() {
  const { id = '' } = useParams();
  const { t, i18n } = useTranslation();
  const d = usePerson(id);
  const [dialog, setDialog] = useState<'edit' | 'log' | 'action' | 'connection' | 'job' | 'consent'>();
  const close = () => setDialog(undefined);
  const [tab, setTab] = useState('overview');
  const [resending, setResending] = useState(false);
  const [notice, setNotice] = useState<{ color: string; text: string }>();
  // One click: the notice goes out in the current UI language (no dialog); the privacy tab keeps the full dialog.
  const resend = async (p: Schemas['Person']) => {
    setResending(true);
    setNotice(undefined);
    try {
      const r = await api.POST('/people/{id}/consent/requests', { params: { path: { id: p.id } }, body: { language: i18n.language === 'en' ? 'EN' : 'TR' } });
      if (r.error) setNotice({ color: 'red', text: problemText(r.error, t('network.consentFailed')) });
      else {
        setNotice({ color: 'green', text: t('network.profile.consentSent', { email: p.email }) });
        d.reloadAll();
      }
    } catch {
      setNotice({ color: 'red', text: t('network.consentFailed') });
    } finally {
      setResending(false);
    }
  };

  if (d.person.loading) {
    return (
      <Container py="md" data-testid="person-loading">
        <Skeleton height={120} mb="md" />
        <Skeleton height={200} />
      </Container>
    );
  }
  const p = d.person.data;
  if (!p) {
    const notFound = (d.person.error as { status?: number } | undefined)?.status === 404;
    return (
      <Container py="md">
        <ErrorBanner message={notFound ? t('network.notFound') : t('network.loadFailed')} hint={notFound ? t('network.notFoundHint') : t('network.loadErrorHint')} onRetry={notFound ? undefined : d.person.reload} />
        {notFound && (
          <Button component={Link} to="/network" mt="sm" variant="light">
            {t('network.backToList')}
          </Button>
        )}
      </Container>
    );
  }
  const links = d.links.data ?? [];
  const apps = d.applications.data ?? [];
  const activities = d.activities.data?.content ?? [];
  const blocked = p.consentStatus === 'REFUSED' || p.consentStatus === 'WITHDRAWN';
  // Consent problems: full-width banner under the header (refused and withdrawn are danger, not yet requested is a warning).
  const consentProblem = blocked || p.consentStatus === 'NOT_SENT';
  const canSendConsent = !!p.email && !p.anonymised;

  const panel = (title: string, body: ReactNode) => <Panel title={title}>{body}</Panel>;
  const tabs = (
    <Tabs.List style={{ flexWrap: 'nowrap', overflowX: 'auto', overflowY: 'hidden', borderBottom: 0 }}>
      <Tabs.Tab value="overview" style={tabStyle}>{t('network.tabs.overview')}</Tabs.Tab>
      <Tabs.Tab value="connections" style={tabStyle} rightSection={<Count n={links.length} />}>
        {t('network.tabs.connections')}
      </Tabs.Tab>
      <Tabs.Tab value="map" style={tabStyle}>{t('network.tabs.map')}</Tabs.Tab>
      <Tabs.Tab value="jobs" style={tabStyle} rightSection={<Count n={apps.length} />}>
        {t('network.tabs.jobs')}
      </Tabs.Tab>
      <Tabs.Tab value="activity" style={tabStyle} rightSection={<Count n={activities.length} />}>
        {t('network.tabs.activity')}
      </Tabs.Tab>
      <Tabs.Tab value="files" style={tabStyle}>{t('network.tabs.files')}</Tabs.Tab>
      <Tabs.Tab value="exposure" style={tabStyle}>{t('network.tabs.exposure')}</Tabs.Tab>
      <Tabs.Tab value="privacy" style={tabStyle}>{t('network.tabs.privacy')}</Tabs.Tab>
      <Tabs.Tab value="history" style={tabStyle}>{t('network.edit.history')}</Tabs.Tab>
    </Tabs.List>
  );
  const banner =
    consentProblem && !p.anonymised ? (
      <Alert color={blocked ? 'red' : 'yellow'} variant="light" icon={<ConsentIcon size={ICON_SIZE} stroke={ICON_STROKE} />} data-testid="consent-banner">
        <Group justify="space-between" align="center" wrap="wrap" gap="sm">
          <Text size="sm" c="var(--color-ink)">
            {t(`network.consentBanner.${p.consentStatus}`)}
            {!p.email && ` ${t('network.consentNoEmail')}`}
          </Text>
          {canSendConsent && (
            <Button variant="default" onClick={() => setDialog('consent')}>
              {t('network.sendConsent')}
            </Button>
          )}
        </Group>
      </Alert>
    ) : undefined;
  const nextActions = d.actions.data?.content ?? [];

  return (
    <Tabs value={tab} onChange={(v) => v && setTab(v)} keepMounted={false} variant="underline">
      {notice && (
        <Alert color={notice.color} m="md" role={notice.color === 'red' ? 'alert' : 'status'} withCloseButton onClose={() => setNotice(undefined)}>
          {notice.text}
        </Alert>
      )}
      <RecordLayout
        header={<PersonHeader person={p} onLog={() => setDialog('log')} onAction={() => setDialog('action')} onResend={() => void resend(p)} onEdit={() => setDialog('edit')} resending={resending} searchCount={apps.length} tabs={tabs} />}
        banner={banner}
        main={
          <>
            <Tabs.Panel value="overview">
              <Stack gap="lg">
                <RelationshipSnapshot person={p} activities={activities} actions={nextActions} linkCount={links.length} jobCount={apps.length} />
                <OverviewTab person={p} actions={nextActions} activities={activities} roles={d.roles.data ?? []} onAddAction={() => setDialog('action')} onEdit={() => setDialog('edit')} onViewAllActivity={() => setTab('activity')} />
              </Stack>
            </Tabs.Panel>
            <Tabs.Panel value="map">{panel(t('network.tabs.map'), <RelationshipMap center={p} links={links} />)}</Tabs.Panel>
            <Tabs.Panel value="connections">{panel(t('network.tabs.connections'), <ConnectionsTab links={links} onAdd={() => setDialog('connection')} />)}</Tabs.Panel>
            <Tabs.Panel value="jobs">{panel(t('network.tabs.jobs'), <JobsTab applications={apps} roles={d.roles.data ?? []} canAdd={!blocked} onAdd={() => setDialog('job')} />)}</Tabs.Panel>
            <Tabs.Panel value="activity">{panel(t('network.tabs.activity'), <ActivityTab activities={activities} onLog={() => setDialog('log')} />)}</Tabs.Panel>
            <Tabs.Panel value="files">{panel(t('network.tabs.files'), <FilesTab personId={id} anonymised={p.anonymised} onUploaded={d.reloadAll} />)}</Tabs.Panel>
            <Tabs.Panel value="exposure">{panel(t('network.tabs.exposure'), <ExposureTab personId={id} />)}</Tabs.Panel>
            <Tabs.Panel value="privacy">{panel(t('network.tabs.privacy'), <PrivacyTab consent={d.consent.data} onSend={() => setDialog('consent')} />)}</Tabs.Panel>
            <Tabs.Panel value="history">{panel(t('network.edit.history'), <HistoryPanel entityType="PERSON" entityId={id} />)}</Tabs.Panel>
          </>
        }
        aside={
          <>
            <PersonDetails person={p} />
            <PersonConnections links={links} onViewAll={() => setTab('connections')} />
          </>
        }
      />

      <PersonEditDrawer person={p} opened={dialog === 'edit'} onClose={close} onSaved={d.reloadAll} />
      <QuickLog opened={dialog === 'log'} onClose={close} personId={id} onDone={d.reloadAll} />
      <QuickAdd opened={dialog === 'action'} initialMode="action" onClose={close} context={{ person: p }} onSaved={d.reloadAll} />
      <AddConnectionDialog personId={id} opened={dialog === 'connection'} onClose={close} onSaved={d.links.reload} />
      <AddToJobDialog personId={id} exclude={apps.map((a) => a.jobOrder.id)} opened={dialog === 'job'} onClose={close} onSaved={d.applications.reload} />
      <SendConsentDialog person={p} opened={dialog === 'consent'} onClose={close} onSent={d.reloadAll} />
    </Tabs>
  );
}
