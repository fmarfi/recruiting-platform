import { describe, expect, it } from 'vitest';
import { toCsv } from './csv';
import { DEFAULT_FILTERS, PRESETS, sameFilters } from './savedViews';
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
