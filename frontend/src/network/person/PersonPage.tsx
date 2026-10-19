// Person page (A-12, A-13, S-01): relationship, every pipeline the person is in, network links, consent status.
import { Alert, Anchor, Button, Container, Skeleton, Tabs, Text } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router-dom';
import { QuickAdd } from '../../pipeline/quickadd/QuickAdd';
import { ActivityTab } from './ActivityTab';
import { AddConnectionDialog } from './AddConnectionDialog';
import { AddToJobDialog } from './AddToJobDialog';
import { ConnectionsTab } from './ConnectionsTab';
import { JobsTab } from './JobsTab';
import { OverviewTab } from './OverviewTab';
import { PersonHeader } from './PersonHeader';
import { PrivacyTab } from './PrivacyTab';
import { SendConsentDialog } from './SendConsentDialog';
import { usePerson } from './usePerson';

export function PersonPage() {
  const { id = '' } = useParams();
  const { t } = useTranslation();
  const d = usePerson(id);
  const [dialog, setDialog] = useState<'log' | 'action' | 'connection' | 'job' | 'consent'>();
  const close = () => setDialog(undefined);

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
        <Alert color="red" role="alert" title={notFound ? t('network.notFound') : t('network.loadFailed')}>
          <Button variant="light" onClick={d.person.reload}>
            {t('common.retry')}
          </Button>
        </Alert>
      </Container>
    );
  }
  const links = d.links.data ?? [];
  const apps = d.applications.data ?? [];
  const activities = d.activities.data?.content ?? [];
  const blocked = p.consentStatus === 'REFUSED' || p.consentStatus === 'WITHDRAWN';

  return (
    <Container maw="var(--content-max-width)" fluid>
      <Anchor component={Link} to="/network" size="sm">
        {t('nav.network')}
      </Anchor>{' '}
      <Text span size="sm" c="dimmed">
        / {p.name}
      </Text>
      {blocked && (
        <Alert color="red" my="sm" data-testid="do-not-contact">
          {t('network.doNotContact')}
        </Alert>
      )}
      <PersonHeader person={p} onLog={() => setDialog('log')} onAction={() => setDialog('action')} />
      <Tabs defaultValue="overview" keepMounted={false}>
        <Tabs.List mb="md">
          <Tabs.Tab value="overview">{t('network.tabs.overview')}</Tabs.Tab>
          <Tabs.Tab value="connections">
            {t('network.tabs.connections')} {links.length}
          </Tabs.Tab>
          <Tabs.Tab value="jobs">
            {t('network.tabs.jobs')} {apps.length}
          </Tabs.Tab>
          <Tabs.Tab value="activity">
            {t('network.tabs.activity')} {activities.length}
          </Tabs.Tab>
          <Tabs.Tab value="privacy">{t('network.tabs.privacy')}</Tabs.Tab>
        </Tabs.List>
        <Tabs.Panel value="overview">
          <OverviewTab person={p} actions={d.actions.data?.content ?? []} roles={d.roles.data ?? []} />
        </Tabs.Panel>
        <Tabs.Panel value="connections">
          <ConnectionsTab links={links} onAdd={() => setDialog('connection')} />
        </Tabs.Panel>
        <Tabs.Panel value="jobs">
          <JobsTab applications={apps} roles={d.roles.data ?? []} canAdd={!blocked} onAdd={() => setDialog('job')} />
        </Tabs.Panel>
        <Tabs.Panel value="activity">
          <ActivityTab activities={activities} onLog={() => setDialog('log')} />
        </Tabs.Panel>
        <Tabs.Panel value="privacy">
          <PrivacyTab consent={d.consent.data} onSend={() => setDialog('consent')} />
        </Tabs.Panel>
      </Tabs>

      <QuickAdd
        opened={dialog === 'log' || dialog === 'action'}
        initialMode={dialog === 'action' ? 'action' : 'activity'}
        onClose={close}
        context={{ person: p }}
        onSaved={d.reloadAll}
      />
      <AddConnectionDialog personId={id} opened={dialog === 'connection'} onClose={close} onSaved={d.links.reload} />
      <AddToJobDialog personId={id} exclude={apps.map((a) => a.jobOrder.id)} opened={dialog === 'job'} onClose={close} onSaved={d.applications.reload} />
      <SendConsentDialog person={p} opened={dialog === 'consent'} onClose={close} onSent={d.reloadAll} />
    </Container>
  );
}
