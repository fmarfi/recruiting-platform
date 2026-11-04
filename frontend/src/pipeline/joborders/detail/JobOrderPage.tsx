// Search (job order) page with the pipeline board (A-06, A-07, UX-03, UX-04). Record band with facts, full-width pipeline,
// brief and details side by side, history last (visual system v3, section 5; reference: network/person).
import { Alert, Anchor, Box, Button, Container, Group, Menu, SegmentedControl, Skeleton, SimpleGrid, Stack, Text } from '@mantine/core';
import { BulkAddDialog } from '../../longlist/BulkAddDialog';
import { LonglistPanel } from '../../longlist/LonglistPanel';
import { IconDots, IconPlus } from '@tabler/icons-react';
import { useMemo, useState } from 'react';
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
import { DetailList } from '../../../shell/components/DetailList';
import { EmptyState } from '../../../shell/components/EmptyState';
import { ErrorBanner } from '../../../shell/components/ErrorBanner';
import { Panel } from '../../../shell/components/Panel';
import { RecordHeader } from '../../../shell/components/RecordHeader';
import { SkeletonRows } from '../../../shell/components/SkeletonRows';
import { ICON_SIZE, ICON_STROKE, SearchIcon } from '../../../shell/components/objectIcons';
import { HistoryPanel } from '../../../shell/edit';
import { AddCandidateDialog } from './AddCandidateDialog';
import { JobOrderEditDrawer } from './JobOrderEditDrawer';
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
  const moves = useBoardMoves(appsRes.data, job.reload, appsRes.reload);
  const [addOpen, setAddOpen] = useState(false);
  const [bulkOpen, setBulkOpen] = useState(false);
  const existingIds = useMemo(() => new Set(moves.apps.map((a) => a.person.id)), [moves.apps]);
  const [view, setView] = useState('board');
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
  const rejectedCount = moves.apps.filter((a) => a.stage === 'REJECTED').length;
  const showFilledAsk = moves.suggestFilled && next.some((n) => n.to === 'FILLED');
  const showBanner = readOnly || showFilledAsk || !!moves.toast;
  const addBtn = (
    <Button leftSection={<IconPlus size={16} stroke={1.75} aria-hidden />} onClick={() => setAddOpen(true)}>
      {t('pipeline.addCandidate')}
    </Button>
  );
  // Three actions: Log activity (secondary), Add candidate (the one primary), Edit inside the More menu.
  const actions = (
    <>
      <Menu position="bottom-end" shadow="md" withinPortal>
        <Menu.Target>
          <Button variant="default" aria-label={t('pipeline.moreActions')}>
            <IconDots size={ICON_SIZE} stroke={ICON_STROKE} />
          </Button>
        </Menu.Target>
        <Menu.Dropdown>
          <Menu.Item onClick={() => setEditOpen(true)} data-testid="job-edit">
            {t('pipeline.edit.action')}
          </Menu.Item>
        </Menu.Dropdown>
      </Menu>
      <Button variant="default" onClick={() => setLogCtx({ account: j.account, jobOrder: j })}>
        {t('pipeline.logActivity')}
      </Button>
      {!readOnly && addBtn}
    </>
  );

  return (
    <>
      <div className="record-band" data-testid="record-band">
        <RecordHeader
          avatar={<SearchIcon size={32} stroke={ICON_STROKE} color="var(--color-ink3)" aria-hidden />}
          name={j.title}
          badge={
            <JobStatusMenu
              status={j.status}
              isManager={isManager}
              onChoose={(to, reasonRequired) => (reasonRequired ? setStatusAsk(to) : void changeStatus(to))}
            />
          }
          subtitle={
            <>
              <Anchor component={Link} to={`/accounts/${j.account.id}`} size="sm">
                {j.account.name}
              </Anchor>
              {j.location && `, ${j.location}`}
            </>
          }
          actions={actions}
          facts={[
            { label: t('pipeline.hl.seniority'), value: j.seniority || '–' },
            { label: t('pipeline.hl.owner'), value: j.owner.name },
            { label: t('pipeline.hl.opened'), value: `${fmt.dateLong(j.openedOn)} (${t('pipeline.daysOpen', { count: j.daysOpen })})` },
            { label: t('pipeline.hl.candidates'), value: j.activeCandidates },
            { label: t('pipeline.factRejected'), value: rejectedCount },
          ]}
        />
      </div>

      {showBanner && (
        <div className="record-banner">
          <Stack gap="md">
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
            {showFilledAsk && (
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
          </Stack>
        </div>
      )}

      <div className="record-body" style={{ gridTemplateColumns: 'minmax(0, 1fr)' }}>
        <div className="record-main">
          {appsRes.loading && moves.apps.length === 0 ? (
            <Panel title={t('pipeline.panelPipeline')}>
              <SkeletonRows n={4} />
            </Panel>
          ) : appsRes.error ? (
            <ErrorBanner message={t('pipeline.loadFailed')} hint={t('pipeline.loadErrorHint')} onRetry={appsRes.reload} />
          ) : moves.apps.length === 0 ? (
            <Panel title={t('pipeline.panelPipeline')} count={0}>
              <div data-testid="empty-pipeline">
                <EmptyState title={t('pipeline.emptyTitle')} hint={t('pipeline.emptyBody')} actions={!readOnly ? <Group gap="sm">{addBtn}<Button variant="default" onClick={() => setBulkOpen(true)}>{t('pipeline.ll.bulkAdd')}</Button></Group> : undefined} />
              </div>
            </Panel>
          ) : (
            <>
              <SegmentedControl
                size="xs"
                w="fit-content"
                value={view}
                onChange={setView}
                data={[
                  { value: 'board', label: t('pipeline.ll.viewBoard') },
                  { value: 'longlist', label: t('pipeline.ll.viewLonglist') },
                ]}
              />
              {view === 'longlist' ? (
                <LonglistPanel
                  jobId={id}
                  apps={moves.apps}
                  readOnly={readOnly}
                  reload={() => {
                    appsRes.reload();
                    job.reload();
                  }}
                  onBulkAdd={() => setBulkOpen(true)}
                />
              ) : (
            // Board renders the "Pipeline" panel and the "Rejected" panel (one drag context for both).
            <Board
              apps={moves.apps}
              ref={ref}
              readOnly={readOnly}
              onMove={(a, to, reason) => void moves.move(a, to, reason)}
              onRemove={(a) => void moves.remove(a)}
              onLogActivity={(a) => setLogCtx({ account: j.account, person: a.person, jobOrder: j })}
            />
              )}
            </>
          )}

          <SimpleGrid cols={{ base: 1, md: 2 }} spacing="lg">
            <Panel
              title={t('pipeline.brief')}
              testId="job-brief"
              empty={!j.description && !j.feeTerms ? t('pipeline.noBrief') : undefined}
            >
              <Stack gap="md">
                {j.description && (
                  <Text size="sm" c="var(--color-ink2)" style={{ whiteSpace: 'pre-wrap' }}>
                    {j.description}
                  </Text>
                )}
                <DetailList items={[{ label: t('pipeline.fee'), value: j.feeTerms ? <Box component="span" style={{ whiteSpace: 'pre-wrap' }}>{j.feeTerms}</Box> : undefined }]} />
              </Stack>
            </Panel>
            <Panel title={t('pipeline.panelDetails')} testId="job-details">
              <DetailList
                items={[
                  {
                    label: t('pipeline.hl.client'),
                    value: (
                      <Group gap="xs" wrap="nowrap">
                        <Anchor component={Link} to={`/accounts/${j.account.id}`} size="sm">
                          {j.account.name}
                        </Anchor>
                        <StatusBadge lifecycle="account" value={j.account.status} />
                      </Group>
                    ),
                  },
                  { label: t('pipeline.hl.location'), value: j.location },
                  { label: t('pipeline.hl.seniority'), value: j.seniority },
                  { label: t('pipeline.hl.owner'), value: j.owner.name },
                  { label: t('pipeline.hl.opened'), value: fmt.dateLong(j.openedOn) },
                  { label: t('pipeline.list.status'), value: <StatusBadge lifecycle="jobOrder" value={j.status} /> },
                  { label: t('pipeline.lastUpdated'), value: fmt.dateLong(j.updatedAt) },
                ]}
              />
            </Panel>
          </SimpleGrid>

          <SectionToggle title={t('pipeline.edit.history')} open={historyOpen} onToggle={() => setHistoryOpen((o) => !o)} testId="job-history">
            {historyOpen && (
              <Box p="md">
                <HistoryPanel entityType="JOB_ORDER" entityId={id} headingOrder={null} />
              </Box>
            )}
          </SectionToggle>
        </div>
      </div>

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
      <BulkAddDialog
        jobId={id}
        existing={existingIds}
        opened={bulkOpen}
        onClose={() => setBulkOpen(false)}
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
    </>
  );
}
