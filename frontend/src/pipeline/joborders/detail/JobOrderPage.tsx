// Job order page with the pipeline board (A-06, A-07, UX-03, UX-04).
import { Alert, Anchor, Button, Card, Container, Group, Loader, Skeleton, Text, Title } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router-dom';
import { api } from '../../../api';
import { Board } from '../../board/Board';
import { ReasonDialog } from '../../board/ReasonDialog';
import { useBoardMoves } from '../../board/useBoardMoves';
import { QuickAdd, type QuickAddContext } from '../../quickadd/QuickAdd';
import { StatusBadge, problemText, useCurrentUser, useFormat, useLabel, type Schemas } from '../../shared';
import { AddCandidateDialog } from './AddCandidateDialog';
import { JobStatusMenu } from './JobStatusMenu';
import { MoveToast } from './MoveToast';
import { useJobOrder } from './useJobOrder';

export function JobOrderPage() {
  const { id = '' } = useParams();
  const { t } = useTranslation();
  const fmt = useFormat();
  const { ref, def } = useLabel();
  const me = useCurrentUser();
  const { job, apps: appsRes } = useJobOrder(id);
  const moves = useBoardMoves(appsRes.data, job.reload);
  const [addOpen, setAddOpen] = useState(false);
  const [logCtx, setLogCtx] = useState<QuickAddContext>();
  const [statusAsk, setStatusAsk] = useState<string>();

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

  return (
    <Container py="md" fluid>
      <Group gap="xs" mb="xs">
        <Anchor component={Link} to={`/accounts/${j.account.id}`} size="sm">
          {j.account.name}
        </Anchor>
        <Text size="sm" c="dimmed">
          / {j.title}
        </Text>
      </Group>
      <Group justify="space-between" align="flex-start">
        <Group gap="sm">
          <Title order={1} data-testid="screen-title" size="h2">
            {j.title}
          </Title>
          <JobStatusMenu
            status={j.status}
            isManager={isManager}
            onChoose={(to, reasonRequired) => (reasonRequired ? setStatusAsk(to) : void changeStatus(to))}
          />
        </Group>
        <Group gap="xs">
          {!readOnly && <Button onClick={() => setAddOpen(true)}>+ {t('pipeline.addCandidate')}</Button>}
          <Button variant="default" onClick={() => setLogCtx({ account: j.account, jobOrder: j })}>
            {t('pipeline.logActivity')}
          </Button>
        </Group>
      </Group>
      <Text size="sm" c="dimmed" mt={4}>
        <StatusBadge lifecycle="account" value={j.account.status} /> {[j.location, j.seniority].filter(Boolean).join(' · ')}
        {j.feeTerms ? ` · ${t('pipeline.fee')}: ${j.feeTerms}` : ''} · {t('pipeline.owner')}: {j.owner.name}
      </Text>
      <Text size="sm" c="dimmed" mb="md">
        {t('pipeline.opened')}: {fmt.dateLong(j.openedOn)} ({t('pipeline.daysOpen', { count: j.daysOpen })}) ·{' '}
        {t('pipeline.activeCandidates', { count: j.activeCandidates })}
        {rejectedCount > 0 ? ` · ${t('pipeline.rejectedLane', { count: rejectedCount })}` : ''}
      </Text>

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

      {appsRes.loading && moves.apps.length === 0 ? (
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
        <Board
          apps={moves.apps}
          ref={ref}
          readOnly={readOnly}
          onMove={(a, to, reason) => void moves.move(a, to, reason)}
          onRemove={(a) => void moves.remove(a)}
          onLogActivity={(a) => setLogCtx({ account: j.account, person: a.person, jobOrder: j })}
        />
      )}

      <AddCandidateDialog
        jobId={id}
        opened={addOpen}
        onClose={() => setAddOpen(false)}
        onAdded={() => {
          appsRes.reload();
          job.reload();
        }}
      />
      <QuickAdd opened={!!logCtx} onClose={() => setLogCtx(undefined)} context={logCtx} />
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
