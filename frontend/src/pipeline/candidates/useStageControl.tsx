// Stage change from a table row or the person page: one hook that owns the rules and their dialogs.
// Longlist to shortlist goes through ShortlistDialog (rating and rationale); a move that needs a reason asks for it;
// consent and off-limits refusals open StageGuardDialog. Everything else is a plain stage move with an undo toast.
import { useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { legalMoves, type Stage } from '../board/boardRules';
import { ReasonDialog } from '../board/ReasonDialog';
import { StageGuardDialog } from '../board/StageGuardDialog';
import { warningLines } from '../board/stageGuard';
import { useBoardMoves } from '../board/useBoardMoves';
import { MoveToast } from '../joborders/detail/MoveToast';
import { ShortlistDialog } from '../longlist/ShortlistDialog';
import { shortlistApp, type App } from '../longlist/useLonglist';
import { useCurrentUser, useLabel, type RefData } from '../shared';

/** Client-facing stages: a move into one of them from the longlist is the shortlist entry (rating and rationale). */
export const CLIENT_FACING: Stage[] = ['PRESENTED', 'CLIENT_INTERVIEW', 'OFFER', 'PLACED'];

/** Stages offered in the row select: the current one first, then the legal moves (plus shortlist for longlist rows). */
export function stageOptions(ref: RefData | undefined, app: App): Stage[] {
  const to = legalMoves(ref, app.stage).map((m) => m.to);
  if (app.list === 'LONGLIST' && !to.includes('PRESENTED')) to.push('PRESENTED');
  return [app.stage, ...to];
}

export function useStageControl(initial: App[] | undefined, onChanged: () => void, reloadApps?: () => void) {
  const { t } = useTranslation();
  const { ref, label } = useLabel();
  const me = useCurrentUser();
  const moves = useBoardMoves(initial, onChanged, reloadApps);
  const [reasonFor, setReasonFor] = useState<{ app: App; to: Stage }>();
  const [shortlistFor, setShortlistFor] = useState<{ app: App; to: Stage }>();
  const [busy, setBusy] = useState<ReadonlySet<string>>(new Set());
  const inflight = useRef(new Set<string>());
  const track = async (id: string, run: () => Promise<void>) => {
    inflight.current.add(id);
    setBusy(new Set(inflight.current));
    try {
      await run();
    } finally {
      inflight.current.delete(id);
      setBusy(new Set(inflight.current));
    }
  };

  const change = (app: App, to: Stage) => {
    if (to === app.stage) return;
    if (inflight.current.has(app.id)) return;
    if (CLIENT_FACING.includes(to) && app.list === 'LONGLIST') setShortlistFor({ app, to });
    else if (legalMoves(ref, app.stage).find((m) => m.to === to)?.reasonRequired) setReasonFor({ app, to });
    else void track(app.id, () => moves.move(app, to));
  };

  const dialogs = (
    <>
      <ShortlistDialog
        app={shortlistFor?.app}
        onClose={() => setShortlistFor(undefined)}
        onConfirm={async (rating, rationale) => {
          const { app, to } = shortlistFor!;
          const r = await shortlistApp(app, rating, rationale, to, t('pipeline.ll.writeFailed'));
          if (!r.errors) {
            moves.show({ kind: 'info', text: t('pipeline.moved', { name: app.person.name, stage: label('pipelineStage', to) }), warnings: warningLines(r.saved?.warnings) });
            onChanged();
            reloadApps?.();
          }
          return r.errors;
        }}
      />
      <ReasonDialog
        opened={!!reasonFor}
        title={t('pipeline.cand.rejectTitle', { name: reasonFor?.app.person.name })}
        onCancel={() => setReasonFor(undefined)}
        onConfirm={(reason) => {
          if (reasonFor) void track(reasonFor.app.id, () => moves.move(reasonFor.app, reasonFor.to, reason));
          setReasonFor(undefined);
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
    </>
  );
  const toast = moves.toast ? <MoveToast toast={moves.toast} onClose={() => moves.setToast(undefined)} onRefresh={() => (reloadApps ?? onChanged)()} /> : null;
  return { ...moves, change, busy, dialogs, toast };
}
