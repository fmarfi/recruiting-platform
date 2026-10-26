import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createApiClient } from '../../api';
import { apiViewStore, columnsFromApi, columnsToApi, defaultToFilters, sortFromApi, sortToApi } from './apiViewStore';
import { mapViewError, ViewStoreError } from './viewErrors';
import { createViewStore, type ViewNotice } from './viewStoreFactory';

const owner = { id: 'u1', name: 'Deniz' };
const apiView = (over: Record<string, unknown> = {}) => ({
  id: 'v1', entity: 'ACCOUNTS', name: 'Mine', visibility: 'PRIVATE', owner, editable: true, createdAt: 'x', updatedAt: 'x', version: 3,
  definition: { filters: { status: 'ACTIVE', ownerId: 'me' }, columns: ['name', 'status'], sort: ['name,desc'] },
  ...over,
});
const json = (status: number, body?: unknown) => new Response(body === undefined ? null : JSON.stringify(body), { status, headers: { 'content-type': 'application/json' } });

let fetchMock: ReturnType<typeof vi.fn>;
beforeEach(() => {
  localStorage.clear();
  fetchMock = vi.fn();
  vi.stubGlobal('fetch', fetchMock);
});
afterEach(() => vi.unstubAllGlobals());

const bodyOf = async (call: number) => JSON.parse(await (fetchMock.mock.calls[call]![0] as Request).clone().text());
const store = (o: { canShare?: boolean } = {}) => apiViewStore<{ status: string }>({ entity: 'ACCOUNTS', client: createApiClient('http://t/api'), userId: 'u1', ...o });

describe('pure mapping', () => {
  it('maps problem responses to codes', () => {
    expect(mapViewError(409, { code: 'DUPLICATE' }).code).toBe('DUPLICATE');
    expect(mapViewError(409, { code: 'VERSION_CONFLICT' }).code).toBe('CONFLICT');
    expect(mapViewError(400, { errors: [{ field: 'entity', code: 'LIMIT' }] }).code).toBe('LIMIT');
    expect(mapViewError(400, { errors: [{ field: 'definition.filters.x', code: 'UNKNOWN' }] }).code).toBe('VALIDATION');
    expect(mapViewError(403, {}).code).toBe('FORBIDDEN');
    expect(mapViewError(404, undefined).code).toBe('NOT_FOUND');
    expect(mapViewError(503, undefined).code).toBe('UNAVAILABLE');
  });
  it('converts columns, sort and filters', () => {
    expect(columnsToApi({ order: ['b', 'a', 'c'], visible: ['a', 'b'] })).toEqual(['b', 'a']);
    expect(columnsFromApi(['a'])).toEqual({ order: ['a'], visible: ['a'] });
    expect(sortToApi({ field: 'name', dir: 'desc' })).toEqual(['name,desc']);
    expect(sortFromApi(['name,desc'])).toEqual({ field: 'name', dir: 'desc' });
    expect(sortFromApi([])).toBeNull();
    expect(defaultToFilters({ q: '', status: 'A', x: [], y: null })).toEqual({ status: 'A' });
  });
});

describe('apiViewStore', () => {
  it('lists views of the entity and marks own ones', async () => {
    fetchMock.mockResolvedValue(json(200, [apiView(), apiView({ id: 'v2', visibility: 'SHARED', owner: { id: 'u2', name: 'Ece' }, editable: false })]));
    const all = await store().list();
    expect((fetchMock.mock.calls[0]![0] as Request).url).toContain('/views?entity=ACCOUNTS');
    expect(all[0]).toMatchObject({ id: 'v1', mine: true, editable: true, sort: { field: 'name', dir: 'desc' }, columns: { visible: ['name', 'status'] } });
    expect(all[1]).toMatchObject({ visibility: 'SHARED', mine: false, editable: false });
  });
  it('creates private by default and updates with the listed version', async () => {
    const s = store();
    fetchMock.mockResolvedValueOnce(json(200, [apiView()])).mockResolvedValueOnce(json(201, apiView({ id: 'v9' }))).mockResolvedValueOnce(json(200, apiView()));
    await s.list();
    await s.save({ name: 'New', filters: { status: 'A' } });
    expect(await bodyOf(1)).toMatchObject({ entity: 'ACCOUNTS', name: 'New', visibility: 'PRIVATE', definition: { filters: { status: 'A' }, columns: [], sort: [] } });
    await s.save({ id: 'v1', name: 'Mine', filters: { status: 'B' } });
    expect((fetchMock.mock.calls[2]![0] as Request).method).toBe('PATCH');
    expect(await bodyOf(2)).toMatchObject({ version: 3, name: 'Mine' });
  });
  it('rename sends only name and version', async () => {
    const s = store();
    fetchMock.mockResolvedValueOnce(json(200, [apiView()])).mockResolvedValueOnce(json(200, apiView({ name: 'B' })));
    await s.list();
    await s.rename!('v1', 'B');
    expect(await bodyOf(1)).toEqual({ name: 'B', version: 3 });
  });
  it('offers SHARED only to managers', async () => {
    expect(store().canShare).toBe(false);
    expect(store({ canShare: true }).canShare).toBe(true);
    await expect(store().save({ name: 'x', filters: { status: '' }, visibility: 'SHARED' })).rejects.toMatchObject({ code: 'FORBIDDEN' });
    expect(fetchMock).not.toHaveBeenCalled();
  });
  it('maps 409 DUPLICATE, 400 LIMIT, 409 VERSION_CONFLICT and network errors', async () => {
    const s = store();
    fetchMock.mockResolvedValueOnce(json(409, { type: 't', title: 't', status: 409, code: 'DUPLICATE' }));
    await expect(s.save({ name: 'x', filters: { status: '' } })).rejects.toMatchObject({ code: 'DUPLICATE' });
    fetchMock.mockResolvedValueOnce(json(400, { status: 400, code: 'VALIDATION_FAILED', errors: [{ field: 'entity', code: 'LIMIT' }] }));
    await expect(s.save({ name: 'x', filters: { status: '' } })).rejects.toMatchObject({ code: 'LIMIT' });
    fetchMock.mockResolvedValueOnce(json(409, { status: 409, code: 'VERSION_CONFLICT' }));
    await expect(s.save({ id: 'v1', name: 'x', filters: { status: '' } })).rejects.toMatchObject({ code: 'CONFLICT' });
    fetchMock.mockRejectedValueOnce(new TypeError('network'));
    await expect(s.list()).rejects.toBeInstanceOf(ViewStoreError);
  });
  it('deletes', async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));
    await store().delete('v1');
    expect((fetchMock.mock.calls[0]![0] as Request).method).toBe('DELETE');
  });
});

describe('createViewStore (fallback and migration)', () => {
  const make = (notices: ViewNotice[]) => createViewStore<{ status: string }>({ gridId: 'g', entity: 'ACCOUNTS', client: createApiClient('http://t/api'), userId: 'u1', canShare: true, onNotice: (n) => notices.push(n) });

  it('falls back to local views when the API is unavailable', async () => {
    const notices: ViewNotice[] = [];
    fetchMock.mockResolvedValue(json(503, { status: 503 }));
    const s = make(notices);
    expect(await s.list()).toEqual([]);
    await s.save({ name: 'Loc', filters: { status: 'A' }, visibility: 'SHARED' });
    expect((await s.list()).map((v) => v.name)).toEqual(['Loc']);
    expect(notices).toEqual([{ type: 'fallback' }]);
    expect(s.canShare).toBe(false);
  });

  it('migrates local views once and says so', async () => {
    localStorage.setItem('grid.g.views', JSON.stringify([{ id: 'a', name: 'One', filters: { status: 'A' } }, { id: 'b', name: 'Two', filters: { status: 'B' } }]));
    const notices: ViewNotice[] = [];
    fetchMock
      .mockResolvedValueOnce(json(200, [])) // first list
      .mockResolvedValueOnce(json(201, apiView({ name: 'One' })))
      .mockResolvedValueOnce(json(409, { status: 409, code: 'DUPLICATE' })) // already there
      .mockResolvedValueOnce(json(200, [apiView({ name: 'One' })])); // re-list
    const s = make(notices);
    expect(await s.list()).toHaveLength(1);
    expect(notices).toEqual([{ type: 'migrated', moved: 2, left: 0 }]);
    expect(localStorage.getItem('grid.g.views')).toBeNull();
    expect(localStorage.getItem('grid.g.views.migrated')).toBe('1');
    fetchMock.mockResolvedValueOnce(json(200, []));
    await make([]).list();
    expect(fetchMock).toHaveBeenCalledTimes(5); // no second migration
  });

  it('keeps unmigrated views and retries later when the limit is hit', async () => {
    localStorage.setItem('grid.g.views', JSON.stringify([{ id: 'a', name: 'One', filters: { status: 'A' } }]));
    const notices: ViewNotice[] = [];
    fetchMock.mockResolvedValueOnce(json(200, [])).mockResolvedValueOnce(json(400, { status: 400, errors: [{ code: 'LIMIT' }] }));
    await make(notices).list();
    expect(notices).toEqual([{ type: 'migrated', moved: 0, left: 1 }]);
    expect(localStorage.getItem('grid.g.views')).not.toBeNull();
    expect(localStorage.getItem('grid.g.views.migrated')).toBeNull();
  });
});
