import { beforeEach, describe, expect, it } from 'vitest';
import { clearRecentRefs, loadRecentRefs, pushRecentRef, recentPath } from './recentItems';

const DAY = 24 * 3600 * 1000;
beforeEach(() => localStorage.clear());

describe('recent items (KVKK)', () => {
  it('stores kind, id and time only, newest first, deduplicated and capped', () => {
    for (let i = 0; i < 10; i++) pushRecentRef('u', { kind: 'person', id: `p${i}` }, 1000 + i);
    pushRecentRef('u', { kind: 'person', id: 'p5' }, 2000);
    const refs = loadRecentRefs('u', 2000);
    expect(refs).toHaveLength(8);
    expect(refs[0]).toEqual({ kind: 'person', id: 'p5', at: 2000 });
    expect(refs.filter((r) => r.id === 'p5')).toHaveLength(1);
    expect(Object.keys(refs[0]!).sort()).toEqual(['at', 'id', 'kind']);
  });
  it('keeps a list per user and clears it', () => {
    pushRecentRef('a', { kind: 'job', id: '1' });
    expect(loadRecentRefs('b')).toEqual([]);
    clearRecentRefs('a');
    expect(loadRecentRefs('a')).toEqual([]);
  });
  it('expires entries after 30 days and purges the old title-bearing key', () => {
    localStorage.setItem('command.recent', JSON.stringify([{ to: '/people/1', title: 'Real Name' }]));
    pushRecentRef('u', { kind: 'account', id: 'old' }, 0);
    pushRecentRef('u', { kind: 'account', id: 'new' }, 31 * DAY);
    expect(loadRecentRefs('u', 31 * DAY).map((r) => r.id)).toEqual(['new']);
    expect(localStorage.getItem('command.recent')).toBeNull();
  });
  it('maps refs to routes', () => {
    expect(recentPath({ kind: 'account', id: 'x' })).toBe('/accounts/x');
    expect(recentPath({ kind: 'person', id: 'x' })).toBe('/people/x');
    expect(recentPath({ kind: 'job', id: 'x' })).toBe('/job-orders/x');
  });
});
