// 409 handling for stage moves (ADR 0011). CONSENT_REQUIRED can only be fixed by getting consent; OFF_LIMITS can be
// overridden by MANAGER/ADMIN with a reason (3 to 500 chars after trimming), which is sent as offLimitsOverrideReason.
import type { Schemas } from '../shared';

type Problem = Pick<Schemas['ProblemDetail'], 'code' | 'blockingItems'>;
export type GuardItem = { id: string; title: string; reasonCode?: string };
export type Guard = { kind: 'CONSENT_REQUIRED' | 'OFF_LIMITS'; items: GuardItem[] };

export const REASON_MIN = 3;
export const REASON_MAX = 500;

/** Returns the guard to show, or undefined for any other failure (those stay a plain error toast). */
export function classifyStageError(error: unknown): Guard | undefined {
  const p = error as Partial<Problem> | undefined;
  if (p?.code !== 'CONSENT_REQUIRED' && p?.code !== 'OFF_LIMITS') return undefined;
  const items = (p.blockingItems ?? []).map((b) => ({ id: b.id, title: b.title, reasonCode: b.reasonCode }));
  return { kind: p.code, items };
}

export const validOverrideReason = (s: string): boolean => {
  const n = s.trim().length;
  return n >= REASON_MIN && n <= REASON_MAX;
};

/** Only MANAGER and ADMIN may override; the server answers 403 for anyone else. */
export const canOverrideOffLimits = (role?: string | null): boolean => role === 'MANAGER' || role === 'ADMIN';

const NO_RESEND = ['ANONYMISED', 'CONSENT_REFUSED', 'CONSENT_WITHDRAWN'];
/** No resend button for anonymised, refused or withdrawn people (do not contact); those go through the privacy tab on the person page. */
export const consentResendPossible = (items: GuardItem[]): boolean => !items.some((i) => i.reasonCode && NO_RESEND.includes(i.reasonCode));

/** Message for add-to-job failures: the OFF_LIMITS rules, or undefined for other errors. */
export const offLimitsRules = (error: unknown): string | undefined => {
  const g = classifyStageError(error);
  return g?.kind === 'OFF_LIMITS' ? g.items.map((i) => i.title).join(', ') : undefined;
};

/** The request body of a retry after the override reason was typed: the trimmed reason, only for managers. */
export function overrideBody(reason: string): { offLimitsOverrideReason: string } {
  return { offLimitsOverrideReason: reason.trim() };
}

/** Non-blocking warnings of a successful move, as stable codes with a count for the toast. */
export function warningLines(warnings: Schemas['PipelineWarning'][] | undefined): { code: Schemas['PipelineWarningCode']; count: number }[] {
  return (warnings ?? []).map((w) => ({ code: w.code, count: w.code === 'PREVIOUSLY_PRESENTED_TO_ACCOUNT' ? (w.exposures?.length ?? 1) : (w.otherProcesses?.length ?? 1) }));
}
