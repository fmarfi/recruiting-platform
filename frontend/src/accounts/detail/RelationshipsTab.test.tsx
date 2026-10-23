import { describe, expect, it } from 'vitest';
import { coverage } from './RelationshipsTab';

const NOW = new Date('2026-10-23T12:00:00Z').getTime();
const ago = (d: number) => new Date(NOW - d * 86_400_000).toISOString();
const contact = (id: string, lastContactAt: string | null, active = true) =>
  ({
    id,
    active,
    primary: false,
    createdAt: ago(400),
    account: { id: 'a', name: 'A' },
    person: { id: `p-${id}`, name: id, type: 'CLIENT_CONTACT', owner: { id: 'u', name: 'U' }, consentStatus: 'GIVEN', retentionStatus: 'ACTIVE', lastContactAt },
  }) as never;

describe('coverage', () => {
  it('counts recent contacts, ignores ended ones and lists the stalest first', () => {
    const c = coverage([contact('fresh', ago(10)), contact('old', ago(200)), contact('never', null), contact('gone', ago(500), false)], NOW);
    expect(c.total).toBe(3);
    expect(c.covered).toBe(1);
    expect(c.gaps).toBe(2);
    expect(c.rows.map((r) => r.contact.id)).toEqual(['never', 'old', 'fresh']);
  });
});
