import { beforeEach, describe, expect, it, vi } from 'vitest';
import { defaultColumnState, formatSort, loadColumnState, moveColumn, nextSort, normalizeColumnState, pageSelection, parseSort, saveColumnState, toggleColumn, toggleId, togglePage, visibleColumns } from './gridState';
import { toCsv } from './csv';
import { localViewStore } from './viewStore';
import type { ColumnDef } from './types';

const defs: ColumnDef<unknown>[] = [
  { id: 'name', header: 'Name', locked: true },
  { id: 'city', header: 'City' },
  { id: 'owner', header: 'Owner', defaultVisible: false },
];

beforeEach(() => localStorage.clear());

describe('column persistence', () => {
  it('defaults to definition order with default-visible columns', () => {
    expect(defaultColumnState(defs)).toEqual({ order: ['name', 'city', 'owner'], visible: ['name', 'city'] });
  });

  it('round-trips per grid id and ignores other grids', () => {
    saveColumnState('a', { order: ['city', 'name', 'owner'], visible: ['name', 'owner'] });
    expect(loadColumnState('a', defs)).toEqual({ order: ['city', 'name', 'owner'], visible: ['name', 'owner'] });
    expect(loadColumnState('b', defs)).toEqual(defaultColumnState(defs));
  });

  it('drops unknown ids, appends new columns and keeps locked ones visible', () => {
    const s = normalizeColumnState(defs, { order: ['gone', 'city'], visible: ['gone'] });
    expect(s.order).toEqual(['city', 'name', 'owner']);
    expect(s.visible.sort()).toEqual(['name']);
  });

  it('survives corrupt storage and a throwing localStorage', () => {
    localStorage.setItem('grid.x.columns', '{nope');
    expect(loadColumnState('x', defs)).toEqual(defaultColumnState(defs));
    const orig = Storage.prototype.setItem;
    Storage.prototype.setItem = () => {
      throw new Error('blocked');
    };
    expect(() => saveColumnState('x', defaultColumnState(defs))).not.toThrow();
    Storage.prototype.setItem = orig;
  });

  it('toggles, moves, and never hides locked or the last column', () => {
    const s = defaultColumnState(defs);
    expect(toggleColumn(defs, s, 'name')).toBe(s);
    expect(toggleColumn(defs, s, 'owner').visible).toContain('owner');
    expect(toggleColumn(defs, s, 'city').visible).toEqual(['name']);
    expect(moveColumn(s, 'city', -1).order).toEqual(['city', 'name', 'owner']);
    expect(moveColumn(s, 'name', -1)).toBe(s);
    expect(visibleColumns(defs, moveColumn(s, 'city', -1)).map((d) => d.id)).toEqual(['city', 'name']);
  });
});

describe('sort toggling', () => {
  it('starts at the first direction and flips on the same column', () => {
    const a = nextSort(null, 'name', 'asc');
    expect(a).toEqual({ field: 'name', dir: 'asc' });
    expect(nextSort(a, 'name')).toEqual({ field: 'name', dir: 'desc' });
    expect(nextSort(nextSort(a, 'name'), 'name')).toEqual({ field: 'name', dir: 'asc' });
    expect(nextSort(a, 'lastActivityAt', 'desc')).toEqual({ field: 'lastActivityAt', dir: 'desc' });
  });
  it('parses and formats the server form', () => {
    expect(parseSort('lastActivityAt,desc')).toEqual({ field: 'lastActivityAt', dir: 'desc' });
    expect(parseSort('name')).toEqual({ field: 'name', dir: 'asc' });
    expect(parseSort('')).toBeNull();
    expect(formatSort({ field: 'name', dir: 'asc' })).toBe('name,asc');
  });
});

describe('selection', () => {
  it('toggles single ids', () => {
    const s = toggleId(new Set(['a']), 'b');
    expect([...s]).toEqual(['a', 'b']);
    expect([...toggleId(s, 'a')]).toEqual(['b']);
  });
  it('select-all acts on the page and keeps other pages', () => {
    const page = ['1', '2', '3'];
    const other = new Set(['9']);
    expect(pageSelection(other, page)).toBe('none');
    const all = togglePage(other, page);
    expect([...all].sort()).toEqual(['1', '2', '3', '9']);
    expect(pageSelection(all, page)).toBe('all');
    expect(pageSelection(new Set(['1']), page)).toBe('some');
    expect([...togglePage(all, page)]).toEqual(['9']);
    expect([...togglePage(new Set(['1']), page)].sort()).toEqual(['1', '2', '3']);
  });
});

describe('csv', () => {
  it('quotes separators and neutralises formulas', () => {
    expect(toCsv(['a', 'b'], [['=1+1', 'x;y'], [null, 'q"r']])).toBe('a;b\r\n\'=1+1;"x;y"\r\n;"q""r"');
  });
});

describe('local view store when storage fails', () => {
  it('keeps the in-memory list as the truth and reports the error', async () => {
    const spy = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('blocked');
    });
    const onErr = vi.fn();
    const store = localViewStore<{ q: string }>('f', onErr);
    await store.save({ name: 'A', filters: { q: '' } });
    await store.save({ name: 'B', filters: { q: '' } });
    expect((await store.list()).map((x) => x.name)).toEqual(['A', 'B']);
    expect(onErr).toHaveBeenCalledTimes(2);
    spy.mockRestore();
  });
});

describe('local view store', () => {
  it('saves, replaces by id, lists and deletes', async () => {
    const store = localViewStore<{ q: string }>('t');
    const v = await store.save({ name: 'Mine', filters: { q: 'a' }, sort: { field: 'name', dir: 'asc' } });
    await store.save({ id: v.id, name: 'Mine', filters: { q: 'b' } });
    await store.save({ name: 'Other', filters: { q: '' } });
    const all = await store.list();
    expect(all.map((x) => x.name)).toEqual(['Mine', 'Other']);
    expect(all[0]!.filters.q).toBe('b');
    expect((await localViewStore('u').list())).toEqual([]);
    await store.delete(v.id);
    expect((await store.list()).map((x) => x.name)).toEqual(['Other']);
  });
});
