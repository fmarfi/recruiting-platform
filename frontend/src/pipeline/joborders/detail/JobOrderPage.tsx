// Job order page with the pipeline board (A-06, A-07, UX-03, UX-04).
import { Alert, Anchor, Button, Card, Container, Group, Loader, Skeleton, Tabs, Text } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router-dom';
import { api } from '../../../api';
import { ActivityTimeline } from '../../../common/activity/ActivityTimeline';
import type { ComposerChoice } from '../../../common/activity/ActivityComposer';
import { RecordLayout } from '../../../common/record/RecordLayout';
import type { Application } from '../../board/boardRules';
import { ReasonDialog } from '../../board/ReasonDialog';
import { useBoardMoves } from '../../board/useBoardMoves';
import { QuickAdd, type QuickAddContext } from '../../quickadd/QuickAdd';
import { StatusBadge, problemText, useCurrentUser, useLabel, type Schemas } from '../../shared';
import { AddCandidateDialog } from './AddCandidateDialog';
import { CandidateDrawer } from './CandidateDrawer';
import { CandidatesCard } from './CandidatesCard';
import { JobHeader } from './JobHeader';
import { MoveToast } from './MoveToast';
import { useJobOrder } from './useJobOrder';

export function JobOrderPage() {
  const { id = '' } = useParams();
  const { t } = useTranslation();
  const { ref, def } = useLabel();
  const me = useCurrentUser();
  const { job, apps: appsRes, activities, actions } = useJobOrder(id);
  const moves = useBoardMoves(appsRes.data, job.reload);
  const [addOpen, setAddOpen] = useState(false);
  const [logCtx, setLogCtx] = useState<QuickAddContext>();
  const [statusAsk, setStatusAsk] = useState<string>();
  const [openAppId, setOpenAppId] = useState<string>();
  const [rejectAsk, setRejectAsk] = useState<Application>();
  const [compose, setCompose] = useState<ComposerChoice>();

  const j = job.data;
  const readOnly = !!j && (j.status === 'FILLED' || j.status === 'CANCELLED');
  const isManager = me?.role === 'MANAGER' || me?.role === 'ADMIN';

  const changeStatus = async (status: string, reason?: string) => {
    if (!j) return;
    const r = await api.PATCH('/job-orders/{id}/status', {
      params: { path: { id } },
      body: { status: status as Schemas['JobOrderStatus'], ...(reason ? { reason } : {}), version: j.version },
    });
    if (r.error) moves.show({ kind: 'error', text: problemText(r.error, t('pipeline.statusFailed')) });
    else {
      moves.setSuggestFilled(false);
      job.reload();
    }
  };

  if (job.loading) {
    return (
      <Container py="md" data-testid="board-loading">
        <Skeleton height={32} width={240} mb="md" />
        <Skeleton height={200} />
      </Container>
    );
  }
  if (!j) {
    const notFound = (job.error as { status?: number } | undefined)?.status === 404;
    return (
      <Container py="md">
        <Alert color="red" role="alert" title={notFound ? t('pipeline.notFound') : t('pipeline.loadFailed')}>
          <Button variant="light" onClick={job.reload}>
            {t('common.retry')}
          </Button>
        </Alert>
      </Container>
    );
  }

  const next = def('jobOrder', j.status)?.next ?? [];
  const rejectedCount = moves.apps.filter((a) => a.stage === 'REJECTED').length;

  const board = {
    apps: moves.apps,
    ref,
    readOnly,
    onMove: (a: Application, to: Application['stage'], reason?: string) => void moves.move(a, to, reason),
    onRemove: (a: Application) => void moves.remove(a),
    onLogActivity: (a: Application) => setLogCtx({ account: j.account, person: a.person, jobOrder: j }),
    onOpen: (a: Application) => setOpenAppId(a.id),
  };

  const candidates = (
    appsRes.loading && moves.apps.length === 0 ? (
          <Loader size="sm" />
        ) : appsRes.error ? (
          <Alert color="red" role="alert" title={t('pipeline.loadFailed')}>
            <Button variant="light" onClick={appsRes.reload}>
              {t('common.retry')}
            </Button>
          </Alert>
        ) : moves.apps.length === 0 ? (
          <Card withBorder padding="xl" ta="center" data-testid="empty-pipeline">
            <Text fw={600}>{t('pipeline.emptyTitle')}</Text>
            <Text c="dimmed" size="sm" mb="md">
              {t('pipeline.emptyBody')}
            </Text>
            {!readOnly && <Button onClick={() => setAddOpen(true)}>{t('pipeline.addCandidate')}</Button>}
          </Card>
        ) : (
          <CandidatesCard rejectedCount={rejectedCount} board={board} />
  )
  );

  return (
    <Container maw="var(--content-max-width)" fluid>
      <JobHeader
        job={j}
        isManager={isManager}
        readOnly={readOnly}
        onStatus={(to, reasonRequired) => (reasonRequired ? setStatusAsk(to) : void changeStatus(to))}
        onAdd={() => setAddOpen(true)}
        onLog={() => setLogCtx({ account: j.account, jobOrder: j })}
      />

      {readOnly && (
        <Alert color={j.status === 'FILLED' ? 'green' : 'gray'} mb="md" data-testid="readonly-banner">
          <Group justify="space-between">
            <Text size="sm">{t(j.status === 'FILLED' ? 'pipeline.bannerFilled' : 'pipeline.bannerCancelled')}</Text>
            {next.map((n) => (
              <Button key={n.to} size="compact-sm" disabled={n.managerOnly && !isManager} onClick={() => void changeStatus(n.to)}>
                {t('pipeline.reopenJob')}
              </Button>
            ))}
          </Group>
        </Alert>
      )}
      {moves.suggestFilled && next.some((n) => n.to === 'FILLED') && (
        <Alert mb="md" color="green" withCloseButton onClose={() => moves.setSuggestFilled(false)}>
          <Group justify="space-between">
            <Text size="sm">{t('pipeline.markFilledAsk')}</Text>
            <Button size="compact-sm" onClick={() => void changeStatus('FILLED')}>
              {t('pipeline.markFilled')}
            </Button>
          </Group>
        </Alert>
      )}
      {moves.toast && <MoveToast toast={moves.toast} onClose={() => moves.setToast(undefined)} onRefresh={appsRes.reload} />}

      <RecordLayout
        main={
          <Tabs defaultValue="candidates" keepMounted={false}>
            <Tabs.List mb="md">
              <Tabs.Tab value="candidates">{t('pipeline.candidates')}</Tabs.Tab>
              <Tabs.Tab value="activity">{t('record.activity')}</Tabs.Tab>
            </Tabs.List>
            <Tabs.Panel value="candidates">{candidates}</Tabs.Panel>
            <Tabs.Panel value="activity">
              <ActivityTimeline
                activities={activities.data?.content ?? []}
                actions={actions.data?.content ?? []}
                onCompose={setCompose}
              />
            </Tabs.Panel>
          </Tabs>
        }
        aside={
          <Card withBorder padding="md" data-testid="client-card">
            <Text fw={600} mb="xs">
              {t('pipeline.client')}
            </Text>
            <Anchor component={Link} to={`/accounts/${j.account.id}`}>
              {j.account.name}
            </Anchor>
            <div style={{ marginTop: 6 }}>
              <StatusBadge lifecycle="account" value={j.account.status} />
            </div>
          </Card>
        }
      />

      <AddCandidateDialog
        jobId={id}
        opened={addOpen}
        onClose={() => setAddOpen(false)}
        onAdded={() => {
          appsRes.reload();
          job.reload();
        }}
      />
      <CandidateDrawer
        app={moves.apps.find((a) => a.id === openAppId)}
        readOnly={readOnly}
        isManager={isManager}
        onClose={() => setOpenAppId(undefined)}
        onStage={(a, to, reasonRequired) => (reasonRequired ? setRejectAsk(a) : void moves.move(a, to as Application['stage']))}
        onLog={(a) => setLogCtx({ account: j.account, person: a.person, jobOrder: j })}
      />
      <ReasonDialog
        opened={!!rejectAsk}
        title={t('pipeline.rejectTitle')}
        chips={t('pipeline.rejectChips', { returnObjects: true }) as unknown as string[]}
        onCancel={() => setRejectAsk(undefined)}
        onConfirm={(reason) => {
          if (rejectAsk) void moves.move(rejectAsk, 'REJECTED', reason);
          setRejectAsk(undefined);
        }}
      />
      <QuickAdd opened={!!logCtx} onClose={() => setLogCtx(undefined)} context={logCtx} onSaved={activities.reload} />
      <QuickAdd
        opened={!!compose}
        initialMode={compose?.mode}
        initialType={compose?.type}
        onClose={() => setCompose(undefined)}
        context={{ account: j.account, jobOrder: j }}
        onSaved={() => {
          activities.reload();
          actions.reload();
        }}
      />
      <ReasonDialog
        opened={!!statusAsk}
        title={t('pipeline.statusReason')}
        onCancel={() => setStatusAsk(undefined)}
        onConfirm={(reason) => {
          if (statusAsk) void changeStatus(statusAsk, reason);
          setStatusAsk(undefined);
        }}
      />
    </Container>
  );
}
