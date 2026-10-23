import { describe, expect, it } from 'vitest';
import { applyView, filtersFromParams, toView } from './accountFilters';

const p = (s: string) => new URLSearchParams(s);

describe('filtersFromParams', () => {
  it('defaults to my accounts, sorted by last activity', () => {
    expect(filtersFromParams(p(''), 'me-1')).toEqual({ q: '', status: '', ownerId: 'me-1', page: 0, sort: 'lastActivityAt,desc' });
  });
  it('reads owner, status and q from the command-bar query', () => {
    expect(filtersFromParams(p('owner=me&status=active&q=holding'), 'me-1')).toMatchObject({ ownerId: 'me-1', status: 'ACTIVE', q: 'holding' });
    expect(filtersFromParams(p('owner=all'), 'me-1').ownerId).toBe('');
  });
  it('ignores unknown statuses', () => {
    expect(filtersFromParams(p('status=OPEN'), 'm').status).toBe('');
  });
});

describe('saved views', () => {
  it('stores the owner as a scope and restores it for the current user', () => {
    const f = { q: 'x', status: 'LEAD', ownerId: 'u1', page: 3, sort: 'name,asc' };
    expect(toView(f)).toEqual({ q: 'x', status: 'LEAD', owner: 'me' });
    expect(toView({ ...f, ownerId: '' }).owner).toBe('all');
    const applied = applyView(f, { filters: { q: '', status: 'ACTIVE', owner: 'me' }, sort: { field: 'status', dir: 'desc' } }, 'u2');
    expect(applied).toEqual({ q: '', status: 'ACTIVE', ownerId: 'u2', page: 0, sort: 'status,desc' });
    expect(applyView(f, { filters: { q: '', status: '', owner: 'all' } }, 'u2')).toMatchObject({ ownerId: '', sort: 'name,asc' });
  });
});
