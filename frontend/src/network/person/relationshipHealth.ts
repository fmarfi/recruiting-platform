// Relationship health from data the person page already loads: how warm is the contact and what should happen next.
import type { Schemas } from '../../pipeline/shared';

export type Health = 'GOOD' | 'WARN' | 'BAD';
export type NextStep = 'doNotContact' | 'overdueAction' | 'reconnect' | 'neverContacted' | 'planNext' | 'onTrack';

const DAY = 86_400_000;

export const daysSince = (iso?: string | null, now = Date.now()) => (iso ? Math.max(0, Math.floor((now - new Date(iso).getTime()) / DAY)) : undefined);

export function healthOf(days: number | undefined): Health {
  if (days === undefined) return 'BAD';
  return days <= 30 ? 'GOOD' : days <= 90 ? 'WARN' : 'BAD';
}

export function relationshipSummary(
  person: Pick<Schemas['Person'], 'lastContactAt' | 'consentStatus'>,
  activities: Pick<Schemas['Activity'], 'occurredAt'>[],
  actions: Pick<Schemas['NextAction'], 'overdue'>[],
  now = Date.now(),
) {
  const days = daysSince(person.lastContactAt, now);
  const touches90 = activities.filter((a) => now - new Date(a.occurredAt).getTime() <= 90 * DAY).length;
  const overdue = actions.filter((a) => a.overdue).length;
  const blocked = person.consentStatus === 'REFUSED' || person.consentStatus === 'WITHDRAWN';
  const next: NextStep = blocked
    ? 'doNotContact'
    : overdue > 0
      ? 'overdueAction'
      : days === undefined
        ? 'neverContacted'
        : days > 90
          ? 'reconnect'
          : actions.length === 0
            ? 'planNext'
            : 'onTrack';
  return { days, health: healthOf(days), touches90, overdue, next };
}
