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
  it('does not let a stray quote swallow the rest of the input', () => {
    const p = parseQuery('ali " owner:me stale:90');
    expect(p.operators.map((o) => o.key)).toEqual(['owner', 'stale']);
    expect(p.text).toBe('ali');
    expect(parseQuery('company:"Y owner:me').operators.map((o) => o.key)).toEqual(['company', 'owner']);
  });
  it('ignores inherited object keys as operators', () => {
    const p = parseQuery('constructor:x toString:y __proto__:z');
    expect(p.operators).toEqual([]);
    expect(p.text).toBe('constructor:x toString:y __proto__:z');
  });
  it('records token positions', () => {
    const p = parseQuery('a company:"Y Hol"');
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
  it('keeps only the last status when it is repeated', () => {
    expect(routeFor(parseQuery('status:active status:lead'))!.to).toBe('/accounts?status=LEAD');
    expect(routeFor(parseQuery('status:open status:filled'))!.to).toBe('/job-orders?status=FILLED');
  });
  it('flags city and company as not applied yet on /network', () => {
    const r = routeFor(parseQuery('city:izmir type:exec company:"Y Holding"'))!;
    expect(r.pending.map((o) => o.key)).toEqual(['city', 'company']);
    expect(routeFor(parseQuery('type:exec'))!.pending).toEqual([]);
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

describe('Wave 2b operators: tag, source, cv', () => {
  it('parses and normalizes them', () => {
    const p = parseQuery('tag:Java source:referral cv:"project manager" cv:sap');
    expect(p.operators.map((o) => [o.key, o.value])).toEqual([['tag', 'java'], ['source', 'REFERRAL'], ['cv', '"project manager"'], ['cv', 'sap']]);
  });
  it('maps source aliases and constants, rejects unknown ones', () => {
    expect(parseQuery('source:linkedin_ad source:upload source:pool').operators.map((o) => o.value)).toEqual(['LINKEDIN_AD', 'CV_UPLOAD', 'EXISTING_POOL']);
    expect(parseQuery('source:fax').operators).toEqual([]);
    expect(parseQuery('source:fax').text).toBe('source:fax');
  });
  it('keeps empty, too short or comma-holding values as free text', () => {
    expect(parseQuery('tag: cv:a tag:a,b').operators).toEqual([]);
  });
  it('routes to /network with the /people parameter names', () => {
    const r = routeFor(parseQuery('tag:java tag:sap source:referral source:email cv:"project manager" ali'))!;
    expect(r.path).toBe('/network');
    expect(r.params).toEqual([['tag', 'java,sap'], ['sourceChannel', 'REFERRAL,EMAIL'], ['cv', '"project manager"'], ['q', 'ali']]);
    expect(r.pending).toEqual([]);
  });
  it('routes to /network on their own and drops them from accounts lists', () => {
    expect(routeFor(parseQuery('tag:java'))!.to).toBe('/network?tag=java');
    const r = routeFor(parseQuery('status:active tag:java source:email'))!;
    expect(r.path).toBe('/network');
    expect(r.params).toEqual([['tag', 'java'], ['sourceChannel', 'EMAIL']]);
    expect(r.dropped.map((o) => o.key)).toEqual(['status']);
  });
  it('joins repeated cv with a space and quotes only what the user quoted', () => {
    expect(routeFor(parseQuery('cv:java cv:sap'))!.to).toBe('/network?cv=java+sap');
    expect(routeFor(parseQuery('cv:-intern cv:"project manager"'))!.params).toEqual([['cv', '-intern "project manager"']]);
    expect(parseQuery('cv:sap').operators[0]!.value).toBe('sap');
  });
});
