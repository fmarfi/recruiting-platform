import { describe, expect, it } from 'vitest';
import { healthOf, relationshipSummary } from './relationshipHealth';

const NOW = new Date('2026-10-23T12:00:00Z').getTime();
const ago = (d: number) => new Date(NOW - d * 86_400_000).toISOString();

describe('relationship health', () => {
  it('grades by days since last contact', () => {
    expect(healthOf(undefined)).toBe('BAD');
    expect(healthOf(0)).toBe('GOOD');
    expect(healthOf(30)).toBe('GOOD');
    expect(healthOf(31)).toBe('WARN');
    expect(healthOf(90)).toBe('WARN');
    expect(healthOf(91)).toBe('BAD');
  });

  it('counts touchpoints in the last 90 days only', () => {
    const s = relationshipSummary({ lastContactAt: ago(5), consentStatus: 'GIVEN' }, [{ occurredAt: ago(5) }, { occurredAt: ago(89) }, { occurredAt: ago(120) }], [], NOW);
    expect(s.touches90).toBe(2);
    expect(s.days).toBe(5);
  });

  it('picks the next step in priority order', () => {
    const p = (d?: number, consentStatus: 'GIVEN' | 'WITHDRAWN' = 'GIVEN') => ({ lastContactAt: d === undefined ? null : ago(d), consentStatus });
    expect(relationshipSummary(p(5, 'WITHDRAWN'), [], [{ overdue: true }], NOW).next).toBe('doNotContact');
    expect(relationshipSummary(p(5), [], [{ overdue: true }], NOW).next).toBe('overdueAction');
    expect(relationshipSummary(p(undefined), [], [], NOW).next).toBe('neverContacted');
    expect(relationshipSummary(p(120), [], [{ overdue: false }], NOW).next).toBe('reconnect');
    expect(relationshipSummary(p(10), [], [], NOW).next).toBe('planNext');
    expect(relationshipSummary(p(10), [], [{ overdue: false }], NOW).next).toBe('onTrack');
  });
});
