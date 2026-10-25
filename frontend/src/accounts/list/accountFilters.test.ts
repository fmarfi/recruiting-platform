import { describe, expect, it } from 'vitest';
import { applyView, filtersFromParams, paramsFromFilters, toView } from './accountFilters';

const p = (s: string) => new URLSearchParams(s);

describe('filtersFromParams', () => {
  it('defaults to my accounts, sorted by last activity', () => {
    expect(filtersFromParams(p(''), 'me-1')).toEqual({ q: '', status: '', ownerId: 'me-1', page: 0, sort: 'lastActivityAt,desc' });
  });
  it('reads owner, status and q from the command-bar query', () => {
    expect(filtersFromParams(p('owner=me&status=active&q=holding'), 'me-1')).toMatchObject({ ownerId: 'me-1', status: 'ACTIVE', q: 'holding' });
    expect(filtersFromParams(p('owner=all'), 'me-1').ownerId).toBe('');
  });
  it('keeps only the last status of a repeated or comma separated list', () => {
    expect(filtersFromParams(p('status=ACTIVE&status=LEAD'), 'm').status).toBe('LEAD');
    expect(filtersFromParams(p('status=ACTIVE,LEAD'), 'm').status).toBe('LEAD');
  });
  it('reads page and sort and round-trips through the URL, leaving defaults out', () => {
    const f = filtersFromParams(p('page=3&sort=name,asc&owner=all'), 'm');
    expect(f).toMatchObject({ page: 2, sort: 'name,asc', ownerId: '' });
    expect(paramsFromFilters(f).toString()).toBe('owner=all&page=3&sort=name%2Casc');
    expect(paramsFromFilters(filtersFromParams(p(''), 'm')).toString()).toBe('');
    expect(filtersFromParams(p('sort=evil,asc&page=-2'), 'm')).toMatchObject({ sort: 'lastActivityAt,desc', page: 0 });
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
