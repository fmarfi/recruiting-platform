import type { StatusDef } from '../../pipeline/shared';

export type StepState = 'done' | 'current' | 'upcoming';
export type PathStepModel = { value: string; label: string; tone: string; state: StepState };

/**
 * Builds the chevron list from a lifecycle's statuses (display order from /reference-data).
 * Steps before the current one are done, the rest upcoming.
 */
export function stepsFor(statuses: StatusDef[], value: string, labelOf: (s: StatusDef) => string): PathStepModel[] {
  const currentIdx = statuses.findIndex((s) => s.value === value);
  return statuses.map((s, i) => ({
    value: s.value,
    label: labelOf(s),
    tone: s.tone,
    state: i === currentIdx ? 'current' : i < currentIdx ? 'done' : 'upcoming',
  }));
}
