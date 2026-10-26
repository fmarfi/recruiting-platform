import { describe, expect, it } from 'vitest';
import { emptyForm, endDateFor, endsToday, offLimitsErrorKey, toCreateBody, validateForm } from './offLimits';
import { parseReminderDays, reminderState } from './retentionForm';

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

describe('reminderState', () => {
  const saved = [60, 14];
  it('shows saved values when untouched', () => {
    expect(reminderState(null, saved)).toEqual({ tags: ['60', '14'], error: false, days: saved });
  });
  it('keeps an invalid tag visible with an error and the saved days', () => {
    expect(reminderState(['60', 'abc'], saved)).toEqual({ tags: ['60', 'abc'], error: true, days: saved });
  });
  it('clears the error once the bad tag is removed', () => {
    expect(reminderState(['60'], saved)).toEqual({ tags: ['60'], error: false, days: [60] });
    expect(reminderState([], saved).error).toBe(false);
  });
});

describe('ending a rule', () => {
  it('ends yesterday when the rule started earlier, so it is inactive at once', () => {
    expect(endDateFor({ startsOn: '2026-10-01' }, '2026-10-26')).toBe('2026-10-25');
    expect(endDateFor({ startsOn: '2026-10-25' }, '2026-11-01')).toBe('2026-10-31');
    expect(endDateFor({ startsOn: '2026-10-01' }, '2026-03-01')).toBe('2026-10-01');
  });
  it('cannot end before its start: ends on the start day', () => {
    expect(endDateFor({ startsOn: '2026-10-26' }, '2026-10-26')).toBe('2026-10-26');
    expect(endDateFor({ startsOn: '2026-11-05' }, '2026-10-26')).toBe('2026-11-05');
  });
  it('flags active rules ending today', () => {
    expect(endsToday({ active: true, endsOn: '2026-10-26' }, '2026-10-26')).toBe(true);
    expect(endsToday({ active: false, endsOn: '2026-10-26' }, '2026-10-26')).toBe(false);
  });
});
