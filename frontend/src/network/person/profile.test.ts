import { describe, expect, it } from 'vitest';
import { canResendConsent, compensationVisible, safeLinkedIn } from './profile';
import { daysLeft, splitFiles } from '../files/fileRules';

const comp = { currency: 'TRY', period: 'ANNUAL' as const, basis: 'GROSS' as const, expected: 100 };

describe('compensation visibility', () => {
  it('shows only when present and not restricted', () => {
    expect(compensationVisible({ compensation: comp, compensationRestricted: false })).toBe(true);
    expect(compensationVisible({ compensation: comp })).toBe(true);
    expect(compensationVisible({ compensation: comp, compensationRestricted: true })).toBe(false);
    expect(compensationVisible({ compensation: null, compensationRestricted: false })).toBe(false);
    expect(compensationVisible({ compensationRestricted: true })).toBe(false);
  });
});

describe('profile helpers', () => {
  it('links only http(s) LinkedIn URLs', () => {
    expect(safeLinkedIn('https://www.linkedin.com/in/x')).toContain('linkedin.com/in/x');
    expect(safeLinkedIn('javascript:alert(1)')).toBeUndefined();
    expect(safeLinkedIn('nonsense')).toBeUndefined();
  });
  it('offers one-click resend unless consent is given, no email or anonymised', () => {
    expect(canResendConsent('SENT', true, false)).toBe(true);
    expect(canResendConsent('NOT_SENT', true, false)).toBe(true);
    expect(canResendConsent('GIVEN', true, false)).toBe(false);
    expect(canResendConsent('SENT', false, false)).toBe(false);
    expect(canResendConsent('SENT', true, true)).toBe(false);
  });
});

describe('file pre-checks', () => {
  it('rejects empty, wrong type, oversize and extra files', () => {
    const mb = 1024 * 1024;
    const r = splitFiles([{ name: 'a.pdf', size: 10 }, { name: 'b.exe', size: 10 }, { name: 'c.docx', size: 11 * mb }, { name: 'd.txt', size: 0 }, { name: 'e.PDF', size: 5 }], 1);
    expect(r.ok).toEqual([0]);
    expect(r.rejected.map((x) => x.code)).toEqual(['UNSUPPORTED_MEDIA_TYPE', 'PAYLOAD_TOO_LARGE', 'EMPTY', 'TOO_MANY']);
  });
  it('counts days to expiry', () => {
    const now = Date.parse('2026-10-01T00:00:00Z');
    expect(daysLeft('2026-10-15T00:00:00Z', now)).toBe(14);
    expect(daysLeft('2026-09-01T00:00:00Z', now)).toBe(0);
  });
});

describe('do-not-contact people', () => {
  it('get no one-click consent send', () => {
    expect(canResendConsent('REFUSED', true, false)).toBe(false);
    expect(canResendConsent('WITHDRAWN', true, false)).toBe(false);
  });
});
