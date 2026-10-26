import { describe, expect, it } from 'vitest';
import { DEFAULT_FILTERS, filtersFromParams, paramsFromFilters, sameFilters } from './networkFilters';
import { splitCvMatch } from './PoolFilters';

describe('pool filters <-> URL', () => {
  it('writes tag, channel, cv and shareable and reads them back', () => {
    const f = { ...DEFAULT_FILTERS, tags: ['cfo', 'istanbul'], channels: ['REFERRAL' as const, 'WEBSITE' as const], cv: '"financial controller" -intern', shareable: true };
    const p = paramsFromFilters(f, 0);
    expect(p.get('tag')).toBe('cfo,istanbul');
    expect(p.get('channel')).toBe('REFERRAL,WEBSITE');
    expect(p.get('shareable')).toBe('true');
    expect(filtersFromParams(p).filters).toEqual(f);
  });
  it('lower-cases tags, drops unknown channels and over-long tags, ignores shareable=false', () => {
    const p = new URLSearchParams('tag=CFO,cfo, &tag=' + 'x'.repeat(41) + '&channel=referral,NOPE&shareable=false');
    const { filters } = filtersFromParams(p);
    expect(filters.tags).toEqual(['cfo']);
    expect(filters.channels).toEqual(['REFERRAL']);
    expect(filters.shareable).toBe(false);
  });
  it('defaults add nothing to the URL and compare tag order-insensitively', () => {
    expect(paramsFromFilters(DEFAULT_FILTERS, 0).toString()).toBe('');
    expect(sameFilters({ ...DEFAULT_FILTERS, tags: ['a', 'b'] }, { ...DEFAULT_FILTERS, tags: ['b', 'a'] })).toBe(true);
    expect(sameFilters(DEFAULT_FILTERS, { ...DEFAULT_FILTERS, shareable: true })).toBe(false);
  });
});

describe('splitCvMatch', () => {
  it('marks the hit without producing HTML', () => {
    expect(splitCvMatch('led «treasury» team')).toEqual([
      { text: 'led ', hit: false },
      { text: 'treasury', hit: true },
      { text: ' team', hit: false },
    ]);
    expect(splitCvMatch('<b>x</b> «a')).toEqual([{ text: '<b>x</b> a', hit: false }]);
  });
});
