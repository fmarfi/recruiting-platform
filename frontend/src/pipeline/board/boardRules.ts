import type { RefData, Schemas } from '../shared';

export type Stage = Schemas['PipelineStage'];
export type Application = Schemas['Application'];
export type Move = { to: Stage; reasonRequired: boolean };

/** Moves offered for a stage: exactly the `next` list of /reference-data (docs/status-lifecycles.md section 4). */
export function legalMoves(ref: RefData | undefined, from: Stage): Move[] {
  const def = ref?.lifecycles.pipelineStage.statuses.find((s) => s.value === from);
  return (def?.next ?? []).map((n) => ({ to: n.to as Stage, reasonRequired: n.reasonRequired }));
}

export function canMove(ref: RefData | undefined, from: Stage, to: Stage): Move | undefined {
  return legalMoves(ref, from).find((m) => m.to === to);
}

/** Replace one application in a list (optimistic update and rollback). */
export function withStage(list: Application[], id: string, stage: Stage, reason?: string | null): Application[] {
  return list.map((a) =>
    a.id === id
      ? { ...a, stage, rejectionReason: stage === 'REJECTED' ? (reason ?? null) : null, stageChangedAt: new Date().toISOString() }
      : a,
  );
}
