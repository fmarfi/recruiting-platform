import { describe, expect, it } from 'vitest';
import { toCsv, visibleCsv } from './csv';
import { DEFAULT_FILTERS, PRESETS, clientFilter, filtersFromParams, paramsFromFilters, sameFilters } from './networkFilters';
import { layoutMap } from '../person/RelationshipMap';

describe('toCsv', () => {
  it('quotes separators and quotes, joins with semicolons', () => {
    expect(toCsv(['a', 'b'], [['x;y', 'say "hi"'], [null, 'ok']])).toBe('a;b\r\n"x;y";"say ""hi"""\r\n;ok');
  });
  it('neutralises spreadsheet formulas', () => {
    expect(toCsv(['n'], [['=SUM(A1)'], ['+1'], ['@cmd'], ['-5']])).toBe("n\r\n'=SUM(A1)\r\n'+1\r\n'@cmd\r\n'-5");
  });
});

describe('saved views', () => {
  it('compares filters regardless of type order', () => {
    expect(sameFilters({ ...DEFAULT_FILTERS, types: ['EXECUTIVE', 'REFERRER'] }, { ...DEFAULT_FILTERS, types: ['REFERRER', 'EXECUTIVE'] })).toBe(true);
    expect(sameFilters(DEFAULT_FILTERS, PRESETS[0]!.filters)).toBe(false);
  });
});

const person = (id: string, name = id) =>
  ({ id, name, type: 'EXECUTIVE', owner: { id: 'u', name: 'U' }, consentStatus: 'GIVEN', retentionStatus: 'ACTIVE' }) as const;
const conn = (id: string) => ({ linkId: `l-${id}`, type: 'KNOWS', direction: 'BOTH', person: person(id) }) as const;

describe('layoutMap', () => {
  const center = { id: 'c', name: 'Center', type: 'CANDIDATE' } as const;
  it('places first-degree people around the centre and caps the ring', () => {
    const links = Array.from({ length: 20 }, (_, i) => conn(`p${i}`));
    const m = layoutMap(center, links, {});
    expect(m.nodes).toHaveLength(15);
    expect(m.hidden).toBe(6);
    expect(m.edges).toHaveLength(14);
  });
  it('adds second-degree people once and never loops back to the centre', () => {
    const links = [conn('a'), conn('b')];
    const m = layoutMap(center, links, { a: [conn('x'), conn('c'), conn('b')], b: [conn('x')] });
    const ids = m.nodes.map((n) => n.id).sort();
    expect(ids).toEqual(['a', 'b', 'c', 'x']);
    expect(m.edges.filter((e) => e.to === 'x')).toHaveLength(2);
    expect(m.edges.some((e) => e.to === 'c' && e.from !== 'c' && e.from === 'a')).toBe(false);
  });
});

describe('network URL params (command bar)', () => {
  const parse = (q: string) => filtersFromParams(new URLSearchParams(q));
  it('reads owner, stale, type, consent, city, company and q', () => {
    const { filters: f, page } = parse('owner=me&stale=90&type=EXECUTIVE,referrer&consent=GIVEN&city=Izmir&company=Y%20Holding&q=ayse&page=3&sort=lastContactAt,desc');
    expect(f).toMatchObject({ mine: true, staleDays: 90, types: ['EXECUTIVE', 'REFERRER'], consent: 'GIVEN', city: 'Izmir', company: 'Y Holding', q: 'ayse', sort: 'lastContactAt,desc' });
    expect(page).toBe(2);
  });
  it('drops unknown values and keeps the last repeated one', () => {
    const { filters: f } = parse('type=ALIEN,CANDIDATE&consent=BOGUS&stale=-4&sort=password,asc&owner=all&owner=me');
    expect(f).toMatchObject({ types: ['CANDIDATE'], consent: undefined, staleDays: 0, sort: '', mine: true });
  });
  it('round-trips and leaves defaults out of the URL', () => {
    expect(paramsFromFilters(DEFAULT_FILTERS, 0).toString()).toBe('');
    const f = { ...DEFAULT_FILTERS, mine: true, staleDays: 30, types: ['EXECUTIVE' as const], city: 'Ankara' };
    expect(parse(paramsFromFilters(f, 1).toString())).toEqual({ filters: f, page: 1 });
  });
  it('city/company narrow the loaded page, Turkish case-insensitive', () => {
    const rows = [{ city: 'İzmir', company: 'Y Holding' }, { city: 'Ankara', company: 'Z' }, { city: null, company: null }];
    expect(clientFilter(rows, { city: 'izmir', company: '' })).toHaveLength(1);
    expect(clientFilter(rows, { city: '', company: 'holding' })).toHaveLength(1);
    expect(clientFilter(rows, { city: '', company: '' })).toHaveLength(3);
  });
});

describe('visibleCsv', () => {
  it('writes only the given columns, header from column headers', () => {
    const cols = [{ id: 'a', header: 'A', accessor: (r: { a: string; b: string }) => r.a }, { id: 'b', header: 'B', accessor: (r: { a: string; b: string }) => r.b }];
    expect(visibleCsv(cols.slice(1), [{ a: '1', b: '2' }])).toEqual({ header: ['B'], rows: [['2']] });
  });
});