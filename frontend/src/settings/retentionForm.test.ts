import { describe, expect, it } from 'vitest';
import { emptyForm, offLimitsErrorKey, toCreateBody, validateForm } from './offLimits';
import { parseReminderDays } from './retentionForm';

describe('parseReminderDays', () => {
  it('accepts 1..365, dedupes and sorts largest first', () => {
    expect(parseReminderDays(['14', '60', '14'])).toEqual([60, 14]);
    expect(parseReminderDays([])).toEqual([]);
  });
  it('rejects bad values and more than five', () => {
    expect(parseReminderDays(['0'])).toBeNull();
    expect(parseReminderDays(['366'])).toBeNull();
    expect(parseReminderDays(['x'])).toBeNull();
    expect(parseReminderDays(['1', '2', '3', '4', '5', '6'])).toBeNull();
  });
});

describe('off-limits form', () => {
  it('needs exactly one target and a sane date range', () => {
    const f = emptyForm();
    expect(validateForm(f, '2026-10-26')).toBe('target');
    expect(validateForm({ ...f, accountId: 'a1' }, '2026-10-26')).toBeNull();
    expect(validateForm({ ...f, target: 'company', companyName: '  ' }, '2026-10-26')).toBe('target');
    expect(validateForm({ ...f, accountId: 'a1', endsOn: '2026-10-01' }, '2026-10-26')).toBe('dates');
    expect(validateForm({ ...f, accountId: 'a1', startsOn: '2026-11-01', endsOn: '2026-10-30' }, '2026-10-26')).toBe('dates');
  });
  it('builds the body with one target and no blank optionals', () => {
    expect(toCreateBody({ ...emptyForm(), accountId: 'a1' })).toEqual({ accountId: 'a1', reason: 'CLIENT_AGREEMENT' });
    expect(toCreateBody({ ...emptyForm(), target: 'company', accountId: 'stale', companyName: ' Acme ', note: ' n ', endsOn: '2027-01-01' })).toEqual({ companyName: 'Acme', reason: 'CLIENT_AGREEMENT', note: 'n', endsOn: '2027-01-01' });
  });
  it('maps errors', () => {
    expect(offLimitsErrorKey(409, { code: 'DUPLICATE' })).toBe('duplicate');
    expect(offLimitsErrorKey(403, {})).toBe('forbidden');
    expect(offLimitsErrorKey(400, {})).toBe('validation');
    expect(offLimitsErrorKey(500, undefined)).toBe('generic');
  });
});
