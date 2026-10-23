import { describe, expect, it } from 'vitest';
import { matchesText, moveActive, parseQuery, removeOperator, routeFor } from './operators';

describe('parseQuery', () => {
  it('parses the documented example', () => {
    const p = parseQuery('owner:me stale:90 city:izmir type:exec consent:given company:"Y Holding" status:active');
    expect(p.operators.map((o) => [o.key, o.value])).toEqual([
      ['owner', 'me'], ['stale', '90'], ['city', 'izmir'], ['type', 'EXECUTIVE'], ['consent', 'GIVEN'], ['company', 'Y Holding'], ['status', 'ACTIVE'],
    ]);
    expect(p.text).toBe('');
  });
  it('keeps free text, quoted phrases and unknown operators', () => {
    const p = parseQuery('ahmet foo:bar "ali veli" owner:me');
    expect(p.operators).toHaveLength(1);
    expect(p.text).toBe('ahmet foo:bar ali veli');
  });
  it('keeps invalid or empty values as free text', () => {
    expect(parseQuery('stale:abc owner:other type: consent:maybe stale:0').operators).toEqual([]);
    expect(parseQuery('stale:abc').text).toBe('stale:abc');
  });
  it('is case and Turkish-I insensitive', () => {
    expect(parseQuery('TYPE:Exec CONSENT:GIVEN').operators.map((o) => o.value)).toEqual(['EXECUTIVE', 'GIVEN']);
    expect(parseQuery('status:on-hold').operators[0]!.value).toBe('ON_HOLD');
  });
  it('handles an unclosed quote and records token positions', () => {
    const p = parseQuery('a company:"Y Hol');
    expect(p.operators[0]).toMatchObject({ key: 'company', value: 'Y Hol', start: 2 });
    const q = parseQuery('x owner:me y');
    expect(removeOperator('x owner:me y', q.operators[0]!)).toBe('x y');
  });
});

describe('routeFor', () => {
  it('maps people operators to /network with URL params', () => {
    const r = routeFor(parseQuery('owner:me stale:90 type:exec'))!;
    expect(r.to).toBe('/network?owner=me&stale=90&type=EXECUTIVE');
    expect(r.path).toBe('/network');
  });
  it('joins repeated types, encodes values and appends free text as q', () => {
    expect(routeFor(parseQuery('type:exec type:cand company:"Y Holding" ali'))!.to).toBe('/network?type=EXECUTIVE%2CCANDIDATE&company=Y+Holding&q=ali');
  });
  it('sends account and job statuses to their lists and drops what they cannot filter', () => {
    expect(routeFor(parseQuery('status:active owner:me'))!.to).toBe('/accounts?owner=me&status=ACTIVE');
    expect(routeFor(parseQuery('status:open'))!.to).toBe('/job-orders?status=OPEN');
    expect(routeFor(parseQuery('owner:me'))!.to).toBe('/accounts?owner=me');
    const r = routeFor(parseQuery('type:exec status:active'))!;
    expect(r.path).toBe('/network');
    expect(r.dropped.map((o) => o.key)).toEqual(['status']);
  });
  it('returns null without list operators', () => {
    expect(routeFor(parseQuery('ahmet'))).toBeNull();
  });
});

describe('keyboard helpers', () => {
  it('wraps arrow navigation', () => {
    expect(moveActive(-1, 1, 3)).toBe(0);
    expect(moveActive(-1, -1, 3)).toBe(2);
    expect(moveActive(2, 1, 3)).toBe(0);
    expect(moveActive(0, -1, 3)).toBe(2);
    expect(moveActive(0, 1, 0)).toBe(-1);
  });
  it('matches accent-insensitively', () => {
    expect(matchesText('Git: Gün Başlangıcı', 'gun basl')).toBe(true);
    expect(matchesText('New client', 'xyz')).toBe(false);
  });
});
