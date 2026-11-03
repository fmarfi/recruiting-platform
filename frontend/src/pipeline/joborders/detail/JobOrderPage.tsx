// Job order page with the pipeline board (A-06, A-07, UX-03, UX-04). Header on the canvas, facts and brief below it, board in one panel.
import { Alert, Anchor, Box, Button, Container, Group, Skeleton, Stack, Text } from '@mantine/core';
import { IconPlus } from '@tabler/icons-react';
import { useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router-dom';
import { api } from '../../../api';
import { Board } from '../../board/Board';
import { ReasonDialog } from '../../board/ReasonDialog';
import { useBoardMoves } from '../../board/useBoardMoves';
import { QuickAdd, type QuickAddContext } from '../../quickadd/QuickAdd';
import { StatusBadge, problemText, useCurrentUser, useFormat, useLabel, type Schemas } from '../../shared';
import { SectionToggle } from '../../shared/SectionToggle';
import { StageGuardDialog } from '../../board/StageGuardDialog';
import { EmptyState } from '../../../shell/components/EmptyState';
import { ErrorBanner } from '../../../shell/components/ErrorBanner';
import { PageHeader } from '../../../shell/components/PageHeader';
import { SkeletonRows } from '../../../shell/components/SkeletonRows';
import { HistoryPanel } from '../../../shell/edit';
import { AddCandidateDialog } from './AddCandidateDialog';
import { JobOrderEditDrawer } from './JobOrderEditDrawer';
import { JobStatusMenu } from './JobStatusMenu';
import { MoveToast } from './MoveToast';
import { useJobOrder } from './useJobOrder';

const LABEL_STYLE = { color: 'var(--color-ink3)' } as const;

/** Fact row under the title (v3 section 5): label 12 ink3 above value 14 ink, no boxes around the facts. */
function FactRow({ items }: { items: { label: string; value: ReactNode }[] }) {
  return (
    <Box
      component="dl"
      m={0}
      data-testid="job-highlights"
      style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: 'var(--space-3) var(--space-5)' }}
    >
      {items.map((h) => (
        <div key={h.label} style={{ minWidth: 0 }}>
          <Text component="dt" size="xs" fw={500} style={LABEL_STYLE}>
            {h.label}
          </Text>
          <Text component="dd" m={0} size="sm" style={{ marginTop: 'var(--space-2)', color: 'var(--color-ink)', overflowWrap: 'anywhere' }}>
            {h.value}
          </Text>
        </div>
      ))}
    </Box>
  );
}

export function JobOrderPage() {
  const { id = '' } = useParams();
  const { t } = useTranslation();
  const fmt = useFormat();
  const { ref, def } = useLabel();
  const me = useCurrentUser();
  const { job, apps: appsRes } = useJobOrder(id);
  const moves = useBoardMoves(appsRes.data, job.reload, appsRes.reload);
  const [addOpen, setAddOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [historyOpen, setHistoryOpen] = useState(false);
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
        <ErrorBanner message={notFound ? t('pipeline.notFound') : t('pipeline.loadFailed')} hint={notFound ? t('pipeline.notFoundHint') : t('pipeline.loadErrorHint')} onRetry={notFound ? undefined : job.reload} />
        {notFound && (
          <Button component={Link} to="/job-orders" mt="sm" variant="default">
            {t('pipeline.backToList')}
          </Button>
        )}
      </Container>
    );
  }

  const next = def('jobOrder', j.status)?.next ?? [];
  const addBtn = (
    <Button leftSection={<IconPlus size={16} stroke={1.75} aria-hidden />} onClick={() => setAddOpen(true)}>
      {t('pipeline.addCandidate')}
    </Button>
  );

  return (
    <Container maw="var(--content-max-width)" fluid>
      <PageHeader
        breadcrumb={
          <Anchor component={Link} to={`/accounts/${j.account.id}`} size="sm" c="var(--color-ink3)">
            {j.account.name}
          </Anchor>
        }
        title={j.title}
        badge={
          <JobStatusMenu
            status={j.status}
            isManager={isManager}
            onChoose={(to, reasonRequired) => (reasonRequired ? setStatusAsk(to) : void changeStatus(to))}
          />
        }
        actions={
          <>
            <Button variant="subtle" onClick={() => setEditOpen(true)} data-testid="job-edit">
              {t('pipeline.edit.action')}
            </Button>
            <Button variant="default" onClick={() => setLogCtx({ account: j.account, jobOrder: j })}>
              {t('pipeline.logActivity')}
            </Button>
            {!readOnly && addBtn}
          </>
        }
      />
      <FactRow
        items={[
          { label: t('pipeline.hl.client'), value: <Group gap={6}><Anchor component={Link} to={`/accounts/${j.account.id}`}>{j.account.name}</Anchor><StatusBadge lifecycle="account" value={j.account.status} /></Group> },
          { label: t('pipeline.hl.location'), value: j.location || '-' },
          { label: t('pipeline.hl.seniority'), value: j.seniority || '-' },
          { label: t('pipeline.hl.owner'), value: j.owner.name },
          { label: t('pipeline.hl.opened'), value: `${fmt.dateLong(j.openedOn)} (${t('pipeline.daysOpen', { count: j.daysOpen })})` },
          { label: t('pipeline.hl.candidates'), value: `${j.activeCandidates}` },
        ]}
      />
      {(j.feeTerms || j.description) && (
        <Stack gap="sm" mt="xl" data-testid="job-brief">
          <Text component="h2" m={0} fw={600} size="md">
            {t('pipeline.brief')}
          </Text>
          {j.feeTerms && (
            <Stack gap="xs">
              <Text size="xs" fw={500} style={LABEL_STYLE}>
                {t('pipeline.fee')}
              </Text>
              <Text size="sm" style={{ whiteSpace: 'pre-wrap' }}>
                {j.feeTerms}
              </Text>
            </Stack>
          )}
          {j.description && (
            <Text size="sm" style={{ whiteSpace: 'pre-wrap', color: 'var(--color-ink2)' }}>
              {j.description}
            </Text>
          )}
        </Stack>
      )}

      <Stack gap="md" mt="xl">
        {readOnly && (
          <Alert color={j.status === 'FILLED' ? 'green' : 'gray'} data-testid="readonly-banner">
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
          <Alert color="green" withCloseButton onClose={() => moves.setSuggestFilled(false)}>
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
          <SkeletonRows n={4} />
        ) : appsRes.error ? (
          <ErrorBanner message={t('pipeline.loadFailed')} hint={t('pipeline.loadErrorHint')} onRetry={appsRes.reload} />
        ) : moves.apps.length === 0 ? (
          <div data-testid="empty-pipeline">
            <EmptyState title={t('pipeline.emptyTitle')} hint={t('pipeline.emptyBody')} actions={!readOnly ? addBtn : undefined} />
          </div>
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

        <SectionToggle title={t('pipeline.edit.history')} open={historyOpen} onToggle={() => setHistoryOpen((o) => !o)} testId="job-history">
          {historyOpen && <HistoryPanel entityType="JOB_ORDER" entityId={id} headingOrder={null} />}
        </SectionToggle>
      </Stack>
      <JobOrderEditDrawer job={j} opened={editOpen} onClose={() => setEditOpen(false)} onSaved={job.reload} />
      <AddCandidateDialog
        jobId={id}
        opened={addOpen}
        onClose={() => setAddOpen(false)}
        onAdded={() => {
          appsRes.reload();
          job.reload();
        }}
      />
      <StageGuardDialog
        guard={moves.guard}
        role={me?.role}
        onClose={moves.clearGuard}
        onOverride={(g, reason) => {
          moves.clearGuard();
          void moves.move(g.app, g.to, g.reason, false, reason);
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
